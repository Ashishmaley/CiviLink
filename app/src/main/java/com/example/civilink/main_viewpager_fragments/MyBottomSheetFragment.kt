package  com.example.civilink.main_viewpager_fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.civilink.R
import com.example.civilink.data.models.ImageViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.civilink.data.ReportData
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyBottomSheetFragment : BottomSheetDialogFragment() {
    private var userId : String? = null
    private lateinit var imageViewModel: ImageViewModel
    private var reportId: String? = null
    private var dialog: Dialog? = null
    private lateinit var deleteButton: Button
    private lateinit var imageBitmap : Bitmap
    var userCurrentLatitude: Double = 0.0
    var userCurrentLongitude: Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageViewModel = ViewModelProvider(requireActivity()).get(ImageViewModel::class.java)
    }

    @SuppressLint("ResourceAsColor", "MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_bottom_sheet, container, false)
        Log.d("MyBottomSheetFragment", "onCreateView called")
        val imageViewModel: ImageViewModel by activityViewModels()
        val imageView = view.findViewById<SubsamplingScaleImageView>(R.id.imageView)
        deleteButton = view.findViewById(R.id.deleteButton)
        val likeButton = view.findViewById<ImageButton>(R.id.likeButton)
        val like = view.findViewById<TextView>(R.id.like)
        val address = view.findViewById<TextView>(R.id.useId)
        val problemDescription = view.findViewById<TextView>(R.id.problem)
        val ptitle = view.findViewById<TextView>(R.id.pTitle)
        val time = view.findViewById<TextView>(R.id.timeAndDate)
        val problemSolved = view.findViewById<Button>(R.id.problemSolved)
        reportId = imageViewModel.reportId
        userId = imageViewModel.userEmail
        val shimmer: Shimmer = Shimmer.ColorHighlightBuilder()
            .setBaseColor(R.color.white)
            .setHighlightColor(R.color.white)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()
        view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout).setShimmer(shimmer)
        view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout).startShimmer()

        ptitle.text = imageViewModel.spinnerSelectedItem

        val userLikesRef = FirebaseDatabase.getInstance().getReference("user_report_likes")
        val userReportLikesRef = userLikesRef.child(imageViewModel.reportId!!)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        if (currentUserId != null) {
            val reportLikeRef = userReportLikesRef.child(currentUserId)
            // Use reportLikeRef as needed
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
                view.findViewById<TextView>(R.id.commentCount).text = count.toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors
                Log.e("FirebaseError", "Error: ${databaseError.message}")
            }
        })

        val imageUrl = imageViewModel.selectedImageUrl

        if (!imageUrl.isNullOrEmpty()) {
            Log.d("bottomUP", "$imageUrl")
            val latitude = imageViewModel.latitude ?: 0.0
            val longitude = imageViewModel.longitude ?: 0.0
            address.text = getAddressFromLocation(latitude, longitude)
            problemDescription.text = imageViewModel.problemDescription

            val shimmerViewContainer = view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout)

            shimmerViewContainer.startShimmer()

            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .apply(RequestOptions().dontTransform())
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        imageView.setImage(ImageSource.bitmap(resource))
                        shimmerViewContainer.stopShimmer()
                        shimmerViewContainer.hideShimmer()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle placeholder if needed
                    }
                })
        }

        setupDeleteButton()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    userCurrentLatitude = it.latitude
                    userCurrentLongitude = it.longitude

                    // Now, you have the user's current latitude and longitude, use it as needed
                }
            }
            .addOnFailureListener { e: Exception ->
                // Handle location retrieval failure
            }

        problemSolved.setOnClickListener {
            val reportLatitude = imageViewModel.latitude
            val reportLongitude = imageViewModel.longitude
            val locationThreshold = 0.0001 // Adjust this threshold according to your requirements

            if (isSameLocation(reportLatitude!!, reportLongitude!!, userCurrentLatitude, userCurrentLongitude, locationThreshold)) {
                dispatchTakePictureIntent()
            } else {
                showCustomSeekBarNotification(R.raw.errorlottie,"Sorry,report location is not same as your location to submit Acknowledgment.")
            }
        }

        view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout).stopShimmer()
        view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout).hideShimmer()
        return view
    }

    private fun isSameLocation(
        reportLat: Double,
        reportLng: Double,
        userLat: Double,
        userLng: Double,
        threshold: Double
    ): Boolean {
        val latDiff = Math.abs(reportLat - userLat)
        val lngDiff = Math.abs(reportLng - userLng)
        return latDiff < threshold && lngDiff < threshold
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


    // Launch Camera Intent
    val REQUEST_IMAGE_CAPTURE = 1
    private var tempImageFile: File? = null
    private var currentPhotoPath: String = ""

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
                // Create the File where the photo should go
                tempImageFile = createImageFile()

                // Continue only if the File was successfully created
                tempImageFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.civilink.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    // Create a file to save the image
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    // Override onActivityResult to handle the result of the camera capture
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            showCustomProgressDialog("Loading..")

            val imageUri = tempImageFile?.let { FileProvider.getUriForFile(requireContext(), "com.example.civilink.fileprovider", it) }

            val storageRef = FirebaseStorage.getInstance().getReference().child("solvedReport/${reportId}.jpg")

            // Uploading the image using the URI
            val uploadTask = imageUri?.let { storageRef.putFile(it) }

            uploadTask?.addOnSuccessListener { taskSnapshot ->
                // Image uploaded successfully, get the download URL
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString() // Retrieve the URL of the uploaded image

                    val sourceDB = FirebaseDatabase.getInstance()
                    val targetDB = FirebaseDatabase.getInstance()

                    val reportId = imageViewModel.reportId ?: ""
                    val userId = imageViewModel.userEmail ?: ""

                    val userReportsSolvedRef = targetDB.getReference("user_reports_solved")
                    val solvedReport = HashMap<String, Any>()
                    solvedReport["reportId"] = reportId
                    solvedReport["solvedBy"] = userId
                    transferReportToNewDB(reportId, sourceDB, targetDB) { transferredReport ->
                        solvedReport["report"] = transferredReport
                        solvedReport["solvedTime"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        solvedReport["imageUrl"] = imageUrl // Add the image URL to the solved report

                        userReportsSolvedRef.child(reportId).setValue(solvedReport)
                            .addOnSuccessListener {
                                // Transfer the entire report data to the new branch 'solvedproblem'
                                Toast.makeText(requireContext(), "Report Marked as Solved", Toast.LENGTH_SHORT).show()
                                dialog?.dismiss()
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseError", "Error marking report as solved: $e")
                                Toast.makeText(requireContext(), "Failed to mark the report as Solved", Toast.LENGTH_SHORT).show()
                                dialog?.dismiss()
                            }
                    }
                }.addOnFailureListener {
                    // Handle failure to retrieve image URL
                }
            }?.addOnFailureListener { e ->
                // Handle failure to upload image
                dialog?.dismiss()
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(requireContext(), "Failed to mark the report as Solved", Toast.LENGTH_SHORT).show()
        }
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
        val commentButton = view.findViewById<CardView>(R.id.commentButton)
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
    private fun showCustomSeekBarNotification(animationResId: Int, message: String) {
        val inflater = LayoutInflater.from(requireContext())
        val customSeekBarView = inflater.inflate(R.layout.custom_seekbar_layout1, null)

        val lottieAnimationView = customSeekBarView.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage = customSeekBarView.findViewById<TextView>(R.id.textViewMessage)

        lottieAnimationView.setAnimation(animationResId) // Replace with your animation resource
        lottieAnimationView.playAnimation()

        // Set the message
        textViewMessage.text = message

        // Use a Dialog to display the custom SeekBar notification
        val customSeekBarDialog = Dialog(requireContext())
        customSeekBarDialog.setContentView(customSeekBarView)

        // Optional: Set dialog properties (e.g., background, dimensions, etc.)
        customSeekBarDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        customSeekBarDialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Show the custom SeekBar notification
        customSeekBarDialog.show()
    }

}
