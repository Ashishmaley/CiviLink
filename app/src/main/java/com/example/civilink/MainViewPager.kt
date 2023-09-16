package com.example.civilink

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.example.civilink.adapters.MyPagerAdapter
import com.example.civilink.main_viewpager_fragments.MapFragment
import com.example.civilink.main_viewpager_fragments.CameraFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainViewPager : AppCompatActivity() {
    private var isSwipingEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_viewpager)
        val userImg = findViewById<ImageView>(R.id.userProfile)

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
    }
}
