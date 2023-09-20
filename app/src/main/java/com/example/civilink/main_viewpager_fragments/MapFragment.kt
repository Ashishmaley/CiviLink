package com.example.civilink.main_viewpager_fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.airbnb.lottie.LottieAnimationView
import com.example.civilink.data.models.ImageViewModel
import com.example.civilink.R
import com.example.civilink.data.ReportData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapToggleImageView: ImageView
    private val markers = mutableListOf<Marker>()
    private lateinit var childEventListener: ChildEventListener


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        requestPermissions()
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        mapToggleImageView = view.findViewById(R.id.mapToggleImageView)
        return view
    }

    private fun requestPermissions() {
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
        val cameraPermission = Manifest.permission.CAMERA

        val fineLocationPermissionGranted =
            ContextCompat.checkSelfPermission(requireActivity(), fineLocationPermission) == PackageManager.PERMISSION_GRANTED
        val coarseLocationPermissionGranted =
            ContextCompat.checkSelfPermission(requireActivity(), coarseLocationPermission) == PackageManager.PERMISSION_GRANTED
        val cameraPermissionGranted =
            ContextCompat.checkSelfPermission(requireActivity(), cameraPermission) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationPermissionGranted || !coarseLocationPermissionGranted || !cameraPermissionGranted) {
            // Request permissions
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(fineLocationPermission, coarseLocationPermission, cameraPermission),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        if (isLocationPermissionGranted()) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            googleMap.isMyLocationEnabled = true
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12.0f))
                }
            }

            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            mapToggleImageView.setOnClickListener {
                Log.d("MapFragment", "ImageView clicked") // Add this line for debugging
                if (googleMap.mapType == GoogleMap.MAP_TYPE_NORMAL) {
                    googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE // Switch to Satellite view
                    mapToggleImageView.setImageResource(R.drawable.satellite) // Set the Satellite image
                } else {
                    googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL // Switch to Normal view
                    mapToggleImageView.setImageResource(R.drawable.satelliteoff) // Set the Normal image
                }
            }
            googleMap.setOnCameraIdleListener {
                updateMarkersBasedOnZoomLevel()
                setupDatabaseListener()
            }
            setupDatabaseListener()
        }
    }
    private fun setupDatabaseListener() {
        childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                fetchLocationDataFromFirebase(googleMap)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                fetchLocationDataFromFirebase(googleMap)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                fetchLocationDataFromFirebase(googleMap)
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        val userReportsRef = FirebaseDatabase.getInstance().getReference("user_reports")
        userReportsRef.addChildEventListener(childEventListener)
    }

    private fun isLocationPermissionGranted(): Boolean {

        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
        return (ContextCompat.checkSelfPermission(requireContext(), fineLocationPermission)
                == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
            requireContext(),
            coarseLocationPermission
        ) == PackageManager.PERMISSION_GRANTED)
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun fetchLocationDataFromFirebase(gMap: GoogleMap) {
        val database = FirebaseDatabase.getInstance()
        val desiredWidth = 120
        val desiredHeight = 120
        val originalMarkerBitmap =
            BitmapFactory.decodeResource(resources, R.drawable.handshakeappicon)
        val resizedMarkerBitmap =
            Bitmap.createScaledBitmap(originalMarkerBitmap, desiredWidth, desiredHeight, false)
        val customMarker = BitmapDescriptorFactory.fromBitmap(resizedMarkerBitmap)
        val userReportsRef = database.getReference("user_reports")
        val viewModel: ImageViewModel by activityViewModels()

        userReportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                markers.clear() // Clear existing markers
                for (userSnapshot in dataSnapshot.children) {
                    for (reportSnapshot in userSnapshot.children) {
                        val reportData = reportSnapshot.getValue(ReportData::class.java)
                        reportData?.let { data ->
                            val location = LatLng(data.latitude, data.longitude)
                            // Create marker options based on zoom level
                            val markerOptions = createMarkerOptions(location, data)
                            val tag = "${data.userId}|${data.problemDescription}|${data.photoUrl}|${data.latitude}|${data.longitude}|${reportSnapshot.key}"
                            val marker = googleMap.addMarker(markerOptions)
                            marker?.let { markers.add(it) }
                            marker!!.tag = tag
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                showCustomLottieToast(R.raw.networkerror, "Check Internet connection")
            }
        })

        googleMap.setOnMarkerClickListener { marker ->
            val tag = marker.tag as String?
            Log.d("map", "$tag")

            if (tag != null) {
                val tagParts = tag.split("|")
                if (tagParts.size == 6) {
                    val userId = tagParts[0]
                    val problemDescription = tagParts[1]
                    val imageUrl = tagParts[2]
                    val latitude = tagParts[3].toDoubleOrNull()
                    val longitude = tagParts[4].toDoubleOrNull()
                    val reportId = tagParts[5]

                    viewModel.selectedImageUrl = imageUrl
                    viewModel.userEmail = userId
                    viewModel.problemDescription = problemDescription
                    viewModel.latitude = latitude
                    viewModel.longitude = longitude
                    viewModel.reportId = reportId // Store the report ID
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(marker.position, 15.0f)
                    googleMap.animateCamera(cameraUpdate)
                    val bottomSheetFragment = MyBottomSheetFragment()
                    bottomSheetFragment.show(childFragmentManager, imageUrl)
                    Log.d("bundle", "$imageUrl")
                }
            }

            true
        }
    }

    private fun createMarkerOptions(location: LatLng, data: ReportData): MarkerOptions {
        // Create different marker icons based on zoom level
        val zoomLevel = googleMap.cameraPosition.zoom
        val icon = when {
            zoomLevel < 10 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            zoomLevel < 12 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        }

        return MarkerOptions()
            .position(location)
            .title("User Report")
            .snippet(data.problemDescription)
            .icon(icon)
    }

    private fun updateMarkersBasedOnZoomLevel() {
        val zoomLevel = googleMap.cameraPosition.zoom

        for (marker in markers) {
            val tag = marker.tag
            if (tag is ReportData) { // Check if the tag is of type ReportData
                val markerData = tag as ReportData
                val markerOptions = createMarkerOptions(marker.position, markerData)

                // Adjust marker visibility based on zoom level
                if (zoomLevel < 10 && markerOptions.icon == BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) {
                    marker.isVisible = true
                } else if (zoomLevel < 15 && markerOptions.icon == BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)) {
                    marker.isVisible = true
                } else marker.isVisible = markerOptions.icon == BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            }
        }
    }




    override fun onResume() {
        super.onResume()
        mapView.onResume()
        // Check if googleMap is initialized
        if (this::googleMap.isInitialized) {
            googleMap.clear() // Clear existing markers
            fetchLocationDataFromFirebase(googleMap)
        }
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

        val lottieAnimationView = layout.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage = layout.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.overlay))

        lottieAnimationView.setAnimation(animationResId)
        lottieAnimationView.playAnimation()

        textViewMessage.text = message

        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }
}
