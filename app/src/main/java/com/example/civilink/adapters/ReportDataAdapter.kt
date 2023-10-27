package com.example.civilink.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.civilink.R
import com.example.civilink.data.ReportData
import com.example.civilink.databinding.FeedItemBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportDataAdapter(private val reportDataList: List<ReportData>) :
    RecyclerView.Adapter<ReportDataAdapter.ViewHolder>() {

    class ViewHolder(val binding: FeedItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FeedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reportData = reportDataList[position]
        holder.binding.feedItemName.text = "User: ${reportData.spinnerSelectedItem}"
        holder.binding.feedProblemDescription.text = reportData.problemStatement
        val formattedTimestamp = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            .format(Date(reportData.timestamp!!))
        val databaseReference =
            reportData.userId?.let { FirebaseDatabase.getInstance().getReference("users").child(it) }

        databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userEmail = snapshot.child("emailId").getValue(String::class.java)
                    if (!userEmail.isNullOrEmpty()) {
                        holder.binding.feedItemName.text = userEmail
                    }
                    val userPhoto = snapshot.child("profileImage").toString()
                    if (!userPhoto.isNullOrEmpty()) {
                        Glide.with(holder.itemView.context)
                            .load(userPhoto)
                            .preload()
                        Glide.with(holder.itemView.context)
                            .load(userPhoto)
                            .into(holder.binding.userPhoto)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle any errors here
            }
        })
        holder.binding.timeStamp.text = formattedTimestamp.toString()
        Glide.with(holder.itemView.context)
            .load(reportData.photoUrl)
            .preload()
        Glide.with(holder.itemView.context)
            .load(reportData.photoUrl)
            .into(holder.binding.feedItemImage)
    }

    override fun getItemCount(): Int {
        return reportDataList.size
    }
}
