package com.example.civilink
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var fadeInAnimation: Animation
    private var storage: FirebaseStorage? = null
    private var database: FirebaseDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spalsh_screen)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_anim)

        val splashScreenView = findViewById<ImageView>(R.id.splashImage)
        splashScreenView.startAnimation(fadeInAnimation)

        // Check for internet connectivity
        if (isNetworkConnected()) {
            Handler().postDelayed({
                val currentUser = auth.currentUser
                if (currentUser != null && currentUser.isEmailVerified) {
                    val uid = auth.currentUser!!.uid
                    database!!.reference
                        .child("users")
                        .child(uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    if (dataSnapshot.child("uid").exists() && dataSnapshot.child("name").exists()) {
                                        startActivity(Intent(this@SplashScreenActivity, WorkSpace::class.java))
                                        this@SplashScreenActivity.finish()
                                    } else {
                                        startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
                                        this@SplashScreenActivity.finish()
                                    }
                                } else {
                                    startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
                                    this@SplashScreenActivity.finish()
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
                                this@SplashScreenActivity.finish()
                                showCustomLottieToast(R.raw.networkerror, "Check Internet connection")
                            }
                        })
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }, 4000) // 2 seconds delay
        }
        else {
            // No internet connection, show a toast message or take appropriate action
            showCustomLottieToast(R.raw.networkerror, "No Internet Connection")
        }
    }

    private fun showCustomLottieToast(animationResId: Int, message: String) {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast_lottie_layout, null)

        // Customize the layout elements
        val lottieAnimationView = layout.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage = layout.findViewById<TextView>(R.id.textViewMessage)

        // Set the Lottie animation resource
        lottieAnimationView.setAnimation(animationResId)
        lottieAnimationView.playAnimation()

        textViewMessage.text = message

        val toast = Toast(this)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}

