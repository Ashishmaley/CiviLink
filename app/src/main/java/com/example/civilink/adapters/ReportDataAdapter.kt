package com.example.civilink.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.example.civilink.R
import com.example.civilink.data.ReportData1
import com.example.civilink.databinding.FeedItemBinding
import com.example.civilink.main_viewpager_fragments.CommentsBottomSheetFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.github.glailton.expandabletextview.EXPAND_TYPE_DEFAULT
import io.github.glailton.expandabletextview.EXPAND_TYPE_LAYOUT
import io.github.glailton.expandabletextview.EXPAND_TYPE_POPUP
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportDataAdapter(private val reportDataList: List<ReportData1>, private val context: Context) :
    RecyclerView.Adapter<ReportDataAdapter.ViewHolder>() {

    private val userLikesRef = FirebaseDatabase.getInstance().getReference("user_report_likes")
    private val userDetailMap: MutableMap<String, String> = mutableMapOf()
    private lateinit var reportData:ReportData1
    class ViewHolder(val binding: FeedItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FeedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        reportData = reportDataList[position]

        if (!userDetailMap.containsKey(reportData.userId)) {
            fetchUserDetails(reportData, holder)
        } else {
            setUserDetailsToUI(holder, userDetailMap[reportData.userId])
        }

        val formattedTimestamp = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            .format(Date(reportData.timestamp!!))
        holder.binding.timeStamp.text = formattedTimestamp

        loadReportImage(holder, reportData)

        manageLikeFunctionality(holder, reportData)


        val commentRef = FirebaseDatabase.getInstance().reference
            .child("comments")
            .child(reportData.reportId!!)

        commentRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get the count of children under the reportId node
                val count = dataSnapshot.childrenCount
                holder.binding.commentCount.text = count.toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors
                Log.e("FirebaseError", "Error: ${databaseError.message}")
            }
        })


        holder.binding.Comments.setOnClickListener {
            val commentsBottomSheetFragment = CommentsBottomSheetFragment.newInstance(reportData.reportId!!)
            commentsBottomSheetFragment.show((context as AppCompatActivity).supportFragmentManager,reportDataList[position].reportId)
        }

        holder.binding.feedProblemDescription
            .setAnimationDuration(1000)
            .setReadMoreText("View More")
            .setReadLessText("View Less")
            .setCollapsedLines(1)
            .setIsExpanded(false)
            .setIsUnderlined(true)
            .setExpandType(EXPAND_TYPE_DEFAULT)
            .setEllipsizedTextColor(ContextCompat.getColor(context, R.color.bluee))

        holder.binding.feedProblemDescription.text = "${reportDataList[position].spinnerSelectedItem} :- ${reportDataList[position].problemStatement}"

    }

    private fun fetchUserDetails(reportData: ReportData1, holder: ViewHolder) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users").child(reportData.userId!!)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userEmail = snapshot.child("emailId").getValue(String::class.java)
                    val userPhoto = snapshot.child("profileImage").getValue(String::class.java)
                    userEmail?.let { userDetailMap[reportData.userId] = it }
                    userPhoto?.let { userDetailMap["photo_${reportData.userId}"] = it }

                    setUserDetailsToUI(holder, userEmail)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors here
            }
        })
    }

    private fun setUserDetailsToUI(holder: ViewHolder, userEmail: String?) {
        userEmail?.let {
            holder.binding.feedItemName.text = it
            val userPhoto = userDetailMap["photo_${reportData.userId!!}"]
            userPhoto?.let {
                Glide.with(holder.itemView.context)
                    .load(userPhoto)
                    .into(holder.binding.userPhoto)
            }
        }
    }

    fun updateData(newData: List<ReportData1>) {
        val reportDataList = ArrayList<ReportData1>()
        reportDataList.clear()
        reportDataList.addAll(newData)
        notifyDataSetChanged()
    }

    // Inside your loadReportImage function

    private fun loadReportImage(holder: ViewHolder, reportData: ReportData1) {
        Glide.with(context)
            .asBitmap()
            .load(reportData.photoUrl)
            .apply(
                RequestOptions()
                    .dontTransform()
                    .placeholder(R.drawable.handshakeappicon) // Set a placeholder
                    .error(R.drawable.handshakeappicon) // Set an error placeholder
            )
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original & resized image
            .override(700, 700) // Specify the size if necessary
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    holder.binding.feedItemImage.setImage(ImageSource.bitmap(resource))
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }


    private fun manageLikeFunctionality(holder: ViewHolder, reportData: ReportData1) {
        val likeButton = holder.binding.likeButton
        val like = holder.binding.like
        val userLikesRef = FirebaseDatabase.getInstance().getReference("user_report_likes")
        val userReportLikesRef = userLikesRef.child(reportData.reportId!!)
        var reportLikeRef = userReportLikesRef.child(reportData.userId!!)

        val usersWhoLikedRef = userLikesRef.child(reportData.reportId!!)
        usersWhoLikedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                like.text = dataSnapshot.childrenCount.toString()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error if the data fetch is unsuccessful
                Log.e("FirebaseError", "Error: ${databaseError.message}")
            }
        })


        reportLikeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                likeButton.setImageResource(if (dataSnapshot.exists()) R.drawable.heart else R.drawable.like)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error if the data fetch is unsuccessful
                Log.e("FirebaseError", "Error: ${databaseError.message}")
            }
        })


        likeButton.setOnClickListener {
            reportLikeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        reportLikeRef.removeValue() // Remove like
                        likeButton.setImageResource(R.drawable.like)
                        usersWhoLikedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                like.text = dataSnapshot.childrenCount.toString()
                            }
                            override fun onCancelled(databaseError: DatabaseError) {
                                // Handle error if the data fetch is unsuccessful
                                Log.e("FirebaseError", "Error: ${databaseError.message}")
                            }
                        })
                    } else {
                        reportLikeRef.setValue(true) // Add like
                        likeButton.setImageResource(R.drawable.heart)
                        usersWhoLikedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                like.text = dataSnapshot.childrenCount.toString()
                            }
                            override fun onCancelled(databaseError: DatabaseError) {
                                // Handle error if the data fetch is unsuccessful
                                Log.e("FirebaseError", "Error: ${databaseError.message}")
                            }
                        })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("FirebaseError", "Error: ${databaseError.message}")
                }
            })
        }
    }

    override fun getItemCount(): Int {
        return reportDataList.size
    }
}
