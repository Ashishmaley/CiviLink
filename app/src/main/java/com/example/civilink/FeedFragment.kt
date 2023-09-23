package com.example.civilink

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.civilink.adapters.ReportDataAdapter
import com.example.civilink.data.ReportData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FeedFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var reportDataAdapter: ReportDataAdapter
    private val reportDataList = mutableListOf<ReportData>()
    private lateinit var databaseReference: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)
        recyclerView = view.findViewById(R.id.feedReCycle)
        reportDataAdapter = ReportDataAdapter(reportDataList)
        recyclerView.adapter = reportDataAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchLocationDataFromFirebase()
    }
    private fun fetchLocationDataFromFirebase() {
        val database = FirebaseDatabase.getInstance()
        val userReportsRef = database.getReference("user_reports")

        userReportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    for (reportSnapshot in userSnapshot.children) {
                        val reportData = reportSnapshot.getValue(ReportData::class.java)
                        if (reportData != null) {
                            reportDataList.add(reportData)
                        }
                    }
                    reportDataAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

}