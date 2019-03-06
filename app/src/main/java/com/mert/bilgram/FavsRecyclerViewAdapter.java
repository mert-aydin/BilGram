package com.mert.bilgram;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class FavsRecyclerViewAdapter extends RecyclerView.Adapter<FavsRecyclerViewAdapter.ViewHolder> {

    private final ArrayList<String> userEmail;
    private final ArrayList<String> postDesc;
    private final ArrayList<String> userImage;
    private final Activity context;
    private final FavoritesActivity favoritesActivity;

    FavsRecyclerViewAdapter(ArrayList<String> userEmail, ArrayList<String> postDesc, ArrayList<String> userImage, Activity context, FavoritesActivity favoritesActivity) {
        this.userEmail = userEmail;
        this.postDesc = postDesc;
        this.userImage = userImage;
        this.context = context;
        this.favoritesActivity = favoritesActivity;
    }

    @NonNull
    @Override
    public FavsRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_view, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final FavsRecyclerViewAdapter.ViewHolder viewHolder, final int i) {

        Glide.with(context).load(userImage.get(i)).into(viewHolder.imageView);
        viewHolder.userEmail.setText(userEmail.get(i));
        viewHolder.postDesc.setText(postDesc.get(i));


        viewHolder.parentLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //remove post from favs

                favoritesActivity.unSave(i);

                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return userEmail.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView userEmail;
        ImageView imageView;
        TextView postDesc;
        ConstraintLayout parentLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            userEmail = itemView.findViewById(R.id.userEmailTVCV);
            imageView = itemView.findViewById(R.id.imageViewCV);
            postDesc = itemView.findViewById(R.id.descTVCV);
            parentLayout = itemView.findViewById(R.id.parent_layout);

        }
    }

}
