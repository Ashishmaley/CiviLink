package com.example.civilink

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class WorkSpace : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var fireBaseAuth: FirebaseAuth
    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var currentPhotoPath: String
    private lateinit var googleMap: GoogleMap
    private lateinit var database: DatabaseReference
    private lateinit var problemDescription: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_space)
        fireBaseAuth = FirebaseAuth.getInstance()
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermissions()
        val editTextProblemDescription = findViewById<EditText>(R.id.editTextProblemDescription)
        val floatingSave = findViewById<LottieAnimationView>(R.id.floatingSave)

        floatingSave.setOnClickListener {
            problemDescription = editTextProblemDescription.text.toString()
            editTextProblemDescription.text.clear()
            if (problemDescription.isNotEmpty()) {
                capturePhoto()
            } else {
                showCustomLottieToast(R.raw.errorlottie,"Problem description cannot be empty")
            }
        }
    }

    private fun requestPermissions() {
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
        val cameraPermission = Manifest.permission.CAMERA

        val fineLocationPermissionGranted =
            ContextCompat.checkSelfPermission(this, fineLocationPermission) == PackageManager.PERMISSION_GRANTED
        val coarseLocationPermissionGranted =
            ContextCompat.checkSelfPermission(this, coarseLocationPermission) == PackageManager.PERMISSION_GRANTED
        val cameraPermissionGranted =
            ContextCompat.checkSelfPermission(this, cameraPermission) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationPermissionGranted || !coarseLocationPermissionGranted || !cameraPermissionGranted) {
            // Request permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(fineLocationPermission, coarseLocationPermission, cameraPermission),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun fetchCurrentLocation(callback: (LatLng?) -> Unit) {
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION

        if (ContextCompat.checkSelfPermission(this, fineLocationPermission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(fineLocationPermission),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            callback(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                callback(location?.let { LatLng(it.latitude, it.longitude) })
            }
            .addOnFailureListener { exception ->
                // Handle any errors that may occur
                callback(null)
            }
    }

    private fun capturePhoto() {
        val takePictureIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }

            photoFile?.also {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.example.civilink.fileprovider",
                    it
                )
                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)!!
        val image = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )

        currentPhotoPath = image.absolutePath
        return image
    }

    @RequiresApi(34)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            uploadPhotoToFirebase(File(currentPhotoPath)) { photoUrl ->
                saveDataToFirebase(photoUrl, problemDescription)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        val location = LatLng(0.0, 0.0)
        val cameraZoom = 12.0f

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        googleMap.isMyLocationEnabled = true
        if (isLocationPermissionGranted()) {
            fetchLocationDataFromFirebase(googleMap)
        }
    }

    private fun uploadPhotoToFirebase(photoFile: File, onPhotoUploaded: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val photoRef = storageRef.child("photos/${photoFile.name}")

        val uploadTask = photoRef.putFile(Uri.fromFile(photoFile))

        uploadTask.addOnSuccessListener { taskSnapshot ->
            photoRef.downloadUrl.addOnSuccessListener { uri ->
                val photoUrl = uri.toString()
                onPhotoUploaded(photoUrl)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(34)
    private fun saveDataToFirebase(photoUrl: String, problemDescription: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            fetchCurrentLocation { location ->
                if (location != null) {
                    val database = FirebaseDatabase.getInstance()
                    val userReportsRef = database.getReference("user_reports")
                    val newReportRef = userReportsRef.child(userId).push()

                    val reportData = ReportData(
                        userId,
                        location.latitude,
                        location.longitude,
                        photoUrl,
                        problemDescription
                    )

                    newReportRef.setValue(reportData)
                        .addOnSuccessListener {
                            showCustomLottieToast(R.raw.donelottie,"Report saved successfully")
                        }
                        .addOnFailureListener {
                            showCustomLottieToast(R.raw.errorlottie,"Failed to save report")
                        }
                } else {
                    showCustomLottieToast(R.raw.verify,"Location not available")
                }
            }
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

        return (ContextCompat.checkSelfPermission(this, fineLocationPermission)
                == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
            this,
            coarseLocationPermission
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun fetchLocationDataFromFirebase(gMap: GoogleMap) {
        val database = FirebaseDatabase.getInstance()
        val customMarkerBitmap = BitmapFactory.decodeResource(resources, R.drawable.handshakeappicon)
        val desiredWidth = 120 // Adjust to your desired width
        val desiredHeight = 120 // Adjust to your desired height
        val originalMarkerBitmap = BitmapFactory.decodeResource(resources, R.drawable.handshakeappicon)
        val resizedMarkerBitmap = Bitmap.createScaledBitmap(originalMarkerBitmap, desiredWidth, desiredHeight, false)
        val customMarker = BitmapDescriptorFactory.fromBitmap(resizedMarkerBitmap)
        val userReportsRef = database.getReference("user_reports")

        userReportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    for (reportSnapshot in userSnapshot.children) {
                        val reportData = reportSnapshot.getValue(ReportData::class.java)
                        reportData?.let { data ->
                            val location = LatLng(data.latitude, data.longitude)
                            val markerOptions = MarkerOptions()
                                .position(location)
                                .title("User Report")
                                .snippet(data.problemDescription)
                                .icon(customMarker)
                            gMap.addMarker(markerOptions)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database errors if needed
            }
        })
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
    private fun showCustomLottieToast(animationResId: Int, message: String) {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast_lottie_layout, null)

        // Customize the layout elements
        val lottieAnimationView = layout.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage = layout.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.setTextColor(ContextCompat.getColor(this, R.color.overlay))

        // Set the Lottie animation resource
        lottieAnimationView.setAnimation(animationResId)
        lottieAnimationView.playAnimation()

        textViewMessage.text = message

        val toast = Toast(this)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }
}
