package com.example.civilink.main_viewpager_fragments

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.example.civilink.R
import com.example.civilink.UserProfile.UserProfileEdit
import com.example.civilink.adapters.MyPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainViewPager : AppCompatActivity() {
    private var isSwipingEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_viewpager)
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
            CameraFragment()
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
            R.drawable.lens
        )

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setIcon(tabIcons[position])
        }.attach()
        val userProfileImageView = findViewById<ImageView>(R.id.userProfile)
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


}
