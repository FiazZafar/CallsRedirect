package com.sahiwal.callsredirect.fragments

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager

import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fiver.clientapp.dataclasses.IceCandidatePayload
import com.sahiwal.callsredirect.helpers.WebRTCManager
import com.sahiwal.callsredirect.R
import com.sahiwal.callsredirect.interfaces.MillisAIApi
import kotlinx.coroutines.launch
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import android.media.AudioFormat
import android.media.AudioRecord
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sahiwal.callsredirect.helpers.WebRTCMode
import kotlinx.coroutines.Dispatchers
class HomeFragment : Fragment() {

    private lateinit var micBtn: ImageView
    private lateinit var callDurationTxt: TextView
    private lateinit var latencyRate: TextView
    private lateinit var connected: TextView
    private lateinit var disconnected: TextView
    private lateinit var stopStartTxt: TextView

    private var webRTCManager: WebRTCManager? = null
    private lateinit var millisApiService: MillisAIApi
    private lateinit var sharedPreferences: SharedPreferences

    private var callStartTime: Long = 0
    private var isRecording = false
    private var audioRecord: AudioRecord? = null

    private val currentMode = WebRTCMode.TEST_AGENT
    val baseUrl = "https://api-west.millis.ai"

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        micBtn = view.findViewById(R.id.micBtn)
        callDurationTxt = view.findViewById(R.id.callDuration)
        latencyRate = view.findViewById(R.id.responceLatency)
        connected = view.findViewById(R.id.correctCredentialTxt)
        disconnected = view.findViewById(R.id.invalidCredentials)
        stopStartTxt = view.findViewById(R.id.stopstartTxt)

        sharedPreferences = requireActivity().getSharedPreferences("MySettings", Context.MODE_PRIVATE)

        initializeRetrofit()

        micBtn.setOnClickListener {
            if (isRecording) stopRecording() else startProcess()
        }

        return view
    }

    private fun initializeRetrofit() {
        Log.d("HomeFragment", "Initializing Retrofit...")
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        millisApiService = retrofit.create(MillisAIApi::class.java)
        Log.d("HomeFragment", "Retrofit initialized successfully")
    }

    private fun startProcess() {
        if (!hasAudioPermission()) {
            requestAudioPermission()
            return
        }

        Log.d("HomeFragment", "Starting WebRTC process...")
        if (webRTCManager == null) {
            webRTCManager = WebRTCManager(requireContext(), millisApiService, "z4oHP32NBmepHfRUaJGdOX5PQS4JTHZI", "-OKvLcAI_PHjlKHYklH3").apply {
                initializeWebRTC()
                createPeerConnection(peerConnectionObserver)
            }
        }
                webRTCManager?.createAndSendOffer(currentMode)

        callStartTime = System.currentTimeMillis()
        startCallDurationTimer()
        startRecording()
    }

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let {
                val payload = IceCandidatePayload(it.sdpMid, it.sdpMLineIndex, it.sdp)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // No need to call execute() on a suspend function
                        millisApiService.sendIceCandidate("Bearer ${sharedPreferences.getString("publicKey", "")}", payload)
                        Log.d("WebRTC", "ICE candidate sent successfully")
                    } catch (e: Exception) {
                        Log.e("WebRTC", "Failed to send ICE candidate: ${e.message}")
                    }
                }
            }
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onAddStream(stream: MediaStream?) {}
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            activity?.runOnUiThread {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        val latency = System.currentTimeMillis() - (webRTCManager?.offerSentTime ?: 0)
                        webRTCManager?.latencyList?.add(latency)

                        val avgLatency = webRTCManager?.latencyList?.average()?.toLong() ?: 0
                        latencyRate.text = "$avgLatency ms"

                        connected.visibility = View.VISIBLE
                        disconnected.visibility = View.GONE
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        connected.visibility = View.GONE
                        disconnected.visibility = View.VISIBLE
                    }
                    else -> {}
                }
            }
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onRemoveStream(stream: MediaStream?) {}
        override fun onDataChannel(dataChannel: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
    }

    private fun startCallDurationTimer() {
        Log.d("HomeFragment", "Starting call duration timer...")
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                callDurationTxt.text = "${(System.currentTimeMillis() - callStartTime) / 1000} seconds"
                if (isRecording) handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun startRecording() {
        Log.d("HomeFragment", "Starting audio recording...")
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("HomeFragment", "Microphone permission is missing!")
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

        isRecording = true
        audioRecord?.startRecording()
        stopStartTxt.text = "Call Started..."

        lifecycleScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    // WebRTC automatically handles audio streaming, so no need to manually send audio data
                }
            }
        }
    }

    private fun stopRecording() {
        Log.d("HomeFragment", "Stopping audio recording and WebRTC call...")

        isRecording = false

        // Stop and release AudioRecord
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        // Stop WebRTC connection
        webRTCManager?.close()
        webRTCManager = null  // Ensure WebRTCManager is reset

        // Reset UI elements
        stopStartTxt.text = "Click to start Call"
//        connected.visibility = View.GONE
//        disconnected.visibility = View.VISIBLE

        Log.d("HomeFragment", "Call stopped successfully.")
    }


    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startProcess()
        } else {
            Toast.makeText(requireContext(), "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("HomeFragment", "Fragment destroyed, stopping recording and closing WebRTC manager...")
        stopRecording()
        webRTCManager?.close()
    }
}