
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
import com.sahiwal.callsredirect.interfaces.MillisAIApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*
import org.webrtc.AudioTrack

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
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    public var offerSentTime: Long = 0
    val latencyList = mutableListOf<Long>()

    // Initialize WebRTC
    fun initializeWebRTC() {
        Log.d("WebRTCManager", "Initializing WebRTC...")
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(EglBase.create().eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(EglBase.create().eglBaseContext))
            .createPeerConnectionFactory()

        audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)
        Log.d("WebRTCManager", "WebRTC initialized successfully")
    }

    // Create a PeerConnection
    fun createPeerConnection(observer: PeerConnection.Observer) {
        Log.d("WebRTCManager", "Creating PeerConnection...")
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, observer)?.apply {
            addTrack(localAudioTrack, listOf("media_stream"))
            Log.d("WebRTCManager", "Local audio track added to PeerConnection")
        }

        Log.d("WebRTCManager", "PeerConnection initialized")
    }

    // Start audio capture
    fun startAudioCapture() {
        if (audioRecord != null && isRecording) {
            Log.d("WebRTCManager", "Audio capture already running")
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("WebRTCManager", "Microphone permission is missing!")
            return
        }

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        isRecording = true
        audioRecord?.startRecording()
        Log.d("WebRTCManager", "Audio capture started")

        CoroutineScope(Dispatchers.IO).launch {
            val audioData = ByteArray(bufferSize)
            while (isRecording) {
                val bytesRead = audioRecord?.read(audioData, 0, audioData.size) ?: 0
                if (bytesRead > 0) {
                    // WebRTC automatically handles audio streaming, so no need to manually send audio data
                }
            }
        }
    }

    // Stop audio capture
    fun stopAudioCapture() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d("WebRTCManager", "Audio capture stopped")
    }

    // Create and send an offer to MillisAI
    fun createAndSendOffer(currentMode: WebRTCMode) {
        Log.d("WebRTCManager", "Creating and sending offer...")
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d("WebRTC", "SDP Offer: ${sessionDescription.description}")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        if (currentMode == WebRTCMode.TEST_AGENT) {
                            sendOfferToMillis(sessionDescription, "TestAgent")
                        } else {
                            sendOfferToMillis(sessionDescription, "CallRedirect")
                        }
                        Log.d("WebRTCManager", "Offer created and local description set")
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.e("WebRTCManager", "Failed to set local description: $p0")
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sessionDescription)
            }

            override fun onSetFailure(p0: String?) {
                Log.e("WebRTCManager", "Failed to create offer: $p0")
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {
                Log.e("WebRTCManager", "Failed to create offer: $p0")
            }
        }, MediaConstraints())
    }

    // Send the offer to MillisAI
    private fun sendOfferToMillis(offer: SessionDescription, requestType: String) {
        offerSentTime = System.currentTimeMillis()
        val request = OfferRequest(
            agent_id = agentId,
            offer = Offer(
                sdp = offer.description,
                type = offer.type.canonicalForm()
            )
        )
        val authToken = "$privateKey"
        Log.d("WebRTCManager", "Sending offer for $requestType with authToken: $authToken")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = millisApiService.sendOffer(authToken, request)
                Log.d("WebRTCManager", "Sending Request")

                if (response.isSuccessful) {
                    Log.d("WebRTCManager", "Checking Response")
                    val answerResponse = response.body()?.answer
                    if (answerResponse != null) {
                        latencyList.add(System.currentTimeMillis() - offerSentTime)
                        val answerSDP = answerResponse.sdp
                        handleAnswer(SessionDescription(SessionDescription.Type.ANSWER, answerSDP))
                        Log.d("WebRTCManager", "Offer sent and answer received successfully")
                    } else {
                        Log.e("WebRTCManager", "Response body or answer is null")
                    }
                } else {
                    Log.e("WebRTCManager", "Failed to send offer: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("WebRTCManager", "Failed to send offer: ${e.message}", e)
            }
        }
    }

    // Handle the answer from MillisAI
    private fun handleAnswer(answer: SessionDescription) {
        Log.d("WebRTCManager", "Handling answer...")
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d("WebRTCManager", "Remote description set successfully")
            }
            override fun onSetFailure(p0: String?) {
                Log.e("WebRTCManager", "Failed to set remote description: $p0")
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, answer)
    }

    // Handle ICE candidates
    fun handleIceCandidate(candidate: IceCandidatePayload) {
        Log.d("WebRTCManager", "Handling ICE candidate...")
        peerConnection?.addIceCandidate(IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate))
    }
    // Clean up resources
    fun close() {
        Log.d("WebRTCManager", "Closing WebRTC manager...")
        stopAudioCapture()
        peerConnection?.close()
        peerConnection = null
        Log.d("WebRTCManager", "WebRTC manager closed")
    }
}