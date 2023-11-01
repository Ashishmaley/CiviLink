package com.example.civilink

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.civilink.data.ReportData
import com.example.civilink.data.models.ImageViewModel
import com.example.civilink.data.models.ProblemViewModel
import com.example.civilink.main_viewpager_fragments.CommentsBottomSheetFragment
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SolvedFragment : BottomSheetDialogFragment() {
    private var userId : String? = null
    private lateinit var imageViewModel: ImageViewModel
    private var reportId: String? = null
    private var dialog: Dialog? = null
    private lateinit var deleteButton: Button
    private lateinit var imageBitmap : Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageViewModel = ViewModelProvider(requireActivity()).get(ImageViewModel::class.java)
    }

    @SuppressLint("ResourceAsColor")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_solved, container, false)
        Log.d("MyBottomSheetFragment", "onCreateView called")
        val imageViewModel: ProblemViewModel by activityViewModels()
        val imageView = view.findViewById<SubsamplingScaleImageView>(R.id.imageView1)
        deleteButton = view.findViewById(R.id.deleteButton1)
        val likeButton = view.findViewById<ImageButton>(R.id.likeButton1)
        val like = view.findViewById<TextView>(R.id.like1)
        val address = view.findViewById<TextView>(R.id.useId1)
        val problemDescription = view.findViewById<TextView>(R.id.problem1)
        val ptitle = view.findViewById<TextView>(R.id.pTitle1)
        val time = view.findViewById<TextView>(R.id.timeAndDate1)
        val problemSolved = view.findViewById<Button>(R.id.problemSolved1)
        val image2 = view.findViewById<SubsamplingScaleImageView>(R.id.imageView2)
        val timeAndDate2 = view.findViewById<TextView>(R.id.timeAndDate2)

        timeAndDate2.text = imageViewModel.solvedTime

        val imageUrl1 = imageViewModel.imageUrl1
        if (!imageUrl1.isNullOrEmpty()) {
            Log.d("bottomUP", "$imageViewModel.imageUrl1")
            Glide.with(this)
                .asBitmap()
                .load(imageUrl1)
                .apply(RequestOptions().dontTransform())
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        image2.setImage(ImageSource.bitmap(resource))
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle placeholder if needed
                    }
                })
        }

        reportId = imageViewModel.reportId
        userId = imageViewModel.userEmail
        val shimmer: Shimmer = Shimmer.ColorHighlightBuilder()
            .setBaseColor(R.color.white)
            .setHighlightColor(R.color.white)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()
        view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout1).setShimmer(shimmer)
        view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout1).startShimmer()

        ptitle.text = imageViewModel.spinnerSelectedItem


        val userLikesRef = FirebaseDatabase.getInstance().getReference("user_report_likes")
        val userReportLikesRef = userLikesRef.child(imageViewModel.reportId!!)
        var reportLikeRef = userReportLikesRef.child(imageViewModel.userEmail!!)

        val usersWhoLikedRef = userLikesRef.child(imageViewModel.reportId!!)
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
                        usersWhoLikedRef.addListenerForSingleValueEvent(object :
                            ValueEventListener {
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
                        usersWhoLikedRef.addListenerForSingleValueEvent(object :
                            ValueEventListener {
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

        val formattedTimestamp = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            .format(imageViewModel.timestamp?.let { Date(it) })
        time.text = formattedTimestamp.toString()

        getUserEmail(userId.toString())

        val commentRef = FirebaseDatabase.getInstance().reference
            .child("comments")
            .child(reportId!!)

        commentRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get the count of children under the reportId node
                val count = dataSnapshot.childrenCount
                view.findViewById<TextView>(R.id.commentCount1).text = count.toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors
                Log.e("FirebaseError", "Error: ${databaseError.message}")
            }
        })

        val imageUrl = imageViewModel.selectedImageUrl

        if (imageUrl != null && imageUrl.isNotEmpty()) {
            Log.d("bottomUP", "$imageUrl")
            val latitude = imageViewModel.latitude ?: 0.0
            val longitude = imageViewModel.longitude ?: 0.0
            address.text = getAddressFromLocation(latitude, longitude)
            problemDescription.text = imageViewModel.problemDescription

            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .apply(RequestOptions().dontTransform())
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        imageView.setImage(ImageSource.bitmap(resource))
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle placeholder if needed
                    }
                })
        }

        setupDeleteButton()

        view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout1).stopShimmer()
        view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout1).hideShimmer()
        return view
    }
    private fun transferReportToNewDB(
        reportId: String,
        sourceDB: FirebaseDatabase,
        targetDB: FirebaseDatabase,
        onReportTransfer: (ReportData) -> Unit
    ) {
        val sourceReportRef = sourceDB.getReference("user_reports").child(userId!!).child(reportId)

        sourceReportRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val reportData = dataSnapshot.getValue(ReportData::class.java)

                val targetReportRef = targetDB.getReference("user_reports").child(userId!!).child(reportId)

                if (reportData != null) {
                    targetReportRef.setValue(reportData).addOnSuccessListener {
                        sourceReportRef.removeValue().addOnSuccessListener {
                            onReportTransfer(reportData)
                        }.addOnFailureListener { sourceError ->
                        }
                    }.addOnFailureListener { targetError ->
                    }
                } else {
                    // Handle the case where report data couldn't be fetched
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error if the data fetch is unsuccessful
                Log.e("FirebaseError", "Error: ${databaseError.message}")
            }
        })
    }
    private fun setupDeleteButton() {
        deleteButton.setOnClickListener {
            onDeleteButtonClick()
        }
    }

    private fun getUserEmail(userId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userEmail = snapshot.child("emailId").getValue(String::class.java)
                    if (!userEmail.isNullOrEmpty()) {
                        val uEmail = view?.findViewById<TextView>(R.id.uEmail)
                        uEmail?.text = userEmail
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors here
            }
        })
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(requireContext())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        return if (addresses?.isNotEmpty() == true) {
            addresses.get(0)?.getAddressLine(0) ?: "Address not found"
        } else {
            "Address not found"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            val customPeekHeight = resources.getDimensionPixelSize(R.dimen.peek_height) // Adjust this value as needed
            behavior.peekHeight = customPeekHeight
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        val commentButton = view.findViewById<CardView>(R.id.commentButton1)
        commentButton.setOnClickListener {
            val commentsBottomSheetFragment = CommentsBottomSheetFragment()
            commentsBottomSheetFragment.show(childFragmentManager, reportId)
        }
    }

    private fun onDeleteButtonClick() {
        val reportId = imageViewModel.reportId
        val photoUrl = imageViewModel.selectedImageUrl // Provide the photoUrl here
        if (!reportId.isNullOrEmpty()) {
            userId?.let { deleteReport(it, reportId, photoUrl.toString()) }
        }
        dismiss()
    }


    @SuppressLint("SetTextI18n")
    private fun deleteReport(userId: String, reportId: String, photoUrl: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("user_reports")
        val userReportsRef = databaseReference.child(userId).child(reportId)

        userReportsRef.removeValue()
            .addOnSuccessListener {
                Log.d("MyBottomSheetFragment", "Report deleted successfully")
                val problemDescription = view?.findViewById<TextView>(R.id.problem)
                problemDescription?.visibility =View.GONE
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)
                storageRef.delete()
                    .addOnSuccessListener {
                        Log.d("MyBottomSheetFragment", "Photo deleted successfully")
                        problemDescription?.visibility =View.GONE
                    }
                    .addOnFailureListener { e ->
                        Log.e("MyBottomSheetFragment", "Error deleting photo: $e")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MyBottomSheetFragment", "Error deleting report: $e")
            }
    }
    private fun showCustomProgressDialog(message: String) {
        val inflater = LayoutInflater.from(requireContext())
        val customProgressDialogView = inflater.inflate(R.layout.custom_progress_dialog, null)

        val lottieAnimationView = customProgressDialogView.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage = customProgressDialogView.findViewById<TextView>(R.id.textViewMessage)

        textViewMessage.text = message

        lottieAnimationView.setAnimation(R.raw.loading)
        lottieAnimationView.playAnimation()

        dialog = Dialog(requireContext())
        dialog!!.setContentView(customProgressDialogView)
        dialog!!.setCancelable(false)
        dialog!!.show()
    }
}
