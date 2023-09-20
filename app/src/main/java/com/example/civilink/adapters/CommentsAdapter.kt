package com.example.civilink.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.civilink.R
import com.example.civilink.data.Comment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Define views here
        val commentText: TextView = itemView.findViewById(R.id.commentTextView)
        val userEmail: TextView = itemView.findViewById(R.id.userIdTextView)
        val timestamp: TextView = itemView.findViewById(R.id.timestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate your item layout and return a ViewHolder
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Bind data to views here
        val comment = comments[position]
        holder.commentText.text = comment.commentText
        holder.userEmail.text = comment.userEmail
        // Format timestamp as needed
        val formattedTimestamp = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            .format(Date(comment.timestamp))
        holder.timestamp.text = formattedTimestamp
    }

    override fun getItemCount(): Int {
        return comments.size
    }
}
