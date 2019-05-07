package com.mert.bilgram;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FavoritesActivity extends AppCompatActivity {

    ArrayList<String> userEmailFromFB = new ArrayList<>(), userImageFromFB = new ArrayList<>(), userPostDescFromFB = new ArrayList<>(), postIDsFromFB = new ArrayList<>();
    FavoritesRecyclerViewAdapter adapter = new FavoritesRecyclerViewAdapter(userEmailFromFB, userPostDescFromFB, userImageFromFB, this, this);
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
            setTheme(R.style.AppThemeDark);

        setContentView(R.layout.activity_favorites);
        setTitle("Liked Posts");

        recyclerView = findViewById(R.id.favRecyclerView);
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

            }
        });

        try {

            FeedActivity.database = this.openOrCreateDatabase("SavedPosts", MODE_PRIVATE, null);
            FeedActivity.database.execSQL("CREATE TABLE IF NOT EXISTS posts (id VARCHAR, url VARCHAR, postDesc VARCHAR, useremail VARCHAR, time VARCHAR)");

            Cursor cursor = FeedActivity.database.rawQuery("SELECT * FROM posts ORDER BY time DESC", null);

            int urlIndex = cursor.getColumnIndex("url");
            int postDescIndex = cursor.getColumnIndex("postDesc");
            int userEmailIndex = cursor.getColumnIndex("useremail");
            int IdIndex = cursor.getColumnIndex("id");

            cursor.moveToFirst();

            while (cursor != null) {

                if (!postIDsFromFB.contains(cursor.getString(IdIndex)))
                    postIDsFromFB.add(cursor.getString(IdIndex));
                else {
                    cursor.moveToNext();
                    continue;
                }
                userImageFromFB.add(cursor.getString(urlIndex));
                userEmailFromFB.add(cursor.getString(userEmailIndex));
                userPostDescFromFB.add(cursor.getString(postDescIndex));
                adapter.notifyDataSetChanged();

                cursor.moveToNext();

            }

            cursor.close();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void unSave(final int i) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove")
                .setMessage("Do you want to remove this post from your Favourites")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        String id = postIDsFromFB.get(i);

                        FeedActivity.database.execSQL("Delete from posts where id='" + id + "'");

                        Toast.makeText(FavoritesActivity.this, "Post removed from Favourites", Toast.LENGTH_SHORT).show();

                        userEmailFromFB.remove(i);
                        userImageFromFB.remove(i);
                        postIDsFromFB.remove(i);
                        userPostDescFromFB.remove(i);

                        adapter.notifyDataSetChanged();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).show();

    }

}
