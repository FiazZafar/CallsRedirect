package com.sahiwal.callsredirect.fragments


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private lateinit var agentStatusWrongText: TextView
    private lateinit var agentStatusCorrectText: TextView
    private lateinit var micBtn: ImageView
    private lateinit var webSocketClient: MillisWebSocketClient
    private val handler = Handler(Looper.getMainLooper())
    private val gson = Gson()

    // Variables to store data from SharedPreferences
    private lateinit var agentId: String
    private lateinit var publicKey: String
    private lateinit var serverUrl: String

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
        private const val SHARED_PREFS_NAME = "MySettings"
        private const val KEY_AGENT_ID = "agentId"
        private const val KEY_PUBLIC_KEY = "publicKey"
        private const val KEY_SERVER_URL = "serverUrl"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize views
        agentStatusWrongText = view.findViewById(R.id.invalidCredentials)
        agentStatusCorrectText = view.findViewById(R.id.correctCredentialTxt)
        micBtn = view.findViewById(R.id.micBtn)

        // Hide both TextViews initially
        agentStatusWrongText.visibility = View.GONE
        agentStatusCorrectText.visibility = View.GONE

        // Fetch data from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        agentId = sharedPreferences.getString(KEY_AGENT_ID, "").toString()
        publicKey = sharedPreferences.getString(KEY_PUBLIC_KEY, "").toString()
        serverUrl = sharedPreferences.getString(KEY_SERVER_URL, "").toString()

        // Log the fetched data (optional)
        Log.d("MYTAG", "Agent ID: $agentId")
        Log.d("MYTAG", "Public Key: $publicKey")
        Log.d("MYTAG", "Server URL: $serverUrl")

        // Set click listener for micBtn
        micBtn.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                if (webSocketClient.isWebSocketConnected) {
                    // Start the conversation
                    webSocketClient.startConversation()
                    startRecording()
                } else {
                    updateConnectionUI(false, "Connection not established. Please check your credentials.")
                }
            }
        }

        // Initialize WebSocket client
        webSocketClient = MillisWebSocketClient(
            webSocketUrl = serverUrl,
            agentId = agentId,
            publicKey = publicKey,
            handler = handler,
            gson = gson,
            onMessageReceived = { handleIncomingMessage(it) },
            onAudioReceived = { playAudioResponse(it) },
            onConnectionClosed = { message ->
                updateAgentStatus(message)
                updateConnectionUI(false, message) // Update UI with the error message
            },
            onConnectionSuccess = {
                updateAgentStatus("Connection successful")
                updateConnectionUI(true)
            }
        )

        // Connect to WebSocket
        webSocketClient.connect()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observe LiveData for UI updates
        agentStatus.observe(viewLifecycleOwner, Observer { status ->
            agentStatusWrongText.text = status
        })
    }

    private fun updateAgentStatus(status: String) {
        _agentStatus.postValue(status)
    }

    private fun updateConnectionUI(isConnected: Boolean, message: String? = null) {
        if (isConnected) {
            // Connection is successful
            agentStatusCorrectText.visibility = View.VISIBLE
            agentStatusWrongText.visibility = View.GONE
        } else {
            // Connection failed
            agentStatusCorrectText.visibility = View.GONE
            agentStatusWrongText.visibility = View.VISIBLE
            if (message != null) {
                agentStatusWrongText.text = message
            }
        }
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
                        if (bytesRead > 0 && !webSocketClient.isAgentSpeaking) { // Ignore if agent is speaking
                            Log.d("MYTAG", "Sending audio data: $bytesRead bytes")
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
        Log.d("MYTAG", "Playing audio response: ${audioData.size} bytes")
    }

    private fun handleIncomingMessage(message: String) {
        Log.d("MYTAG", "Received raw message: $message")

        try {
            // Attempt to parse the message as a JSON object
            val jsonMessage = gson.fromJson(message, Map::class.java)

            // Check if the message contains the "method" key
            if (jsonMessage.containsKey("method")) {
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
                    else -> {
                        Log.w("MYTAG", "Unknown method: ${jsonMessage["method"]}")
                    }
                }
            } else {
                // Handle plain string messages
                Log.d("MYTAG", "Received plain text message: $message")
                updateAgentStatus(message) // Display the plain text message in the UI
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            // Handle plain string messages that are not JSON
            Log.d("MYTAG", "Received plain text message (not JSON): $message")
            updateAgentStatus(message) // Display the plain text message in the UI
        } catch (e: Exception) {
            // Handle other exceptions
            Log.e("MYTAG", "Error processing message: $message", e)
            updateAgentStatus("Error: Failed to process message")
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