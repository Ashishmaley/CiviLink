package com.example.civilink.image_and_problem

import android.app.Dialog
import android.location.Address
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.civilink.R
import com.example.civilink.data.NotificationLocation
import com.example.civilink.data.models.SharedViewModel
import com.example.civilink.databinding.FragmentProblemStatmentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import android.location.Geocoder
import org.json.JSONObject
import java.io.IOException
import java.util.*

class ProblemStatmentFragment : Fragment() {
    private var _binding: FragmentProblemStatmentBinding? = null
    private lateinit var sharedViewModel: SharedViewModel
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var dialog: Dialog? = null
    private var photoUriString: String = ""
    private var problemStatement :String=""
    private var spinnerSelectedItem : String = ""
    private var locu: String = ""

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProblemStatmentBinding.inflate(inflater, container, false)
        return binding.root
    }

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
            "Water related Issues",
            "Damaged road Issues",
            "garbage Issues",
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
            problemStatement = editTextProblemStatement.text.toString().trim()
            spinnerSelectedItem = spinner.selectedItem.toString()
            val intValue = 0 // Replace with your actual integer value

            if (latitude != 0.0 && longitude != 0.0 && photoUriString.isNotEmpty() && problemStatement.isNotEmpty()) {
                showCustomProgressDialog("Loading...")
                uploadPhotoToFirebase(photoUriString) { photoUrl ->
                    saveDataToFirebase(
                        Uri.parse(photoUrl),
                        latitude,
                        longitude,
                        problemStatement,
                        spinnerSelectedItem,
                        intValue
                    )
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

        val uniquePhotoId =
            "${userId}_${reportId}_${System.currentTimeMillis()}.jpg" // Unique photo ID
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
            locu = getAddressFromLocation(latitude, longitude).toString()

            newReportRef.setValue(reportData)
                .addOnSuccessListener {
                    showCustomLottieToast(
                        R.raw.donelottie,
                        "Report saved successfully"
                    )
                    // Add the FCM notification here
                    sendNotificationToNearbyUsers(latitude, longitude)
                }
                .addOnFailureListener {
                    dialog?.dismiss()
                    showCustomLottieToast(R.raw.errorlottie, "Failed to save report")
                }
        }
    }

    fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        var addressText: String? = null

        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                val addressStringBuilder = StringBuilder()

                for (i in 0..address.maxAddressLineIndex) {
                    addressStringBuilder.append(address.getAddressLine(i)).append("\n")
                }

                addressText = addressStringBuilder.toString().trim()
            }
        } catch (e: IOException) {
            Log.e("Geocoder", "Error getting address from location", e)
        }

        return addressText
    }

    private fun sendNotificationToNearbyUsers(latitude: Double, longitude: Double) {
        val database = FirebaseDatabase.getInstance()
        val userLocationsRef = database.getReference("fcmTokens")

        // Assuming userLocations is a list of UserLocation objects
        userLocationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nearbyUsers = mutableListOf<NotificationLocation>()

                for (dataSnapshot in snapshot.children) {
                    val userLocation = dataSnapshot.getValue(NotificationLocation::class.java)
                    if (userLocation != null) {
                        val distance = calculateDistance(
                            userLocation.latitude,
                            userLocation.longitude,
                            latitude,
                            longitude
                        )

                        if (distance <= 1000.0) {
                            nearbyUsers.add(userLocation)
                        }
                    }
                }

                // Now you have a list of nearby users to send notifications to
                sendNotificationToUsers(nearbyUsers)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error retrieving user locations: $error")
            }
        })
    }
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371 // Earth radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    private fun sendNotificationToUsers(nearbyUsers: List<NotificationLocation>) {
        val userEmail = FirebaseAuth.getInstance().currentUser!!.email
        for (userLocation in nearbyUsers) {
            sendNotification(userLocation.token,userEmail.toString())
        }
        requireActivity().finish()
        dialog?.dismiss()
    }

    private fun sendNotification(token: String,userEmail: String) {
        val serverKey = "AAAAQrhY8K4:APA91bFRY45EPy88oToypD_qq1fHhoMSpe8eOmUwg-0BPHX5QOllwcaags50onP_-vvGKvhKTOwoHsE0h4QDbAky6O0AKScA5ppGUyIYnv87K47ZnzkdzUG16kYh3skyhbiV0mO8JAJe" // Replace with your FCM server key
        val url = "https://fcm.googleapis.com/fcm/send"

        // Create the notification message
        val notification = JSONObject()
        notification.put("title", "New report found near your Submitted by $userEmail")
        notification.put("body", "Type :- $spinnerSelectedItem , ProblemStatement :- ${problemStatement} at Location :- $locu")

        // Create the data payload
        val data = JSONObject()
        data.put("key1", "value1")

        // Create the FCM message
        val message = JSONObject()
        message.put("to", token)
        message.put("notification", notification)
        message.put("data", data)

        // Create a Volley JsonObjectRequest
        val request = object : JsonObjectRequest(Method.POST, url, message,
            Response.Listener { response ->
                // Handle success
            },
            Response.ErrorListener { error ->
                // Handle error
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "key=$serverKey"
                return headers
            }
        }

        Volley.newRequestQueue(/* YourContext */ requireContext()).add(request)
    }


    private fun showCustomProgressDialog(message: String) {
        val inflater = LayoutInflater.from(requireContext())
        val customProgressDialogView =
            inflater.inflate(R.layout.custom_progress_dialog, null)

        val lottieAnimationView =
            customProgressDialogView.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage =
            customProgressDialogView.findViewById<TextView>(R.id.textViewMessage)

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
        val lottieAnimationView =
            layout.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
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