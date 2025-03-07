package com.sahiwal.callsredirect.HelperClasses

import android.os.Handler
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class MillisWebSocketClient(
    private val webSocketUrl: String,
    private val agentId: String,
    private val publicKey: String,
    private val handler: Handler,

    private val gson: Gson,
    private val onMessageReceived: (String) -> Unit,
    private val onAudioReceived: (ByteArray) -> Unit,
    private val onConnectionClosed: () -> Unit
) {
    private lateinit var webSocket: WebSocket
    private var isWebSocketConnected = false

    fun connect() {
        val client = OkHttpClient.Builder()
            .pingInterval(0, TimeUnit.SECONDS) // Disable ping/pong
            .build()

        val request = Request.Builder()
            .url(webSocketUrl)
            .build()

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isWebSocketConnected = true
                handler.post { Log.d("MYTAG", "WebSocket connection opened") }
                sendInitiateMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handler.post { onMessageReceived(text) }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handler.post { onAudioReceived(bytes.toByteArray()) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isWebSocketConnected = false
                handler.post { Log.d("MYTAG", "WebSocket connection closing: $code, $reason") }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isWebSocketConnected = false
                handler.post { onConnectionClosed() }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isWebSocketConnected = false
                handler.post { Log.e("MYTAG", "WebSocket connection failed: ${t.message}") }
                handler.postDelayed({ connect() }, 5000) // Reconnect after 5 seconds
            }
        }

        webSocket = client.newWebSocket(request, webSocketListener)
    }

    private fun sendInitiateMessage(webSocket: WebSocket) {
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
    }

    fun sendAudioPacket(audioData: ByteArray) {
        if (isWebSocketConnected) {
            webSocket.send(ByteString.of(*audioData))
        }
    }

    fun close() {
        webSocket.close(1000, "Closing connection")
    }
}