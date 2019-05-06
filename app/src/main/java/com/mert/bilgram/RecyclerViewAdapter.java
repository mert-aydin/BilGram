package com.mert.bilgram;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private final ArrayList<String> userEmail;
    private final ArrayList<String> postDesc;
    private final ArrayList<String> userImage;
    private Activity context;
    private FeedActivity feedActivity;

    RecyclerViewAdapter(ArrayList<String> userEmail, ArrayList<String> postDesc, ArrayList<String> userImage, final Activity context, final FeedActivity feedActivity) {
        this.userEmail = userEmail;
        this.postDesc = postDesc;
        this.userImage = userImage;
        this.context = context;
        this.feedActivity = feedActivity;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_view, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {

        Glide.with(context).load(userImage.get(i)).into(viewHolder.imageView);
        viewHolder.userEmail.setText(userEmail.get(i));
        viewHolder.postDesc.setText(postDesc.get(i));

        viewHolder.parentLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return feedActivity.itemLongClicked(viewHolder.getAdapterPosition());
            }
        });

    }

    @Override
    public int getItemCount() {
        Log.e("RecyclerViewAdapter", "getItemCount: " + userEmail.size());
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
