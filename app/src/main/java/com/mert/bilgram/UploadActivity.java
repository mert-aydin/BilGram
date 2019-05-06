package com.mert.bilgram;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 111;
    ImageView postImage;
    EditText postDesc;
    ShakeListener mShaker;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    Uri selectedImage = null;

    static final int REQUEST_TAKE_PHOTO = 222;
    String mCurrentPhotoPath;
    Button postBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
            setTheme(R.style.AppThemeDark);

        setContentView(R.layout.activity_upload);

        setTitle("Share a Post");

        firebaseDatabase = FirebaseDatabase.getInstance();
        myRef = firebaseDatabase.getReference();
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        postImage = findViewById(R.id.postImageView);
        postDesc = findViewById(R.id.postDescET);
        postBtn = findViewById(R.id.postBtn);

        if (ContextCompat.checkSelfPermission(UploadActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(UploadActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(UploadActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(UploadActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 11);

        }

        mShaker = new ShakeListener(this);
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShake() {

                mShaker.pause();
                dispatchTakePictureIntent();

            }
        });

    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

        selectedImage = contentUri;

        Glide.with(this).load(selectedImage).into(postImage);

    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.mert.bilgram",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpeg", storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void post(View view) {

        postBtn.setText(getString(R.string.posting));
        postBtn.setEnabled(false);
        postDesc.setEnabled(false);
        mShaker.pause();
        postImage.setEnabled(false);

        UUID uuid = UUID.randomUUID();
        final String imageName = "images/" + uuid + ".jgp";

        StorageReference storageReference = mStorageRef.child(imageName);
        storageReference.putFile(selectedImage).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                StorageReference newReference = FirebaseStorage.getInstance().getReference(imageName);
                newReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String downloadURL = uri.toString();

                        FirebaseUser user = mAuth.getCurrentUser();

                        String userEmail = user.getEmail();

                        String desc = postDesc.getText().toString();

                        UUID uuid = UUID.randomUUID();
                        final String postId = uuid.toString();
                        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

                        myRef.child("Posts").child(time + postId).child("useremail").setValue(userEmail);
                        myRef.child("Posts").child(time + postId).child("postDesc").setValue(desc);
                        myRef.child("Posts").child(time + postId).child("downloadURL").setValue(downloadURL);
                        myRef.child("Posts").child(time + postId).child("ID").setValue(time + postId);


                        Toast.makeText(UploadActivity.this, "Post Shared!", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(getApplicationContext(), FeedActivity.class));
                    }
                });

            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }

    public void selectImage(View view) {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
        mShaker.pause();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {

            mShaker.pause();
            selectedImage = data.getData();
            postBtn.setEnabled(true);

            Glide.with(this).load(selectedImage).into(postImage);

        }

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_CANCELED) {

            mShaker.resume();

        }

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            postBtn.setEnabled(true);
            postImage.setEnabled(false);
            mShaker.pause();
            galleryAddPic();

        }

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_CANCELED) {

            mShaker.resume();

        }

    }

    @Override
    public void onBackPressed() {
        mShaker.pause();
        super.onBackPressed();
    }
}