package com.example.civilink.image_and_problem

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.civilink.R
import com.example.civilink.data.ReportData
import com.example.civilink.data.models.SharedViewModel
import com.example.civilink.databinding.FragmentProblemStatmentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class ProblemStatmentFragment : Fragment() {
    private var _binding: FragmentProblemStatmentBinding? = null
    private lateinit var sharedViewModel: SharedViewModel
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var dialog: Dialog? = null
    private var photoUriString: String = ""
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProblemStatmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(34)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editTextProblemStatement = binding.inputText
        val buttonUploadData = binding.addOnMap
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        sharedViewModel.latitude.observe(requireActivity()) { lat ->
            latitude = lat ?: 0.0
        }
        sharedViewModel.longitude.observe(requireActivity()) { long ->
            longitude = long ?: 0.0
        }
        sharedViewModel.imageUri.observe(requireActivity()) { uri ->
            photoUriString = uri ?: ""
        }


        val options = arrayOf(
            "Water Quality Issues in Ponds/Lakes",
            "Urban Flooding",
            "Other"
        )
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            options
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinner = view.findViewById<Spinner>(R.id.dropdown)
        spinner.adapter = adapter


        buttonUploadData.setOnClickListener {
            val problemStatement = editTextProblemStatement.text.toString().trim()
            val spinnerSelectedItem = spinner.selectedItem.toString() // Get the selected spinner text
            val intValue = 0 // Replace with your actual integer value

            if (latitude != 0.0 && longitude != 0.0 && photoUriString.isNotEmpty() && problemStatement.isNotEmpty()) {
                showCustomProgressDialog("Loading...")
                uploadPhotoToFirebase(photoUriString) { photoUrl ->
                    saveDataToFirebase(Uri.parse(photoUrl), latitude, longitude, problemStatement, spinnerSelectedItem, intValue)
                }
            } else {
                showCustomLottieToast(R.raw.errorlottie, "Invalid data. Please fill in all fields.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun uploadPhotoToFirebase(photoUri: String, onPhotoUploaded: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val reportId = UUID.randomUUID().toString()

        val uniquePhotoId = "${userId}_${reportId}_${System.currentTimeMillis()}.jpg" // Unique photo ID
        val photoRef = storageRef.child("photos/$uniquePhotoId")

        val uploadTask = photoRef.putFile(Uri.parse(photoUri))

        uploadTask.addOnSuccessListener { taskSnapshot ->
            photoRef.downloadUrl.addOnSuccessListener { uri ->
                val photoUrl = uri.toString()
                onPhotoUploaded(photoUrl)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to upload photo", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(34)
    private fun saveDataToFirebase(
        photoUri: Uri,
        latitude: Double,
        longitude: Double,
        problemStatement: String,
        spinnerSelectedItem: String,
        intValue: Int
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid

            val database = FirebaseDatabase.getInstance()
            val userReportsRef = database.getReference("user_reports").child(userId)
            val newReportRef = userReportsRef.push()

            val reportData = HashMap<String, Any>()
            reportData["userId"] = userId
            reportData["latitude"] = latitude
            reportData["longitude"] = longitude
            reportData["photoUrl"] = photoUri.toString()
            reportData["problemStatement"] = problemStatement
            reportData["spinnerSelectedItem"] = spinnerSelectedItem
            reportData["intValue"] = intValue
            reportData["timestamp"] = ServerValue.TIMESTAMP

            newReportRef.setValue(reportData)
                .addOnSuccessListener {
                    dialog?.dismiss()
                    showCustomLottieToast(R.raw.donelottie, "Report saved successfully")
                    requireActivity().finish()
                }
                .addOnFailureListener {
                    dialog?.dismiss()
                    showCustomLottieToast(R.raw.errorlottie, "Failed to save report")
                }
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

    private fun showCustomLottieToast(animationResId: Int, message: String) {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast_lottie_layout, null)
        val lottieAnimationView = layout.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage = layout.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        lottieAnimationView.setAnimation(animationResId)
        lottieAnimationView.playAnimation()
        textViewMessage.text = message
        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }
}
