package com.example.civilink.main_viewpager_fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.civilink.FeedFragment
import com.example.civilink.R
import com.example.civilink.UserProfile.UserProfileEdit
import com.example.civilink.adapters.MyPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class MainViewPager : AppCompatActivity() {
    private var isSwipingEnabled = true
    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_viewpager)
        requestPermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.black) // Set status bar color to black
            // Set navigation bar color (if available)
            window.navigationBarColor = resources.getColor(R.color.black) // Set navigation bar color to black
        }
        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tabs)
        val fragments = listOf(
            MapFragment(),
            CameraFragment(),
            FeedFragment()
        )
        val adapter = MyPagerAdapter(this,fragments)
        viewPager.adapter = adapter

        val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                isSwipingEnabled = position != 0
                viewPager.isUserInputEnabled = isSwipingEnabled
            }
        }

        viewPager.registerOnPageChangeCallback(onPageChangeCallback)
        viewPager.isUserInputEnabled = true

        val tabIcons = listOf(
            R.drawable.google_icon,
            R.drawable.lens,
            R.drawable.feed
        )

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setIcon(tabIcons[position])
        }.attach()
        val userProfileImageView = findViewById<CircleImageView>(R.id.userProfile)
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        if (user != null) {
            val databaseReference = FirebaseDatabase.getInstance().reference
            val userId = user.uid // Use the user's ID obtained from Firebase Authentication
            val userReference = databaseReference.child("users").child(userId)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                // Inside your ValueEventListener
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val profileImageUrl = dataSnapshot.child("profileImage").value as String?

                        if (profileImageUrl != null) {
                            Picasso.get()
                                .load(profileImageUrl)
                                .placeholder(R.drawable.user)
                                .error(R.drawable.user)
                                .into(userProfileImageView)
                        } else {
                            // Handle the case where the profile image URL is not found.
                            showToast("Profile image URL not found.")
                        }
                    } else {
                        // Handle the case where the user's data does not exist.
                        showToast("User data does not exist.")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle any errors that occur during the fetch.
                    showToast("Error fetching data: " + databaseError.message)
                }

                // Define a showToast function
                private fun showToast(message: String) {
                    Toast.makeText(this@MainViewPager, message, Toast.LENGTH_SHORT).show()
                }

            })

        } else {
        }

        userProfileImageView.setOnClickListener {
            val intent = Intent(this,UserProfileEdit::class.java)
            startActivity(intent)
        }

    }
    private fun setStatusBarColor(colorResId: Int) {
        // Check if the device has a transparent status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = resources.getColor(colorResId, theme)
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


}
