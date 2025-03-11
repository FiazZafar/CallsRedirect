package com.sahiwal.callsredirect.dataclasses

// API Response Data Models
data class ApiResponse(
    val items: List<CallLogResponse>?,
    val pagination: Pagination?
)
