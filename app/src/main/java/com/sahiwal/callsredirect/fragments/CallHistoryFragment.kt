package com.sahiwal.callsredirect.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sahiwal.callsredirect.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fiver.clientapp.adapter.CallHistoryAdapter
import com.fiver.clientapp.dataclasses.CallHistory

class CallHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CallHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_call_history, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Dummy Data (Replace with API Call)
        val callHistoryList = listOf(
            CallHistory("2025-03-09", "5 min", "Romanian term", "12:30 PM", "Completed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-08", "3 min", "Romanian term", "11:45 AM", "Missed"),
            CallHistory("2025-03-07", "10 min", "Romanian term", "4:20 PM", "Ongoing")
        )

        adapter = CallHistoryAdapter(callHistoryList)
        recyclerView.adapter = adapter

        return view
    }
}
