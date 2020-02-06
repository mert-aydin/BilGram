package com.mert.bilgram

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class UploadActivity : AppCompatActivity() {
    private lateinit var postImage: ImageView
    private lateinit var postDesc: EditText
    private lateinit var mShaker: ShakeListener
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStorageRef: StorageReference
    private lateinit var selectedImage: Uri
    private lateinit var mCurrentPhotoPath: String
    private lateinit var postBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
            setTheme(R.style.AppThemeDark)

        setContentView(R.layout.activity_upload)

        setTitle(R.string.share_a_post)

        firebaseDatabase = FirebaseDatabase.getInstance()
        myRef = firebaseDatabase.reference
        mAuth = FirebaseAuth.getInstance()
        mStorageRef = FirebaseStorage.getInstance().reference
        postImage = findViewById(R.id.postImageView)
        postDesc = findViewById(R.id.postDescET)
        postBtn = findViewById(R.id.postBtn)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 11)

        mShaker = ShakeListener(this)
        mShaker.setOnShakeListener(object : ShakeListener.OnShakeListener {
            override fun onShake() {
                mShaker.pause()
                dispatchTakePictureIntent()
            }
        })
    }

    private fun galleryAddPic() {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(File(mCurrentPhotoPath))
        mediaScanIntent.data = contentUri
        this.sendBroadcast(mediaScanIntent)
        selectedImage = contentUri
        Glide.with(this).load(selectedImage).into(postImage)
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) { // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, "com.mert.bilgram", photoFile))
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File { // Create an image file name
        val image = File.createTempFile("JPEG_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + "_", ".jpeg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    fun post(view: View?) {
        postBtn.text = getString(R.string.posting)
        postBtn.isEnabled = false
        postDesc.isEnabled = false
        mShaker.pause()
        postImage.isEnabled = false
        val imageName = "images/${UUID.randomUUID()}.jpeg"
        mStorageRef.child(imageName).putFile(selectedImage).addOnSuccessListener(this) {
            FirebaseStorage.getInstance().getReference(imageName).downloadUrl.addOnSuccessListener { uri ->
                val postId = UUID.randomUUID().toString()
                val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                myRef.child("Posts").child(time + postId).child("useremail").setValue(mAuth.currentUser!!.email)
                myRef.child("Posts").child(time + postId).child("postDesc").setValue(postDesc.text.toString())
                myRef.child("Posts").child(time + postId).child("downloadURL").setValue(uri.toString())
                myRef.child("Posts").child(time + postId).child("ID").setValue(time + postId)
                Snackbar.make(postBtn, R.string.post_shared, Snackbar.LENGTH_SHORT).show()
                startActivity(Intent(applicationContext, FeedActivity::class.java))
            }
        }.addOnFailureListener(this) { e -> Toast.makeText(this@UploadActivity, e.localizedMessage, Toast.LENGTH_LONG).show() }
    }

    fun selectImage(view: View?) {
        startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQUEST_PICK_IMAGE)
        mShaker.pause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            mShaker.pause()
            selectedImage = data.data!!
            postBtn.isEnabled = true
            Glide.with(this).load(selectedImage).into(postImage)
        }

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_CANCELED)
            mShaker.resume()

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            postBtn.isEnabled = true
            postImage.isEnabled = false
            mShaker.pause()
            galleryAddPic()
        }

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_CANCELED)
            mShaker.resume()
    }

    override fun onBackPressed() {
        mShaker.pause()
        super.onBackPressed()
    }

    companion object {
        private const val REQUEST_PICK_IMAGE = 111
        const val REQUEST_TAKE_PHOTO = 222
    }
}