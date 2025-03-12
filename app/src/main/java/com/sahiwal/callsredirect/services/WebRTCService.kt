package com.sahiwal.callsredirect.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.fiver.clientapp.helpers.RetrofitClient
import com.sahiwal.callsredirect.R
import com.sahiwal.callsredirect.helpers.WebRTCManager
import com.sahiwal.callsredirect.helpers.WebRTCMode
import org.webrtc.SessionDescription

class WebRTCService : Service() {
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var notificationManager: NotificationManager
    private var isCallActive = false
    private lateinit var sharedPreferences: SharedPreferences
    private var currentMode: WebRTCMode = WebRTCMode.CALL_REDIRECT
    private val baseUrl = "https://api-west.millis.ai"

    override fun onCreate() {
        super.onCreate()
        Log.d("WebRTCService", "Service created")

        sharedPreferences = this.getSharedPreferences("MySettings", Context.MODE_PRIVATE)

        RetrofitClient.setBaseUrl(baseUrl)

        webRTCManager = WebRTCManager(
            context = this,
            privateKey = "z4oHP32NBmepHfRUaJGdOX5PQS4JTHZI",
            agentId = "OKvLcAI_PHjlKHYklH3",
            millisApiService = RetrofitClient.millisApiService
        )
        webRTCManager.initializeWebRTC()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForegroundService()

        listenForIncomingCalls()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WebRTCService", "Service started")
        return START_STICKY
    }

    private fun listenForIncomingCalls() {
        webRTCManager.setOnIncomingCallListener { sessionDescription ->
            Log.d("WebRTCService", "Incoming call detected: Auto-answering...")
            autoAnswerCall(sessionDescription)
        }
    }

    private fun autoAnswerCall(sessionDescription: SessionDescription) {
        if (!isCallActive) {
            isCallActive = true
            currentMode = WebRTCMode.CALL_REDIRECT

            webRTCManager.createAndSendAnswer(sessionDescription, currentMode)
            webRTCManager.startAudioCapture()

            updateNotification("Auto-Answered Call", "Your call is active through WebRTC")
        }
    }

    private fun startForegroundService() {
        val channelId = "WebRTCServiceChannel"
        val channelName = "WebRTC Call Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Handles WebRTC call notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = createNotification("Call in Progress", "Waiting for incoming call...")
        startForeground(1, notification)
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, "WebRTCServiceChannel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.microphone)  // Ensure `ic_call` exists in `res/drawable`
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        notificationManager.notify(1, notification)
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
