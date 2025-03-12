package com.sahiwal.callsredirect.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.fiver.clientapp.helpers.RetrofitClient
import com.sahiwal.callsredirect.R
import com.sahiwal.callsredirect.helpers.WebRTCManager
import com.sahiwal.callsredirect.helpers.WebRTCMode

class WebRTCService : Service() {
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var notificationManager: NotificationManager
    private var isCallActive = false
    private lateinit var sharedPreferences: SharedPreferences
    private var currentMode: WebRTCMode = WebRTCMode.CALL_REDIRECT


    override fun onCreate() {
        super.onCreate()
        Log.d("WebRTCService", "Service created")

        sharedPreferences = this.getSharedPreferences("MySettings", Context.MODE_PRIVATE)
        // Load saved data
        // Load saved data
        val savedAgentId = sharedPreferences.getString("agentId", "")
        val savedPrivateKey = sharedPreferences.getString("publicKey", "")
        val savedServerUrl = sharedPreferences.getString("serverUrl", "")
        if (savedAgentId.isNullOrEmpty() || savedPrivateKey.isNullOrEmpty() || savedServerUrl.isNullOrEmpty()){
            Toast.makeText(this,"please set your Credentials in setting...", Toast.LENGTH_SHORT).show()
        }else{

            RetrofitClient.setBaseUrl(savedServerUrl)

             webRTCManager = WebRTCManager(
                context = this,
                privateKey = savedPrivateKey,
                agentId = savedAgentId,
                millisApiService = RetrofitClient.millisApiService
             )
            webRTCManager.initializeWebRTC()

            // Setup Notification
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            startForegroundService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WebRTCService", "Service started")
        if (!isCallActive) {
            isCallActive = true
            handleIncomingCall()
        }
        return START_STICKY
    }

    private fun handleIncomingCall() {
        Log.d("WebRTCService", "Handling incoming call...")
        currentMode = WebRTCMode.CALL_REDIRECT
        webRTCManager.createAndSendOffer(currentMode)
        webRTCManager.startAudioCapture()
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "WebRTCServiceChannel")
            .setContentTitle("Call in Progress")
            .setContentText("Your call is active through WebRTC")
            .setSmallIcon(R.drawable.microphone)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("WebRTCService", "Service destroyed")
        isCallActive = false
        webRTCManager.stopAudioCapture()
        webRTCManager.close()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
