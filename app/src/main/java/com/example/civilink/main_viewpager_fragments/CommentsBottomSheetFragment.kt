package com.example.civilink.main_viewpager_fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.civilink.R
import com.example.civilink.adapters.CommentAdapter
import com.example.civilink.data.Comment
import com.example.civilink.data.models.ImageViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class CommentsBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentAdapter
    private lateinit var reportId: String
    private lateinit var commentsRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var commentsListener: ChildEventListener
    private lateinit var imageViewModel: ImageViewModel
    private val commentsList = mutableListOf<Comment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase references and current user
        val database = FirebaseDatabase.getInstance()
        imageViewModel = ViewModelProvider(requireActivity()).get(ImageViewModel::class.java)
        currentUser = FirebaseAuth.getInstance().currentUser!!
        commentsRef = database.reference.child("comments")
        commentsListener = createCommentsListener()
        reportId = imageViewModel.reportId.toString()
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
        val email = currentUser.email.toString()
        val timestamp = System.currentTimeMillis()

        val commentRef = FirebaseDatabase.getInstance().reference
            .child("comments")
            .child(reportId)

        val newCommentRef = commentRef.push()
        val commentId = newCommentRef.key

        if (commentId != null) {
            val comment = Comment(commentText, userId, email ,timestamp)
            newCommentRef.setValue(comment)
                .addOnSuccessListener {
                    // Comment added successfully
                }
                .addOnFailureListener { error ->
                    // Handle error
                }
        }
    }

    private fun createCommentsListener(): ChildEventListener {
        return object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val comment = snapshot.getValue(Comment::class.java)
                comment?.let {
                    commentsList.add(it)
                    Log.d("CommentsBottomSheet", "Comment added: $it")
                    commentsAdapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle updated comment if needed
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
    override fun onStart() {
        super.onStart()
        val commentPath = "comments/$reportId"
        commentsRef = FirebaseDatabase.getInstance().reference.child(commentPath)
        commentsRef.addChildEventListener(commentsListener)
    }
}
