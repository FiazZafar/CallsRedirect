package com.sahiwal.callsredirect.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.sahiwal.callsredirect.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fiver.clientapp.adapter.CallHistoryAdapter
import com.fiver.clientapp.dataclasses.CallHistory
import com.sahiwal.callsredirect.dataclasses.CallLogResponse
import com.sahiwal.callsredirect.interfaces.MillisAIApi
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadHistory : ProgressBar
    private lateinit var adapter: CallHistoryAdapter
    private lateinit var millisApiService: MillisAIApi
    private val privateKey = "z4oHP32NBmepHfRUaJGdOX5PQS4JTHZI" // Replace with your private key

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_call_history, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        loadHistory = view.findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize Retrofit
        initializeRetrofit()

        // Initialize adapter with an empty list
        adapter = CallHistoryAdapter(emptyList())
        recyclerView.adapter = adapter

        // Fetch data from the API
        getData()

        return view
    }

    private fun initializeRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api-west.millis.ai/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        millisApiService = retrofit.create(MillisAIApi::class.java)
    }
    private fun getData() {
        val agentId = "-OKvLcAI_PHjlKHYklH3" // Replace with actual agent_id
        val authToken = privateKey  // If Bearer doesn't work, just use privateKey

        loadHistory.visibility = View.VISIBLE // Show ProgressBar before loading data

        lifecycleScope.launch {
            try {
                val response = millisApiService.getCallHistories(agentId, authToken, limit = 10)

                if (response.isSuccessful) {
                    val callLogs = response.body()?.items ?: emptyList()

                    if (callLogs.isNotEmpty()) {
                        val callHistoryList = mapToCallHistory(callLogs)
                        adapter = CallHistoryAdapter(callHistoryList)
                    } else {
                        Log.e("CallHistory", "No call logs found")
                        adapter = CallHistoryAdapter(getDummyData()) // Show dummy data
                    }
                } else {
                    Log.e("CallHistory", "Failed: ${response.errorBody()?.string()}")
                    adapter = CallHistoryAdapter(getDummyData()) // Use dummy data on failure
                }
            } catch (e: Exception) {
                Log.e("CallHistory", "Error: ${e.message}", e)
                adapter = CallHistoryAdapter(getDummyData()) // Use dummy data on exception
            } finally {
                recyclerView.adapter = adapter
                loadHistory.visibility = View.GONE // Hide ProgressBar after items are loaded
            }
        }
    }

    private fun mapToCallHistory(callLogs: List<CallLogResponse>): List<CallHistory> {
        return callLogs.map { callLog ->
            CallHistory(
                date = callLog.ts?.toLong()?.let { formatTimestamp(it) } ?: "N/A", // Convert timestamp to readable date
                duration = "${((callLog.duration ?: 0.0) / 60).toInt()} min", // Convert seconds to minutes
                durata = "${((callLog.duration ?: 0.0) / 60).toInt()} min", // Additional duration field
                timestamp = (callLog.ts?.toLong() ?: 0L).toString(), // Convert timestamp to Long safely
                status = callLog.call_status ?: "Unknown" // Handle null status
            )
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date((timestamp * 1000))) // Convert Unix timestamp (seconds) to milliseconds
    }

    private fun getDummyData(): List<CallHistory> {
        return listOf(
            CallHistory("2025-03-09", "5 min", "5 min", "12:30 PM", "Completed"),
            CallHistory("2025-03-08", "3 min", "3 min", "11:45 AM", "Missed"),
            CallHistory("2025-03-07", "10 min", "10 min", "4:20 PM", "Ongoing"),
            CallHistory("2025-03-06", "7 min", "7 min", "3:15 PM", "Completed"),
            CallHistory("2025-03-05", "2 min", "2 min", "10:00 AM", "Missed")
        )
    }
}