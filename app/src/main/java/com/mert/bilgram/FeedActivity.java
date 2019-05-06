package com.mert.bilgram;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class FeedActivity extends AppCompatActivity {

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    DatabaseReference myRef = firebaseDatabase.getReference();
    ArrayList<String> userEmailFromFB = new ArrayList<>(), userImageFromFB = new ArrayList<>(), userPostDescFromFB = new ArrayList<>(), postIDsFromFB = new ArrayList<>();
    RecyclerViewAdapter adapter = new RecyclerViewAdapter(userEmailFromFB, userPostDescFromFB, userImageFromFB, this, this);
    SharedPreferences mPrefs;

    static SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mPrefs.getBoolean("NIGHT_MODE", false))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
            setTheme(R.style.AppThemeDark);

        getDataFromFirebase();

        setContentView(R.layout.activity_feed);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Intent intent = new Intent(getApplicationContext(), ViewImageActivity.class);
                intent.putExtra("downloadURL", userImageFromFB.get(position));
                startActivity(intent);
            }

            @Override
            public void onItemDoubleClicked(RecyclerView recyclerView, int position, View v) {
                Toast.makeText(FeedActivity.this, "Liked", Toast.LENGTH_SHORT).show();

                String id = postIDsFromFB.get(position);
                String url = userImageFromFB.get(position);
                String postdesc = userPostDescFromFB.get(position);
                String useremail = userEmailFromFB.get(position);
                database = openOrCreateDatabase("SavedPosts", MODE_PRIVATE, null);
                database.execSQL("CREATE TABLE IF NOT EXISTS posts (id VARCHAR, url VARCHAR, postDesc VARCHAR, useremail VARCHAR, time VARCHAR)");

                String sqlString = "INSERT INTO posts (id, url, postDesc, useremail, time) VALUES (?, ?, ?, ?, ?)";
                SQLiteStatement statement = database.compileStatement(sqlString);
                statement.bindString(1, id);
                statement.bindString(2, url);
                statement.bindString(3, postdesc);
                statement.bindString(4, useremail);
                statement.bindString(5, new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));
                statement.execute();
            }
        });

    }

    public boolean itemLongClicked(final int i) {

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser().getEmail().equals(userEmailFromFB.get(i))) {

            final EditText edittext = new EditText(this);
            edittext.setText(userPostDescFromFB.get(i));

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Edit or Delete")
                    .setView(edittext)
                    .setMessage("Do you want to edit or delete this post?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i2) {

                            AlertDialog.Builder builder1 = new AlertDialog.Builder(FeedActivity.this);
                            builder1.setTitle("Are you sure?")
                                    .setMessage("You are about to delete this post permanently.")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Query query = myRef.child("Posts").orderByChild("downloadURL").equalTo(userImageFromFB.get(i));
                                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                        snapshot.getRef().removeValue();
                                                        StorageReference photoRef = firebaseStorage.getReferenceFromUrl(userImageFromFB.get(i));
                                                        photoRef.delete();
                                                    }

                                                    userImageFromFB.remove(i);
                                                    userPostDescFromFB.remove(i);
                                                    userEmailFromFB.remove(i);
                                                    adapter.notifyDataSetChanged();
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                }
                                            });

                                        }


                                    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();

                        }

                    }).setNeutralButton("EDIT", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {

                    final String newDesc = edittext.getText().toString();

                    Query query = myRef.child("Posts").orderByChild("downloadURL").equalTo(userImageFromFB.get(i));

                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                DatabaseReference numMesasReference = snapshot.getRef().child("postDesc");
                                numMesasReference.setValue(newDesc);
                            }
                            userPostDescFromFB.set(i, newDesc);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });

                }
            }).show();

        }

        return true;

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void getDataFromFirebase() {

        DatabaseReference newReference = firebaseDatabase.getReference("Posts");
        newReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    HashMap<String, String> hashMap = (HashMap<String, String>) ds.getValue();

                    if (userImageFromFB.contains(hashMap.get("downloadURL")) || hashMap.get("downloadURL") == null)
                        continue;

                    userEmailFromFB.add(0, hashMap.get("useremail"));
                    userPostDescFromFB.add(0, hashMap.get("postDesc"));
                    userImageFromFB.add(0, hashMap.get("downloadURL"));
                    postIDsFromFB.add(0, hashMap.get("ID"));

                    adapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.feed_menu, menu);

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
            menu.getItem(3).setChecked(true);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.sign_out:
                FirebaseAuth user = FirebaseAuth.getInstance();
                user.signOut();
                startActivity(new Intent(getApplicationContext(), SignInWithTabs.class));
                break;

            case R.id.darkLight:

                item.setChecked(!item.isChecked());

                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean("NIGHT_MODE", item.isChecked());
                editor.apply();

                startActivity(new Intent(this, FeedActivity.class));

                break;

            case R.id.myProfile:

                startActivity(new Intent(this, FavoritesActivity.class));
                break;

            case R.id.privacy_policy:

                startActivity(new Intent(this, PrivacyPolicyActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void goToUploadActivity(View view) {

        Intent intent = new Intent(getApplicationContext(), UploadActivity.class);
        startActivity(intent);

    }
}
