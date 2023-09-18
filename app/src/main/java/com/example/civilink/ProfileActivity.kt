
package com.example.civilink

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.civilink.data.User
import com.example.civilink.databinding.ActivityProfileBinding
import com.example.civilink.main_viewpager_fragments.MainViewPager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

@Suppress("DEPRECATION")

class ProfileActivity : AppCompatActivity() {

    var binding : ActivityProfileBinding? = null
    var auth : FirebaseAuth? = null
    var database: FirebaseDatabase?=null
    var uid : String? = null
    var storage:FirebaseStorage?=null
    private var dialog: Dialog? = null
    var selectedImage : Uri? = null
    private var profileImage :ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        supportActionBar?.hide()

        database = FirebaseDatabase.getInstance()
        storage= FirebaseStorage.getInstance()
        auth=FirebaseAuth.getInstance()

        profileImage =binding!!.profileImage

        profileImage!!.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 17)
        }


        binding!!.set.setOnClickListener {
            val name = binding!!.name.text.toString()
            if(name.isEmpty()) {
                binding!!.name.error = "please enter your name"
                showCustomLottieToast(R.raw.verify,"please enter your name")
            }
            if (selectedImage!= null&&name.isNotEmpty()) {
                val reference = storage!!.reference.child("Profile").child(auth!!.uid!!)
                reference.putFile(selectedImage!!).addOnCompleteListener{Task ->
                    if(Task.isSuccessful){
                        showCustomSeekBarNotification(R.raw.donelottie,"Profile created successfully")
                        reference.downloadUrl.addOnSuccessListener { uri->
                            val imageUri = uri.toString()
                            uid = auth!!.uid
                            val userName = binding!!.name.text.toString()
                            val email = auth!!.currentUser!!.email
                            val user = User(uid, userName, imageUri, email)

                            database!!.reference
                                .child("users")
                                .child(uid!!)
                                .setValue(user).addOnCompleteListener {
                                    dialog!!.dismiss()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                        }
                    }
                    else
                    {
                        showCustomSeekBarNotification(R.raw.networkerror,"Internet error")
                    }
                }
            }
            else {
                if(selectedImage==null&&name.isNotEmpty())
                    showCustomSeekBarNotification(R.raw.verify,"select image And provide name")
                else if(name.isNotEmpty())
                    showCustomLottieToast(R.raw.verify,"please enter your name")
                else if (selectedImage==null)
                    showCustomSeekBarNotification(R.raw.verify,"select image")
            }
            if(selectedImage!=null&&name.isNotEmpty())
            {
                showCustomProgressDialog("Loading...")
            }

        }

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==17)
        {
            if(data != null)
            {
                selectedImage=data.data

                profileImage!!.setImageURI(selectedImage)
            }
            else
                showCustomSeekBarNotification(R.raw.verify,"select image")
        }
    }
    override fun onStart() {
        super.onStart()

        // Check if the user is logged in
        if (auth!!.currentUser != null) {
            val uid = auth!!.currentUser!!.uid

            // Check if the user's profile data exists in the database
            database!!.reference
                .child("users")
                .child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // User's profile data exists, check if it contains all necessary fields
                            if (dataSnapshot.child("uid").exists() && dataSnapshot.child("name").exists()) {
                                showCustomSeekBarNotification(R.raw.donelottie,"User profile Found,Redirecting...")
                                startActivity(Intent(this@ProfileActivity, MainViewPager::class.java))
                                finish()
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        showCustomSeekBarNotification(R.raw.networkerror,"Check your Internet connection")
                    }
                })
        }
        else {
            startActivity(Intent(this@ProfileActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun showCustomSeekBarNotification(animationResId: Int, message: String) {
        // Inflate the custom SeekBar layout
        val inflater = LayoutInflater.from(this)
        val customSeekBarView = inflater.inflate(R.layout.custom_seekbar_layout1, null)

        // Customize the layout elements
        val lottieAnimationView = customSeekBarView.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage = customSeekBarView.findViewById<TextView>(R.id.textViewMessage)
        lottieAnimationView.setAnimation(animationResId) // Replace with your animation resource
        lottieAnimationView.playAnimation()
        textViewMessage.text = message
        val customSeekBarDialog = Dialog(this)
        customSeekBarDialog.setContentView(customSeekBarView)
        customSeekBarDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        customSeekBarDialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        customSeekBarDialog.show()
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
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }

    private fun showCustomProgressDialog(message: String) {
        val inflater = LayoutInflater.from(this)
        val customProgressDialogView = inflater.inflate(R.layout.custom_progress_dialog, null)

        val lottieAnimationView = customProgressDialogView.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val textViewMessage = customProgressDialogView.findViewById<TextView>(R.id.textViewMessage)

        textViewMessage.text = message

        lottieAnimationView.setAnimation(R.raw.loading)
        lottieAnimationView.playAnimation()

        dialog = Dialog(this)
        dialog!!.setContentView(customProgressDialogView)
        dialog!!.setCancelable(false)
        dialog!!.show()
    }
}
