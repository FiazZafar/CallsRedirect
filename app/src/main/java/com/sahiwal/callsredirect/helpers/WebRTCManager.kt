package com.sahiwal.callsredirect.helpers

import android.Manifest

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.fiver.clientapp.dataclasses.IceCandidatePayload
import com.fiver.clientapp.fragments.Offer
import com.fiver.clientapp.fragments.OfferRequest
import com.sahiwal.callsredirect.helpers.WebRTCMode
import com.sahiwal.callsredirect.interfaces.MillisAIApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*

class WebRTCManager(
    private val context: Context,
    private val millisApiService: MillisAIApi,
    private val privateKey: String,
    private val agentId: String
) {
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private lateinit var audioSource: AudioSource
    private lateinit var localAudioTrack: AudioTrack
    private var isRecording = false
    private var offerSentTime: Long = 0
    val latencyList = mutableListOf<Long>()

    private var onIncomingCallListener: ((SessionDescription) -> Unit)? = null

    fun initializeWebRTC() {
        Log.d("WebRTCManager", "Initializing WebRTC...")
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(EglBase.create().eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(EglBase.create().eglBaseContext))
            .createPeerConnectionFactory()

        audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)
        Log.d("WebRTCManager", "WebRTC initialized successfully")
    }

    fun createPeerConnection(observer: PeerConnection.Observer) {
        Log.d("WebRTCManager", "Creating PeerConnection...")
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, observer)?.apply {
            addTrack(localAudioTrack, listOf("media_stream"))
            Log.d("WebRTCManager", "Local audio track added to PeerConnection")
        }
    }

    fun setOnIncomingCallListener(listener: (SessionDescription) -> Unit) {
        this.onIncomingCallListener = listener
    }

    fun createAndSendAnswer(sessionDescription: SessionDescription, currentMode: WebRTCMode) {
        Log.d("WebRTCManager", "Creating and sending answer...")

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription) {
                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                sendAnswerToMillis(answer, currentMode)
                                Log.d("WebRTCManager", "Answer created and sent successfully")
                            }

                            override fun onSetFailure(error: String?) {
                                Log.e("WebRTCManager", "Failed to set local description: $error")
                            }

                            override fun onCreateSuccess(session: SessionDescription?) {}
                            override fun onCreateFailure(error: String?) {}
                        }, answer)
                    }

                    override fun onCreateFailure(error: String?) {
                        Log.e("WebRTCManager", "Failed to create answer: $error")
                    }

                    override fun onSetSuccess() {}
                    override fun onSetFailure(error: String?) {}
                }, MediaConstraints())
            }

            override fun onSetFailure(error: String?) {
                Log.e("WebRTCManager", "Failed to set remote description: $error")
            }

            override fun onCreateSuccess(session: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, sessionDescription)
    }

    private fun sendAnswerToMillis(answer: SessionDescription, currentMode: WebRTCMode) {
        val request = OfferRequest(
            agent_id = agentId,
            offer = Offer(
                sdp = answer.description,
                type = answer.type.canonicalForm()
            )
        )
        val authToken = "$privateKey"
        Log.d("WebRTCManager", "Sending answer with authToken: $authToken")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = millisApiService.sendOffer(authToken, request)
                if (response.isSuccessful) {
                    Log.d("WebRTCManager", "Answer successfully sent")
                } else {
                    Log.e("WebRTCManager", "Failed to send answer: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("WebRTCManager", "Error sending answer: ${e.message}", e)
            }
        }
    }

    fun startAudioCapture() {
        if (isRecording) {
            Log.d("WebRTCManager", "Audio capture already running")
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("WebRTCManager", "Microphone permission is missing!")
            return
        }

        isRecording = true
        Log.d("WebRTCManager", "Audio capture started")
    }

    fun stopAudioCapture() {
        isRecording = false
        Log.d("WebRTCManager", "Audio capture stopped")
    }

    fun close() {
        Log.d("WebRTCManager", "Closing WebRTC manager...")
        stopAudioCapture()
        peerConnection?.close()
        peerConnection = null
        Log.d("WebRTCManager", "WebRTC manager closed")
    }
}
