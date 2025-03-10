package com.fiver.clientapp.dataclasses

data class IceCandidatePayload(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String
)