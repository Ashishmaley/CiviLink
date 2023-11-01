package com.example.civilink.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.airbnb.lottie.LottieAnimationView
import com.example.civilink.R

class IntroPagerAdapter(private val context: Context): PagerAdapter() {
    private var layoutInflater: LayoutInflater = LayoutInflater.from(context)
    val background = arrayOf(
        R.raw.dotpatternbackground,
        R.raw.background1,
        R.raw.fullscreen
    )

    val pageAmin = arrayOf(
        R.raw.mappp,
        R.raw.mobile,
        R.raw.newsun
    )
    val textArr = arrayOf("Welcome!","In a world filled with civic challenges, our mission is to empower communities through technology. We present to you an innovative solution that revolutionizes the way we address civic issues such as water supply disruptions , water quality issues in ponds/lakes, urban flooding, and drainage problem, and more they can submit civil problem like road, garbage etc.\n : \"Civic Report\"","Civic Report aims to bridge the gap between citizens and government, offering benefits such as effortless reporting, real-time issue tracking, and enhanced community engagement. Together, we can build stronger, more responsive communities.\n")

    override fun getCount(): Int = pageAmin.size


    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as RelativeLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = layoutInflater.inflate(R.layout.slider, container, false)
        val myLayout = view.findViewById<View>(R.id.myLayout)
        val anim = view.findViewById<View>(R.id.anim)
        val heading = view.findViewById<View>(R.id.heading)
        val done = view.findViewById<View>(R.id.doneBtn)
        if(position==2){
            done.visibility = View.VISIBLE
        }
        else done.visibility = View.GONE
        if (myLayout is LottieAnimationView) {
            myLayout.setAnimation(background[position])
            myLayout.playAnimation()
        }

        if (anim is LottieAnimationView) {
            anim.setAnimation(pageAmin[position])
            anim.playAnimation()
        }

        if (heading is TextView) {
            heading.text = textArr[position]
        }
        container.addView(view)
        return view
    }
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val view = `object` as View
        container.removeView(view)

        if (view is LottieAnimationView) {
            view.cancelAnimation()
        }
    }
}