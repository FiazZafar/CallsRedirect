package com.sahiwal.callsredirect.fragments


import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.sahiwal.callsredirect.HelperClasses.MillisWebSocketClient
import com.sahiwal.callsredirect.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class HomeFragment : Fragment() {

    private lateinit var agentStatusText: TextView
    private lateinit var micBtn: ImageView
    private lateinit var webSocketClient: MillisWebSocketClient
    private val handler = Handler(Looper.getMainLooper())
    private val gson = Gson()

    // Audio configuration
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val sampleRate = 16000 // 16 kHz sample rate
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO // Mono input
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT // 16-bit PCM
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private var isRecording = false
    // Buffer to store recorded audio data
    private val audioBuffer = ByteArrayOutputStream()
    // Coroutine scope for background tasks
    private val scope = CoroutineScope(Dispatchers.IO)
    // LiveData for UI updates
    private val _agentStatus = MutableLiveData<String>()
    val agentStatus: LiveData<String> get() = _agentStatus


    companion object {
        private const val REQUEST_CODE_RECORD_AUDIO = 1
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        // Initialize views
        agentStatusText = view.findViewById(R.id.agentStatusText)
        micBtn = view.findViewById(R.id.micBtn)


        // Set click listener for micBtn
        micBtn.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // Initialize WebSocket client
        webSocketClient = MillisWebSocketClient(
            webSocketUrl = "wss://api-west.millis.ai:8080/millis",
            agentId = "-OKdqbqetEcM4ak369ym",
            publicKey = "XWJrD01tzTR2OwxaR8OQ4cvpTauDHmtl",
            handler = handler,
            gson = gson,
            onMessageReceived = { handleIncomingMessage(it) },
            onAudioReceived = { playAudioResponse(it) },
            onConnectionClosed = { updateAgentStatus("Connection closed") }
        )

        // Connect to WebSocket
        webSocketClient.connect()

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observe LiveData for UI updates
        agentStatus.observe(viewLifecycleOwner, Observer { status ->
            agentStatusText.text = status
        })
    }

    private fun updateAgentStatus(status: String) {
        _agentStatus.postValue(status)
    }

    private fun startRecording() {
        // Check for RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }

        // Initialize AudioRecord
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                // Start recording
                audioRecord?.startRecording()
                isRecording = true
                updateAgentStatus("Recording...")

                // Read audio data in a background coroutine
                scope.launch {
                    val buffer = ByteArray(bufferSize)
                    while (isRecording) {
                        val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                        if (bytesRead > 0) {
                            webSocketClient.sendAudioPacket(buffer.sliceArray(0 until bytesRead))
                        }
                    }
                }
            } else {
                Log.e("MYTAG", "AudioRecord initialization failed")
                updateAgentStatus("Recording failed: AudioRecord not initialized")
            }
        } catch (e: Exception) {
            Log.e("MYTAG", "Error initializing AudioRecord: ${e.message}")
            updateAgentStatus("Recording failed: ${e.message}")
        }
    }

    private fun stopRecording() {
        // Stop recording
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        updateAgentStatus("Recording stopped")
    }

    private fun playAudioResponse(audioData: ByteArray) {
        // Initialize AudioTrack if not already initialized
        if (audioTrack == null) {
            audioTrack = AudioTrack(
                AudioTrack.MODE_STREAM,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.play() // Start playback
        }

        // Write audio data to AudioTrack
        audioTrack?.write(audioData, 0, audioData.size)
        Log.d("MYTAG", "Playing audio response")
    }

    private fun handleIncomingMessage(message: String) {
        Log.d("MYTAG", "Received message: $message")
        val jsonMessage = gson.fromJson(message, Map::class.java)
        when (jsonMessage["method"] as String) {
            "ontranscript" -> {
                val transcript = jsonMessage["data"] as String
                updateAgentStatus("Client: $transcript")
            }
            "onresponsetext" -> {
                val responseText = jsonMessage["data"] as String
                updateAgentStatus("Agent: $responseText")
            }
            "onsessionended" -> {
                updateAgentStatus("Session ended")
                stopAudioPlayback()
                webSocketClient.close()
            }
            // Other cases...
        }
    }

    private fun stopAudioPlayback() {
        audioTrack?.stop() // Stop playback
        audioTrack?.release() // Release resources
        audioTrack = null
        Log.d("MYTAG", "Audio playback stopped")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up resources
        stopRecording()
        stopAudioPlayback()
        webSocketClient.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                } else {
                    updateAgentStatus("Permission denied: RECORD_AUDIO")
                }
            }
        }

}
}