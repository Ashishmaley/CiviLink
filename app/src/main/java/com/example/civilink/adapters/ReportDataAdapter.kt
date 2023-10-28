package com.example.civilink.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.example.civilink.R
import com.example.civilink.data.ReportData
import com.example.civilink.databinding.FeedItemBinding
import com.facebook.shimmer.Shimmer
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportDataAdapter(private val reportDataList: List<ReportData>,private val context:Context) :
    RecyclerView.Adapter<ReportDataAdapter.ViewHolder>() {

    class ViewHolder(val binding: FeedItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FeedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reportData = reportDataList[position]
        val shimmer: Shimmer = Shimmer.ColorHighlightBuilder()
            .setBaseColor(R.color.black)
            .setHighlightColor(R.color.black)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()
        holder.binding.shimmerLayout.setShimmer(shimmer)
        holder.binding.shimmerLayout.startShimmer()
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
                    val userPhoto = snapshot.child("profileImage").getValue(String::class.java)
                    if (!userPhoto.isNullOrEmpty()) {
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
        Glide.with(context)
            .asBitmap()
            .load(reportData.photoUrl)
            .apply(RequestOptions().dontTransform())
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    holder.binding.feedItemImage.setImage(ImageSource.bitmap(resource))
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    // Handle placeholder if needed
                }
            })
        holder.binding.shimmerLayout.stopShimmer()
        holder.binding.shimmerLayout.hideShimmer()
    }

    override fun getItemCount(): Int {
        return reportDataList.size
    }
}
