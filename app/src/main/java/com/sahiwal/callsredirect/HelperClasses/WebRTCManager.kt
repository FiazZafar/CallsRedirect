package com.sahiwal.callredirectapp.HelperClasses

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCManager(context: Context) {
    private val peerConnectionFactory: PeerConnectionFactory

    init {
        // Initialize WebRTC
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

    fun createAudioTrack(): org.webrtc.AudioTrack? {
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        return peerConnectionFactory.createAudioTrack("audioTrack", audioSource)
    }

    fun createPeerConnection(): PeerConnection {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        return peerConnectionFactory.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                // Send the ICE candidate to MillisAI's server
                Log.d("WebRTCManager", "onIceCandidate: ${candidate?.sdp}")
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                TODO("Not yet implemented")
            }

            override fun onAddStream(mediaStream: MediaStream?) {
                // Handle the incoming stream
                Log.d("WebRTCManager", "onAddStream: ${mediaStream?.id}")
            }

            override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {
                // Handle signaling state changes
                Log.d("WebRTCManager", "onSignalingChange: $signalingState")
            }

            override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
                // Handle ICE connection state changes
                Log.d("WebRTCManager", "onIceConnectionChange: $iceConnectionState")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                // Handle ICE connection receiving changes
                Log.d("WebRTCManager", "onIceConnectionReceivingChange: $receiving")
            }

            override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {
                // Handle ICE gathering state changes
                Log.d("WebRTCManager", "onIceGatheringChange: $iceGatheringState")
            }

            override fun onRemoveStream(mediaStream: MediaStream?) {
                // Handle stream removal
                Log.d("WebRTCManager", "onRemoveStream: ${mediaStream?.id}")
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                // Handle data channel creation
                Log.d("WebRTCManager", "onDataChannel: ${dataChannel?.label()}")
            }

            override fun onRenegotiationNeeded() {
                // Handle renegotiation needed
                Log.d("WebRTCManager", "onRenegotiationNeeded")
            }

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                // Handle track addition
                Log.d("WebRTCManager", "onAddTrack: ${receiver?.id()}")
            }
        })!!
    }
}