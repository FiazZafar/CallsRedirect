package com.sahiwal.callsredirect.dataclasses

data class CallLogResponse(
    val session_id: String?,
    val agent_id: String?,
    val duration: Double?, // Ensure duration is Double for accurate conversion
    val ts: Double?, // Ensure timestamp is Double
    val call_status: String?
)