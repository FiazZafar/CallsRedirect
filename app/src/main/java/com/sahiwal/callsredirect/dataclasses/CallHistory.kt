package com.fiver.clientapp.dataclasses

data class CallHistory(
    val date: String,       // Date of the call
    val duration: String,   // Duration of the call
    val durata: String,     // Additional duration field (if needed)
    val timestamp: String,  // Timestamp of the call
    val status: String      // Status of the call (e.g., "Completed", "Missed")
)