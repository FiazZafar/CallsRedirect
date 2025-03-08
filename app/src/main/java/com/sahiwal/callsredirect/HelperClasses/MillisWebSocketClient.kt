package com.sahiwal.callsredirect.HelperClasses

import android.os.Handler
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class MillisWebSocketClient(
    val webSocketUrl: String,
    val agentId: String,
    val publicKey: String,
    val handler: Handler,
    val gson: Gson,
    val onMessageReceived: (String) -> Unit,
    val onAudioReceived: (ByteArray) -> Unit,
    val onConnectionClosed: (String) -> Unit,
    val onConnectionSuccess: () -> Unit
) {
    private lateinit var webSocket: WebSocket
    var isWebSocketConnected = false
    var isAgentSpeaking = false // Flag to track if the agent is speaking
    private var packetCounter = 0 // Packet counter for keep-alive
    // Timeout mechanism
    private val connectionTimeout = 10000L // 10 seconds
    private val timeoutRunnable = Runnable {
        handler.post {
            if (!isWebSocketConnected) {
                onConnectionClosed("Connection timed out. Please check your credentials.")
            }
        }
    }

    fun connect() {
        val client = OkHttpClient.Builder()
            .pingInterval(0, TimeUnit.SECONDS) // Set a reasonable ping interval
            .build()

        val request = Request.Builder()
            .url(webSocketUrl)
            .build()

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isWebSocketConnected = true
                handler.post {
                    Log.d("MYTAG", "WebSocket connection opened")
                    handler.removeCallbacks(timeoutRunnable) // Stop the timeout timer
                    onConnectionSuccess()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handler.post {
                    Log.d("MYTAG", "Received message: $text")
                    val jsonMessage = gson.fromJson(text, Map::class.java)
                    when (jsonMessage["method"] as String) {
                        "ontranscript" -> {
                            val transcript = jsonMessage["data"] as String
                            onMessageReceived("Client: $transcript")
                        }
                        "onresponsetext" -> {
                            val responseText = jsonMessage["data"] as String
                            onMessageReceived("Agent: $responseText")
                            isAgentSpeaking = true // Agent is speaking
                        }
                        "onsessionended" -> {
                            onMessageReceived("Session ended")
                            isAgentSpeaking = false // Agent stopped speaking
                        }
                        "onaudioend" -> { // Add this case if the server sends an "onaudioend" event
                            isAgentSpeaking = false // Agent stopped speaking
                        }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handler.post {
                    Log.d("MYTAG", "Received audio data: ${bytes.size} bytes")
                    onAudioReceived(bytes.toByteArray()) // Pass the audio data to the callback
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isWebSocketConnected = false
                handler.post {
                    Log.d("MYTAG", "WebSocket connection closing: $code, $reason")
                    onConnectionClosed("Connection closed: $reason")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isWebSocketConnected = false
                handler.post {
                    Log.d("MYTAG", "WebSocket connection closed: $code, $reason")
                    onConnectionClosed("Connection closed: $reason")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isWebSocketConnected = false
                handler.post {
                    Log.e("MYTAG", "WebSocket connection failed: ${t.message}")
                    onConnectionClosed("Connection failed: ${t.message}")
                }
                handler.postDelayed({ connect() }, 5000) // Reconnect after 5 seconds
            }
        }

        webSocket = client.newWebSocket(request, webSocketListener)

        // Start the timeout timer
        handler.postDelayed(timeoutRunnable, connectionTimeout)
    }

    // Function to start the conversation
    fun startConversation() {
        if (isWebSocketConnected) {
            val initiateMessage = mapOf(
                "method" to "initiate",
                "data" to mapOf(
                    "agent" to mapOf("agent_id" to agentId),
                    "public_key" to publicKey,
                    "metadata" to mapOf("key" to "value"),
                    "include_metadata_in_prompt" to true
                )
            )
            webSocket.send(gson.toJson(initiateMessage))
            Log.d("MYTAG", "Initiate message sent")
        } else {
            Log.e("MYTAG", "WebSocket connection not established")
        }
    }

    fun sendAudioPacket(audioData: ByteArray) {
        if (!isWebSocketConnected) {
            Log.e("MYTAG", "WebSocket is not connected")
            return
        }

        // Send audio data as a binary message
        webSocket.send(ByteString.of(*audioData))
        Log.d("MYTAG", "Audio packet sent: ${audioData.size} bytes")

        // Send a ping message every 1000 packets
        packetCounter++
        if (packetCounter % 1000 == 0) {
            val pingMessage = mapOf("method" to "ping")
            val jsonMessage = gson.toJson(pingMessage)
            webSocket.send(jsonMessage)
            Log.d("MYTAG", "Ping message sent")
        }
    }

    // Function to close the WebSocket connection
    fun close() {
        webSocket.close(1000, "Closing connection")
    }
}