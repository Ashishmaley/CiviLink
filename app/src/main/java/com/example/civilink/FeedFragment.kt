package com.example.civilink

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.example.civilink.adapters.ReportDataAdapter
import com.example.civilink.data.ReportData1
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FeedFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var reportDataAdapter: ReportDataAdapter
    private var reportDataList = mutableListOf<ReportData1>()
    var rangeIn = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userLocation: Location
    private val kilometerRanges = listOf(1,5,10, 50, 100, 300, 500, 1000, 10000, 30000, 50000, 100000)
    private val reportDataMap = mutableMapOf<Int, MutableList<ReportData1>>()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)
        recyclerView = view.findViewById(R.id.feedReCycle)
        reportDataAdapter = ReportDataAdapter(reportDataList, requireContext())
        recyclerView.adapter = reportDataAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            reportDataMap.clear()
            fetchUserLocation()
            swipeRefreshLayout.isRefreshing = false // Stop the spinner
        }

        val button = view.findViewById<ImageButton>(R.id.filterButton)

        val textKm = view.findViewById<TextView>(R.id.RangeKm)


        button.setOnClickListener {
            val popupView = layoutInflater.inflate(R.layout.number_picker_popup_layout, null)
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
            )
            popupWindow.animationStyle = R.style.PopupAnimation

            // Dismiss the popup when clicked outside
            popupWindow.setBackgroundDrawable(ColorDrawable())
            popupWindow.isOutsideTouchable = true

            val values = listOf(1, 5, 10, 50, 100, 300, 500, 1000, 10000, 30000, 50000, 100000)

            val numberPicker = popupView.findViewById<NumberPicker>(R.id.numberPicker)
            numberPicker.minValue = 0
            numberPicker.maxValue = values.size-1
            numberPicker.displayedValues = values.map { it.toString() }.toTypedArray()

            val updateButton = popupView.findViewById<Button>(R.id.updateButton)
            updateButton.setOnClickListener {
                val selectedValue = numberPicker.value // Get the selected value from NumberPicker
                rangeIn = values[selectedValue]
                textKm.text = rangeIn.toString()
                updateReportData(rangeIn) // Apply update function with the selected value
                popupWindow.dismiss() // Dismiss the popup after applying the update
            }

            // Show the popup at the center of the screen
            popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fetchUserLocation()
    }
    private fun updateReportData(range: Int) {
        reportDataList = reportDataMap[rangeIn] ?: mutableListOf()
        reportDataAdapter = ReportDataAdapter(reportDataList, requireContext())
        recyclerView.adapter = reportDataAdapter
        if(swipeRefreshLayout.isRefreshing){
            swipeRefreshLayout.isRefreshing=false
        }
    }

    private fun fetchUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        userLocation = it
                        fetchLocationDataFromFirebase()
                    } ?: run {
                        showCustomSeekBarNotification(R.raw.errorlottie,"Failed to fetch location. Please make sure location services are enabled.")
                    }
                }
        }
    }

    private fun fetchLocationDataFromFirebase() {
        val database = FirebaseDatabase.getInstance()
        val userReportsRef = database.getReference("user_reports")

        userReportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    for (reportSnapshot in userSnapshot.children) {
                        val reportId = reportSnapshot.key
                        val reportData = reportSnapshot.getValue(ReportData1::class.java)

                        if (reportData != null) {
                            reportData.reportId = reportId

                            val reportLocation = Location("")
                            reportLocation.latitude = reportData.latitude
                            reportLocation.longitude = reportData.longitude

                            val distance = userLocation.distanceTo(reportLocation)
                            val distanceInKm = distance / 1000

                            for (range in kilometerRanges) {
                                if (distanceInKm <= range) {
                                    if (reportDataMap[range] == null) {
                                        reportDataMap[range] = mutableListOf()
                                    }
                                    reportDataMap[range]?.add(reportData)
                                }
                            }
                        }
                    }
                }
                // Set the initial data to 10 km range
                reportDataList = reportDataMap[rangeIn] ?: mutableListOf()
                reportDataAdapter = ReportDataAdapter(reportDataList, requireContext())
                recyclerView.adapter = reportDataAdapter

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event
            }
        })
        if(swipeRefreshLayout.isRefreshing){
            swipeRefreshLayout.isRefreshing=false
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    private fun showCustomSeekBarNotification(animationResId: Int, message: String) {
        // Inflate the custom SeekBar layout
        val inflater = LayoutInflater.from(requireContext())
        val customSeekBarView = inflater.inflate(R.layout.custom_seekbar_layout1, null)

        // Customize the layout elements
        val lottieAnimationView = customSeekBarView.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage = customSeekBarView.findViewById<TextView>(R.id.textViewMessage)

        // Set Lottie animation resource
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
