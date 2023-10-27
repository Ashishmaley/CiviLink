package com.example.civilink

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.example.civilink.adapters.IntroPagerAdapter
import com.example.civilink.databinding.ActivityIntroPagerBinding
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class IntroPager : AppCompatActivity() {
    private var binding: ActivityIntroPagerBinding? = null
    private var introAdapter: IntroPagerAdapter? = null
    private lateinit var sharedPrefs: SharedPreferences
    private val PREFS_NAME = "MyPrefsFile"
    private val APP_OPENED_BEFORE = "app_opened_before"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroPagerBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.teal_700) // Set status bar color to black
            window.navigationBarColor = resources.getColor(R.color.app_bg) // Set navigation bar color to black
        }
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val appOpenedBefore = sharedPrefs.getBoolean(APP_OPENED_BEFORE, false)
        if (appOpenedBefore) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            with(sharedPrefs.edit()) {
                putBoolean(APP_OPENED_BEFORE, true)
                apply()
            }
        }

        val viewPager = binding!!.pager
        introAdapter = IntroPagerAdapter(this)
        viewPager.adapter = introAdapter
        val dotsIndicator = findViewById<DotsIndicator>(R.id.dots_indicator)
        dotsIndicator.attachTo(viewPager)
    }

    fun clicked(view: View) {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}