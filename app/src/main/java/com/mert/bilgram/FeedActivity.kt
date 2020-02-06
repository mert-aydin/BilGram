package com.mert.bilgram

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.novoda.merlin.Merlin
import java.text.SimpleDateFormat
import java.util.*

class FeedActivity : AppCompatActivity() {
    private var firebaseDatabase = FirebaseDatabase.getInstance()
    var firebaseStorage = FirebaseStorage.getInstance()
    private var myRef = firebaseDatabase.reference
    var userEmailFromFB = ArrayList<String?>()
    var userImageFromFB = ArrayList<String?>()
    var userPostDescFromFB = ArrayList<String?>()
    var postIDsFromFB = ArrayList<String?>()
    var adapter = RecyclerViewAdapter(userEmailFromFB, userPostDescFromFB, userImageFromFB, this, this)
    private lateinit var fab: FloatingActionButton
    private lateinit var merlin: Merlin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> setTheme(R.style.AppThemeDark)
            Configuration.UI_MODE_NIGHT_NO -> setTheme(R.style.AppThemeLight)
        }

        merlin = Merlin.Builder().withAllCallbacks().build(this)

        merlin.registerConnectable {
            fab.backgroundTintList = ColorStateList.valueOf(getColor(android.R.color.holo_red_dark))
            fab.setImageDrawable(getDrawable(R.drawable.ic_add_white_24dp))
            fab.setOnClickListener { v -> goToUploadActivity(v) }
        }

        merlin.registerDisconnectable {
            fab.backgroundTintList = ColorStateList.valueOf(Color.DKGRAY)
            fab.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.stat_sys_warning))
            fab.setOnClickListener { Snackbar.make(fab, R.string.connection_error, Snackbar.LENGTH_SHORT).show() }
        }

        setContentView(R.layout.activity_feed)

        fab = findViewById(R.id.fab)

        if (isConnectedToInternet())
            dataFromFirebase
        else {
            fab.backgroundTintList = ColorStateList.valueOf(Color.DKGRAY)

            fab.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.stat_sys_warning))

            fab.setOnClickListener { Snackbar.make(fab, R.string.connection_error, Snackbar.LENGTH_SHORT).show() }

            database = openOrCreateDatabase("Posts", Context.MODE_PRIVATE, null)

            database.execSQL("CREATE TABLE IF NOT EXISTS posts (id VARCHAR, url VARCHAR, postDesc VARCHAR, useremail VARCHAR)")

            val cursor = database.rawQuery("SELECT * FROM posts ORDER BY id DESC", null)

            val idIndex = cursor.getColumnIndex("id")

            val urlIndex = cursor.getColumnIndex("url")

            val postDescIndex = cursor.getColumnIndex("postDesc")

            val userEmailIndex = cursor.getColumnIndex("useremail")

            cursor.moveToFirst()

            do {
                postIDsFromFB.add(cursor.getString(idIndex))
                userImageFromFB.add(cursor.getString(urlIndex))
                userEmailFromFB.add(cursor.getString(userEmailIndex))
                userPostDescFromFB.add(cursor.getString(postDescIndex))
                adapter.notifyDataSetChanged()

            } while (cursor.moveToNext())

            cursor.close()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(this)

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(object : ItemClickSupport.OnItemClickListener {
            override fun onItemClicked(recyclerView: RecyclerView?, position: Int, v: View?) {
                startActivity(Intent(applicationContext, ViewImageActivity::class.java).putExtra("downloadURL", userImageFromFB[position]))
            }

            override fun onItemDoubleClicked(recyclerView: RecyclerView?, position: Int, v: View?) {
                Snackbar.make(v!!, R.string.liked, Snackbar.LENGTH_SHORT).show()

                database = openOrCreateDatabase("SavedPosts", Context.MODE_PRIVATE, null)
                database.execSQL("CREATE TABLE IF NOT EXISTS posts (id VARCHAR, url VARCHAR, postDesc VARCHAR, useremail VARCHAR, time VARCHAR)")

                val statement = database.compileStatement("INSERT INTO posts (id, url, postDesc, useremail, time) VALUES (?, ?, ?, ?, ?)")

                statement.bindString(1, postIDsFromFB[position])
                statement.bindString(2, userImageFromFB[position])
                statement.bindString(3, userPostDescFromFB[position])
                statement.bindString(4, userEmailFromFB[position])
                statement.bindString(5, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()))
                statement.execute()
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun isConnectedToInternet(): Boolean {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
            this.getNetworkCapabilities(this.activeNetwork)?.run {
                return true
            }
        }

        return false
    }

    fun itemLongClicked(i: Int): Boolean {
        val mAuth = FirebaseAuth.getInstance()
        if (mAuth.currentUser!!.email == userEmailFromFB[i]) {

            val editText = EditText(this)
            editText.setText(userPostDescFromFB[i])

            AlertDialog.Builder(this).setTitle(R.string.edit_or_delete)
                    .setView(editText)
                    .setMessage(R.string.edit_or_delete_desc)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        AlertDialog.Builder(this)
                                .setTitle(R.string.are_you_sure)
                                .setMessage(R.string.you_are_deleting_post_permanently)
                                .setPositiveButton(R.string.yes) { _, _ ->
                                    myRef.child("Posts").orderByChild("downloadURL").equalTo(userImageFromFB[i])
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                    for (snapshot in dataSnapshot.children) {
                                                        snapshot.ref.removeValue()
                                                        firebaseStorage.getReferenceFromUrl(userImageFromFB[i]!!).delete()
                                                    }
                                                    userImageFromFB.removeAt(i)
                                                    userPostDescFromFB.removeAt(i)
                                                    userEmailFromFB.removeAt(i)
                                                    adapter.notifyItemRemoved(i)
                                                }

                                                override fun onCancelled(databaseError: DatabaseError) {}
                                            })
                                }.setNegativeButton(R.string.no) { _, _ -> }.show()

                    }.setNeutralButton(R.string.edit) { _, _ ->
                        val newDesc = editText.text.toString()
                        myRef.child("Posts").orderByChild("downloadURL").equalTo(userImageFromFB[i])
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        for (snapshot in dataSnapshot.children)
                                            snapshot.ref.child("postDesc").setValue(newDesc)

                                        userPostDescFromFB[i] = newDesc
                                        adapter.notifyItemChanged(i)
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {}
                                })
                    }.show()
        }
        return true
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private val dataFromFirebase: Unit
        get() {
            database = openOrCreateDatabase("Posts", Context.MODE_PRIVATE, null)
            database.execSQL("CREATE TABLE IF NOT EXISTS posts (id VARCHAR, url VARCHAR, postDesc VARCHAR, useremail VARCHAR)")
            database.execSQL("DELETE FROM Posts")

            firebaseDatabase.getReference("Posts").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (ds in dataSnapshot.children) {
                        val hashMap = ds.value as HashMap<*, *>
                        if (userImageFromFB.contains(hashMap["downloadURL"]) || hashMap["downloadURL"] == null)
                            continue

                        userEmailFromFB.add(0, hashMap["useremail"] as String?)
                        userPostDescFromFB.add(0, hashMap["postDesc"] as String?)
                        userImageFromFB.add(0, hashMap["downloadURL"] as String?)
                        postIDsFromFB.add(0, hashMap["ID"] as String?)

                        val statement = database.compileStatement("INSERT INTO posts (id, url, postDesc, useremail) VALUES (?, ?, ?, ?)")

                        statement.bindString(1, postIDsFromFB[0])
                        statement.bindString(2, userImageFromFB[0])
                        statement.bindString(3, userPostDescFromFB[0])
                        statement.bindString(4, userEmailFromFB[0])
                        statement.execute()
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.feed_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, SignInWithTabs::class.java))
            }
            R.id.myProfile -> startActivity(Intent(this, FavoritesActivity::class.java))
            R.id.privacy_policy -> startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    fun goToUploadActivity(view: View?) {
        startActivity(Intent(applicationContext, UploadActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        merlin.bind()
    }

    companion object {
        lateinit var database: SQLiteDatabase
    }
}