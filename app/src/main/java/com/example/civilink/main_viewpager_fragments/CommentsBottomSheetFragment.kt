package com.example.civilink.main_viewpager_fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.civilink.R
import com.example.civilink.adapters.CommentAdapter
import com.example.civilink.data.Comment
import com.example.civilink.data.NotificationLocation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import org.json.JSONObject
import java.util.HashMap

class CommentsBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentAdapter
    private lateinit var reportId: String
    private lateinit var commentsRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var commentsListener: ChildEventListener
    private val commentsList = mutableListOf<Comment>()
    private var email:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = FirebaseDatabase.getInstance()
        currentUser = FirebaseAuth.getInstance().currentUser!!
        commentsRef = database.reference.child("comments")
        commentsListener = createCommentsListener()
        reportId = tag ?: ""
        if (reportId.isEmpty()) {
            Toast.makeText(requireContext(),"Something went wrong.",Toast.LENGTH_SHORT).show()
        }
        val commentPath = "comments/$reportId"
        commentsRef = database.reference.child(commentPath)
        commentsListener = createCommentsListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comments_bottom_sheet, container, false)
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentsAdapter = CommentAdapter(commentsList)
        commentsRecyclerView.adapter = commentsAdapter
        val commentEditText = view.findViewById<EditText>(R.id.commentEditText)
        val addButton = view.findViewById<Button>(R.id.postCommentButton)

        addButton.setOnClickListener {
            val commentText = commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
                commentEditText.text.clear()
            }
        }
        return view
    }

    override fun onStop() {
        super.onStop()
        commentsRef.child(reportId).removeEventListener(commentsListener)
    }

    private fun addComment(commentText: String) {
        val userId = currentUser.uid
        email = currentUser.email.toString()
        val timestamp = System.currentTimeMillis()

        val commentRef = FirebaseDatabase.getInstance().reference
            .child("comments")
            .child(reportId)

        val newCommentRef = commentRef.push()
        val commentId = newCommentRef.key

        if (commentId != null) {
            val comment = Comment(commentText, userId, email, timestamp)
            newCommentRef.setValue(comment)
                .addOnSuccessListener {
                    notifyReportUser(email)
                }
                .addOnFailureListener { error ->
                    // Handle failure
                }
        }
    }

    private fun notifyReportUser(email: String) {
        val reportRef = FirebaseDatabase.getInstance().reference
            .child("user_reports")

        reportRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key
                    Log.d("FirebaseData", "User ID: $userId")

                    // Assuming each user node has child nodes with report IDs
                    val userReportsRef = FirebaseDatabase.getInstance().reference
                        .child("user_reports")
                        .child(userId!!)

                    userReportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userReportsSnapshot: DataSnapshot) {
                            for (reportSnapshot in userReportsSnapshot.children) {
                                val reportIdk = reportSnapshot.key
                                if (reportIdk == reportId) {
                                    // You found the user ID for the target report ID
                                    Log.d("FirebaseData", "Target User ID: $userId")

                                    // Now, you can proceed to get the FCM token and send a notification
                                    val userTokenRef = FirebaseDatabase.getInstance().reference
                                        .child("fcmTokens")
                                        .child(userId)

                                    userTokenRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(tokenSnapshot: DataSnapshot) {
                                            val userToken = tokenSnapshot.getValue(NotificationLocation::class.java)

                                            if (userToken != null && !userToken.token.isNullOrEmpty()) {
                                                Log.d("FirebaseData", "User Token: ${userToken.token}")
                                                sendNotification(userToken.token, email)
                                            }
                                            else{
                                                Log.d("FirebaseData", "User Token: ${userToken}")
                                            }

                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Handle error
                                        }
                                    })
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }


    private fun createCommentsListener(): ChildEventListener {
        return object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val comment = snapshot.getValue(Comment::class.java)
                comment?.let {
                    commentsList.add(it)
                    commentsAdapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle deleted comment if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle moved comment if needed
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        }
    }

    private fun sendNotification(token: String,userEmail: String) {
        val serverKey = "AAAAQrhY8K4:APA91bFRY45EPy88oToypD_qq1fHhoMSpe8eOmUwg-0BPHX5QOllwcaags50onP_-vvGKvhKTOwoHsE0h4QDbAky6O0AKScA5ppGUyIYnv87K47ZnzkdzUG16kYh3skyhbiV0mO8JAJe" // Replace with your FCM server key
        val url = "https://fcm.googleapis.com/fcm/send"

        // Create the notification message
        val notification = JSONObject()
        notification.put("title", "New comment on your report")
        notification.put("body", "comment post by $userEmail")

        // Create the data payload
        val data = JSONObject()
        data.put("key1", "value1")

        // Create the FCM message
        val message = JSONObject()
        message.put("to", token)
        message.put("notification", notification)
        message.put("data", data)

        // Create a Volley JsonObjectRequest
        val request = object : JsonObjectRequest(
            Method.POST, url, message,
            Response.Listener { response ->
                // Handle success
            },
            Response.ErrorListener { error ->
                // Handle error
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "key=$serverKey"
                return headers
            }
        }
        Volley.newRequestQueue(/* YourContext */ requireContext()).add(request)
    }

    override fun onStart() {
        super.onStart()
        val commentPath = "comments/$reportId"
        commentsRef = FirebaseDatabase.getInstance().reference.child(commentPath)
        commentsRef.addChildEventListener(commentsListener)
    }
    companion object {
        private const val REPORT_ID = "reportId"

        fun newInstance(reportId: String): CommentsBottomSheetFragment {
            val fragment = CommentsBottomSheetFragment()
            val args = Bundle()
            args.putString(REPORT_ID, reportId)
            fragment.arguments = args
            return fragment
        }
    }
}
