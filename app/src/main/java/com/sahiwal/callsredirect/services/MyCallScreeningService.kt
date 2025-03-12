package com.sahiwal.callsredirect.services


import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi

class MyCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle.schemeSpecificPart
        Log.d("MyCallScreeningService", "Incoming call detected: $incomingNumber")

        val response = CallResponse.Builder().setDisallowCall(false).build()
        respondToCall(callDetails, response)

        // Start WebRTC Service for call redirection
        val intent = Intent(this, WebRTCService::class.java)
        intent.putExtra("caller_number", incomingNumber)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("MyCallScreeningService", "Starting WebRTCService as a foreground service")
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
