package com.example.civilink.SLF

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.example.civilink.main_viewpager_fragments.MainViewPager
import com.example.civilink.ProfileActivity
import com.example.civilink.R
import com.example.civilink.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var fireBaseAuth: FirebaseAuth

    // Regular expression to check if the password meets the required restrictions
    private val PASSWORD_PATTERN: Regex =
        Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@$!%*?&])[A-Za-z\\d$@$!%*?&]{8,}")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val animationView: LottieAnimationView = binding.ani
        animationView.playAnimation()
        fireBaseAuth = FirebaseAuth.getInstance()
        val inflater = requireActivity().layoutInflater

        val animator2 = ObjectAnimator.ofFloat(binding.button,"alpha", 0.0f, 1.0f)
        animator2.duration = 1000 // Animation duration in milliseconds
        animator2.start()

        binding.button.setOnClickListener {
            val email = binding.editTextTextEmailAddress2.text.toString()
            val pass = binding.editTextNumberPassword.text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                if (PASSWORD_PATTERN.matches(pass)) {
                    binding.progressBar.visibility = View.VISIBLE
                    // Use lifecycleScope for coroutines
                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = signInWithEmailAndPassword(email, pass)
                        binding.progressBar.visibility = View.GONE
                        handleAuthResult(result)
                    }
                } else {
                    showCustomSeekBarNotification(
                        R.raw.errorlottie,
                        "Password must have at least 8 characters, contain at least one uppercase, one lowercase, one number, and one special character.",
                    )
                }
            } else {
                if(email.isEmpty() && pass.isEmpty()){
                    showCustomSeekBarNotification(
                        R.raw.verify,
                        "*Email is a required field,\n*Password is a required field.",
                    )
                }
                else if(email.isEmpty()){
                    binding.editTextTextEmailAddress2.error="*required field"
                }
                else if(pass.isEmpty()){
                    showCustomSeekBarNotification(
                        R.raw.verify, // Change to your desired icon
                        "*Password is a required field",
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun signInWithEmailAndPassword(email: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                fireBaseAuth.signInWithEmailAndPassword(email, password).await()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun handleAuthResult(success: Boolean) {
        if (success) {
            val currentUser = fireBaseAuth.currentUser
            if (currentUser != null) {
                if (currentUser.isEmailVerified) {
                    showCustomLottieToast(
                        R.raw.donelottie, // Change to your desired icon
                        "Login successful Set your profile.",
                    )
                    val intent = Intent(requireContext(), ProfileActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    showCustomLottieToast(
                        R.raw.verify, // Change to your desired icon
                        "Please verify your email to continue",
                        )
                    fireBaseAuth.signOut()
                }
            }
        } else {
            showCustomLottieToast(
                R.raw.errorlottie, // Change to your desired icon
                "Login failed",
            )
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

        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }


    override fun onStart() {
        super.onStart()
        val currentUser = fireBaseAuth.currentUser
        if (currentUser != null) {
            if (currentUser.isEmailVerified) {
                // Check if the user's profile data exists in the database
                val uid = currentUser.uid
                val databaseReference = FirebaseDatabase.getInstance().reference
                    .child("users")
                    .child(uid)

                databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // User's profile data exists, navigate to the main activity
                            showCustomLottieToast(
                                R.raw.welcome, // Change to your desired icon
                                "Welcome back...",
                            )
                            val intent = Intent(requireActivity(), MainViewPager::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        } else {
                            // User's profile data doesn't exist, redirect to create user profile
                            showCustomLottieToast(
                                R.raw.donelottie, // Change to your desired icon
                                "your Email is verified set your profile",
                            )
                            val intent = Intent(requireActivity(), ProfileActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        showCustomSeekBarNotification(
                            R.raw.networkerror, // Change to your desired icon
                            "Oops...,Check your Internet connection",
                            )
                    }
                })
            } else {
                showCustomSeekBarNotification(
                    R.raw.verify,// Change to your desired icon
                    "Please verify your email to continue",
                )
                fireBaseAuth.signOut()
            }
        }

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
