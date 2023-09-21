package com.example.civilink.UserProfile
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.civilink.R
import com.example.civilink.data.User
import com.squareup.picasso.Picasso
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DisplayProfileFragment : Fragment() {
    private var userId: String? = null
    private var userName: String? = null
    private var userEmail: String? = null
    private var userPhotoUrl: String? = null
    private var auth: FirebaseAuth? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var name: TextView
    private lateinit var email :TextView
    private lateinit var userIdTextView : TextView
    private lateinit var profileImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        sharedPreferences = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "")
        userName = sharedPreferences.getString("userName", "")
        userEmail = sharedPreferences.getString("userEmail", "")
        userPhotoUrl = sharedPreferences.getString("userPhotoUrl", "")
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_display_profile, container, false)
        profileImage = view.findViewById(R.id.userProfileImage)
        name = view.findViewById(R.id.userName)
        email = view.findViewById(R.id.userEmail)
        userIdTextView = view.findViewById(R.id.UserId)

        if (userId.isNullOrEmpty() || userName.isNullOrEmpty() || userEmail.isNullOrEmpty() || userPhotoUrl.isNullOrEmpty()) {
            fetchUserDataFromFirebase()
        } else {
            name.text = userName
            email.text = userEmail
            userIdTextView.text = userId
            Picasso.get().load(userPhotoUrl).into(profileImage)
        }

        return view
    }

    private fun fetchUserDataFromFirebase() {
        val uid = auth?.currentUser?.uid
        val userRef = uid?.let { FirebaseDatabase.getInstance().reference.child("users").child(it) }

        userRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if (user != null) {
                        val fetchedUserId = user.uid
                        val fetchedUserName = user.name
                        val fetchedUserEmail = user.emailId
                        val fetchedUserPhotoUrl = user.profileImage
                        // Update SharedPreferences with fetched data
                        val editor = sharedPreferences.edit()
                        editor.putString("userId", fetchedUserId)
                        editor.putString("userName", fetchedUserName)
                        editor.putString("userEmail", fetchedUserEmail)
                        editor.putString("userPhotoUrl", fetchedUserPhotoUrl)
                        editor.apply()
                        // Update UI with fetched data
                        name.text = fetchedUserName
                        email.text = fetchedUserEmail
                        userIdTextView.text = fetchedUserName
                        Picasso.get().load(fetchedUserPhotoUrl).into(profileImage)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle onCancelled as needed
            }
        })
    }
}
