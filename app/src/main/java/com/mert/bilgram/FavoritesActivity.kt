package com.mert.bilgram

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.util.*

class FavoritesActivity : AppCompatActivity() {

    private var userEmailFromFB = ArrayList<String>()
    private var userImageFromFB = ArrayList<String>()
    private var userPostDescFromFB = ArrayList<String>()
    private var postIDsFromFB = ArrayList<String>()
    private var adapter = FavoritesRecyclerViewAdapter(userEmailFromFB, userPostDescFromFB, userImageFromFB, this, this)
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
            setTheme(R.style.AppThemeDark)

        setContentView(R.layout.activity_favorites)

        title = getString(R.string.liked_posts)

        recyclerView = findViewById(R.id.favRecyclerView)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(object : ItemClickSupport.OnItemClickListener {
            override fun onItemClicked(recyclerView: RecyclerView?, position: Int, v: View?) {
                startActivity(Intent(applicationContext, ViewImageActivity::class.java).putExtra("downloadURL", userImageFromFB[position]))
            }

            override fun onItemDoubleClicked(recyclerView: RecyclerView?, position: Int, v: View?) {}
        })

        try {
            FeedActivity.database = this.openOrCreateDatabase("SavedPosts", Context.MODE_PRIVATE, null)

            FeedActivity.database.execSQL("CREATE TABLE IF NOT EXISTS posts (id VARCHAR, url VARCHAR, postDesc VARCHAR, useremail VARCHAR, time VARCHAR)")

            val cursor = FeedActivity.database.rawQuery("SELECT * FROM posts ORDER BY time DESC", null)

            val urlIndex = cursor!!.getColumnIndex("url")

            val postDescIndex = cursor.getColumnIndex("postDesc")

            val userEmailIndex = cursor.getColumnIndex("useremail")

            val idIndex = cursor.getColumnIndex("id")

            cursor.moveToFirst()

            do {
                if (!postIDsFromFB.contains(cursor.getString(idIndex)))
                    postIDsFromFB.add(cursor.getString(idIndex))
                else
                    continue

                userImageFromFB.add(cursor.getString(urlIndex))
                userEmailFromFB.add(cursor.getString(userEmailIndex))
                userPostDescFromFB.add(cursor.getString(postDescIndex))
                adapter.notifyDataSetChanged()
            } while (cursor.moveToNext())

            cursor.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unSave(i: Int, v: View) {
        AlertDialog.Builder(this).setTitle(getString(R.string.remove))
                .setMessage(getString(R.string.remove_from_favourites))
                .setPositiveButton(R.string.yes) { _, _ ->

                    FeedActivity.database.execSQL("Delete from posts where id='${postIDsFromFB[i]}'")
                    Snackbar.make(v, R.string.removed_from_favourites, Snackbar.LENGTH_SHORT).show()
                    userEmailFromFB.removeAt(i)
                    userImageFromFB.removeAt(i)
                    postIDsFromFB.removeAt(i)
                    userPostDescFromFB.removeAt(i)
                    adapter.notifyItemRemoved(i)

                }.setNegativeButton(R.string.no) { _, _ -> }.show()
    }
}