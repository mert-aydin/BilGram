package com.mert.bilgram

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.*

class FavoritesRecyclerViewAdapter internal constructor(private val userEmail: ArrayList<String>, private val postDesc: ArrayList<String>, private val userImage: ArrayList<String>, private val context: Activity, private val favoritesActivity: FavoritesActivity) : RecyclerView.Adapter<FavoritesRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.recycler_view, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        Glide.with(context).load(userImage[i]).into(viewHolder.imageView)
        viewHolder.userEmail.text = userEmail[i]
        viewHolder.postDesc.text = postDesc[i]
        viewHolder.parentLayout.setOnLongClickListener {
            favoritesActivity.unSave(viewHolder.adapterPosition, viewHolder.imageView)
            true
        }
    }

    override fun getItemCount(): Int {
        return userEmail.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var userEmail: TextView = itemView.findViewById(R.id.userEmailTVCV)
        var imageView: ImageView = itemView.findViewById(R.id.imageViewCV)
        var postDesc: TextView = itemView.findViewById(R.id.descTVCV)
        var parentLayout: ConstraintLayout = itemView.findViewById(R.id.parent_layout)
    }

}