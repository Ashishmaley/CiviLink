package  com.example.civilink.main_viewpager_fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.civilink.R
import com.example.civilink.data.models.ImageViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import android.location.Geocoder
import android.widget.Button
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyBottomSheetFragment : BottomSheetDialogFragment() {
    private var userId : String? = null
    private lateinit var imageViewModel: ImageViewModel
    private var reportId: String? = null
    private lateinit var deleteButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageViewModel = ViewModelProvider(requireActivity()).get(ImageViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_bottom_sheet, container, false)
        Log.d("MyBottomSheetFragment", "onCreateView called")
        val imageViewModel: ImageViewModel by activityViewModels()
        val imageView = view.findViewById<ImageView>(R.id.imageView)
        deleteButton = view.findViewById(R.id.deleteButton)
        val address = view.findViewById<TextView>(R.id.useId)
        val problemDescription = view.findViewById<TextView>(R.id.problem)
        var ptitle = view.findViewById<TextView>(R.id.pTitle)
        var like = view.findViewById<TextView>(R.id.like)
        var time = view.findViewById<TextView>(R.id.timeAndDate)

        ptitle.text = imageViewModel.spinnerSelectedItem
        like.text = imageViewModel.intValue.toString()

        val formattedTimestamp = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            .format(imageViewModel.timestamp?.let { Date(it) })
        time.text = formattedTimestamp.toString()




//        val commetCount = view.findViewById<TextView>(R.id.commentCount)










        reportId = imageViewModel.reportId
        userId = imageViewModel.userEmail
        getUserEmail(userId.toString())

        val imageUrl = imageViewModel.selectedImageUrl

        if (imageUrl != null && imageUrl.isNotEmpty()) {
            Log.d("bottomUP", "$imageUrl")
            val latitude = imageViewModel.latitude ?: 0.0
            val longitude = imageViewModel.longitude ?: 0.0
            address.text = getAddressFromLocation(latitude, longitude)
            problemDescription.text = imageViewModel.problemDescription
            Picasso.get()
                .load(imageUrl)
                .into(imageView)
        }

        setupDeleteButton()
        return view
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
            commentsBottomSheetFragment.show(childFragmentManager, commentsBottomSheetFragment.tag)
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
}
