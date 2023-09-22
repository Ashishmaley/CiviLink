package com.example.civilink.main_viewpager_fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.airbnb.lottie.LottieAnimationView
import com.example.civilink.R
import com.example.civilink.data.ReportData
import com.example.civilink.data.models.ImageViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnCameraIdleListener{

    private lateinit var mapView: MapView
    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapToggleImageView: ImageView
    private val markers = mutableListOf<Marker>()
    private lateinit var childEventListener: ChildEventListener
    private var destinationLatitude: Double? = null
    private var destinationLongitude: Double? = null
    private val hotspotCircles = mutableListOf<Circle>()
    private val hotspotCounts = mutableMapOf<String, Int>()

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
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        val btnMyLocation = view?.findViewById<ImageButton>(R.id.btnMyLocation)
        val btnDirections = view?.findViewById<ImageButton>(R.id.btnDirections)
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
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18.0f))
                }
            }
            btnMyLocation?.setOnClickListener {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val userLocation = LatLng(location.latitude, location.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18.0f))
                    }
                }
            }
            googleMap.setOnCameraIdleListener(this)
            btnDirections?.setOnClickListener {
                // Check if the user's current location is available
                if (destinationLatitude != null && destinationLongitude != null) {
                    // Retrieve the user's current location
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val origin = "${location.latitude},${location.longitude}"
                            val destination = "$destinationLatitude,$destinationLongitude"
                            val uri =
                                Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$origin&destination=$destination")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            startActivity(intent)
                        } ?: run {
                            // Handle the case where the user's location is not available
                            Toast.makeText(
                                requireContext(),
                                "User's location not available.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Handle the case where one or more coordinates are missing
                    Toast.makeText(requireContext(), "Coordinates are missing.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            val hotspotToggle = view?.findViewById<ToggleButton>(R.id.toggleHotspotsButton)

            hotspotToggle?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Hotspots are enabled, so show them
                    createHotspotCircles(googleMap, googleMap.cameraPosition.zoom)
                } else {
                    // Hotspots are disabled, so remove them
                    removeHotspotCircles()
                }
            }

            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            mapToggleImageView.setOnClickListener {
                Log.d("MapFragment", "ImageView clicked") // Add this line for debugging
                if (googleMap.mapType == GoogleMap.MAP_TYPE_NORMAL) {
                    googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE // Switch to Satellite view
                    mapToggleImageView.setImageResource(R.drawable.satelliteoff) // Set the Satellite image
                } else {
                    googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL // Switch to Normal view
                    mapToggleImageView.setImageResource(R.drawable.satellite) // Set the Normal image
                }
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
    private fun removeHotspotCircles() {
        for (circle in hotspotCircles) {
            circle.remove()
        }
        hotspotCircles.clear()
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun fetchLocationDataFromFirebase(gMap: GoogleMap) {
        val database = FirebaseDatabase.getInstance()
        val userReportsRef = database.getReference("user_reports")
        val viewModel: ImageViewModel by activityViewModels()

        userReportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                markers.clear() // Clear existing markers
                hotspotCircles.clear()
                hotspotCounts.clear()
                val ZOOM_THRESHOLD = 1.0f
                val currentZoomLevel = gMap.cameraPosition.zoom
                for (userSnapshot in dataSnapshot.children) {
                    for (reportSnapshot in userSnapshot.children) {
                        val reportData = reportSnapshot.getValue(ReportData::class.java)
                        reportData?.let { data ->
                            val location = LatLng(data.latitude, data.longitude)
                            val zoomLevel = 20.0f
                            if (currentZoomLevel >= ZOOM_THRESHOLD){
                            val markerOptions = createMarkerOptions(location,zoomLevel)
                            val tag =
                                "${data.userId}|${data.problemStatement}|${data.photoUrl}|${data.latitude}|${data.longitude}|${reportSnapshot.key}|${data.spinnerSelectedItem}|${data.intValue}|${data.timestamp}"
                            val marker = googleMap.addMarker(markerOptions)
                            marker?.let { markers.add(it) }
                            marker!!.tag = tag}
                            val hotspotKey = "${data.latitude}|${data.longitude}"
                            hotspotCounts[hotspotKey] = hotspotCounts.getOrDefault(hotspotKey, 0) + 1
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
                if (tagParts.size == 9) {
                    val userId = tagParts[0]
                    val problemDescription = tagParts[1]
                    val imageUrl = tagParts[2]
                    val latitude = tagParts[3].toDoubleOrNull()
                    val longitude = tagParts[4].toDoubleOrNull()
                    destinationLatitude = tagParts[3].toDoubleOrNull()
                    destinationLongitude = tagParts[4].toDoubleOrNull()
                    val reportId = tagParts[5]
                    val spinnerSelectedItem = tagParts[6]
                    val intValue = tagParts[7]
                    val timestamp = tagParts[8]

                    // Update the ViewModel with the problem description
                    viewModel.problemDescription = problemDescription

                    viewModel.selectedImageUrl = imageUrl
                    viewModel.userEmail = userId
                    viewModel.latitude = latitude
                    viewModel.longitude = longitude
                    viewModel.reportId = reportId // Store the report ID
                    viewModel.spinnerSelectedItem = spinnerSelectedItem // Set spinnerSelectedItem
                    viewModel.intValue = intValue.toInt() // Set intValue
                    viewModel.timestamp = timestamp.toLongOrNull() // Set timestamp as Long (if parsing is successful)

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
    override fun onCameraIdle() {
        // This method will be called when the camera position changes (including zoom changes)
        val currentZoomLevel = googleMap.cameraPosition.zoom

        if (currentZoomLevel >= 9.0f) {
            updateMarkers()
        } else {
            clearMarkers()
        }
    }

    private fun updateMarkers() {
        fetchLocationDataFromFirebase(googleMap)
    }

    private fun clearMarkers() {
        googleMap.clear()
    }


    private fun createHotspotCircles(gMap: GoogleMap, currentZoomLevel: Float) {
        for ((hotspotKey, count) in hotspotCounts) {
            val latitudeLongitude = hotspotKey.split("|")
            if (latitudeLongitude.size == 2) {
                val latitude = latitudeLongitude[0].toDoubleOrNull()
                val longitude = latitudeLongitude[1].toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    val location = LatLng(latitude, longitude)
                    createHotspotCircle(gMap, location, count, currentZoomLevel)
                }
            }
        }
    }


    private fun createHotspotCircle(gMap: GoogleMap, location: LatLng, count: Int, currentZoomLevel: Float) {
        // Define the minimum and maximum radius for the hotspot circle
        val minRadiusMeters = 100.0
        val maxRadiusMeters = 1000.0

        val zoomToRadiusFactor = 100.0f // Adjust this factor as needed
        val radius = minRadiusMeters + (maxRadiusMeters - minRadiusMeters) * (currentZoomLevel / zoomToRadiusFactor)

        val circleOptions = CircleOptions()
            .center(location)
            .radius(radius)
            .strokeColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
            .fillColor(ContextCompat.getColor(requireContext(), R.color.overlay))

        val hotspotCircle = gMap.addCircle(circleOptions)
        hotspotCircles.add(hotspotCircle)
    }

    private fun createMarkerOptions(location: LatLng, zoomLevel: Float): MarkerOptions {
        val minSize = 30 // Minimum marker size in pixels
        val maxSize = 120 // Maximum marker size in pixels
        val desiredSize = minSize + (maxSize - minSize) * (zoomLevel / 15.0f) // Adjust 15.0f based on your desired reference zoom level

        // Make sure the size is within the desired range
        val markerSize = desiredSize.coerceIn(minSize.toFloat(), maxSize.toFloat())

        val originalMarkerBitmap = BitmapFactory.decodeResource(resources, R.drawable.location_1)
        val resizedMarkerBitmap = Bitmap.createScaledBitmap(
            originalMarkerBitmap,
            markerSize.toInt(),
            markerSize.toInt(),
            true
        )
        val customMarker = BitmapDescriptorFactory.fromBitmap(resizedMarkerBitmap)

        return MarkerOptions()
            .position(location)
            .icon(customMarker)
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
