package com.example.civilink.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.civilink.data.ReportData
import com.example.civilink.databinding.FeedItemBinding

class ReportDataAdapter(private val reportDataList: List<ReportData>) :
    RecyclerView.Adapter<ReportDataAdapter.ViewHolder>() {

    class ViewHolder(val binding: FeedItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FeedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reportData = reportDataList[position]
        holder.binding.feedItemName.text = "User: ${reportData.spinnerSelectedItem}"
        holder.binding.feedProblemDescription.text = reportData.problemStatement

        Glide.with(holder.itemView.context)
            .load(reportData.photoUrl)
            .preload()
        Glide.with(holder.itemView.context)
            .load(reportData.photoUrl)
            .into(holder.binding.feedItemImage)
    }


    override fun getItemCount(): Int {
        return reportDataList.size
    }
}
