package com.fiver.clientapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fiver.clientapp.dataclasses.CallHistory
import com.sahiwal.callsredirect.R


class CallHistoryAdapter(private val callHistoryList: List<CallHistory>) :
    RecyclerView.Adapter<CallHistoryAdapter.CallHistoryViewHolder>() {

    class CallHistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvDuration: TextView = view.findViewById(R.id.tv_duration)
        val tvDurata: TextView = view.findViewById(R.id.tv_durata)
        val tvTimestamp: TextView = view.findViewById(R.id.tv_timestamp)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_list, parent, false)
        return CallHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallHistoryViewHolder, position: Int) {
        val item = callHistoryList[position]
        holder.tvDate.text = item.date
        holder.tvDuration.text = item.duration
        holder.tvDurata.text = item.durata
        holder.tvTimestamp.text = item.timestamp
        holder.tvStatus.text = item.status
    }

    override fun getItemCount(): Int = callHistoryList.size
}
