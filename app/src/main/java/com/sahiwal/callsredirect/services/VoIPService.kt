package com.sahiwal.callsredirect.services;

import android.app.Service
import android.content.Intent
import android.os.IBinder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log

import androidx.core.app.NotificationCompat
import com.fiver.clientapp.dataclasses.IceCandidatePayload

import com.sahiwal.callsredirect.helpers.WebRTCManager
import com.sahiwal.callsredirect.R
import com.sahiwal.callsredirect.interfaces.MillisAIApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VoIPService : Service() {
    private lateinit var webRTCManager: WebRTCManager
    private val agentId: String = "-OKvLcAI_PHjlKHYklH3" // Replace with your agent ID
    private val privateKey: String = "z4oHP32NBmepHfRUaJGdOX5PQS4JTHZI" // Replace with your private key
    private lateinit var millisApiService: MillisAIApi
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        initializeRetrofit()
        webRTCManager = WebRTCManager(
            context = this,
            millisApiService = millisApiService,
            privateKey = privateKey,
            agentId = agentId
        )
        webRTCManager.initializeWebRTC()
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    private fun initializeRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api-west.millis.ai/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        millisApiService = retrofit.create(MillisAIApi::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val incomingCall = intent?.getBooleanExtra("incoming_call", false)
        if (incomingCall == true) {
            val observer = object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        val payload = IceCandidatePayload(
                            sdpMid = it.sdpMid,
                            sdpMLineIndex = it.sdpMLineIndex,
                            candidate = it.sdp
                        )
                        coroutineScope.launch {
                            try {
                                millisApiService.sendIceCandidate("Bearer $privateKey", payload)
                                Log.d("MYTAG", "ICE candidate sent successfully")
                            } catch (e: Exception) {
                                Log.e("MYTAG", "Failed to send ICE candidate: ${e.message}")
                            }
                        }
                    }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dataChannel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            }

            webRTCManager.createPeerConnection(observer)
            webRTCManager.createAndSendOffer()
            webRTCManager.startAudioCapture()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "voip_channel",
                "VoIP Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "voip_channel")
            .setContentTitle("VoIP Service")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.microphone)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        webRTCManager.close()
        coroutineScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}