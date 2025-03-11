package com.sahiwal.callsredirect.dataclasses

data class CallHistoryResponse(
    val items: List<CallLogResponse>?,  // Ensure items exist
    val pagination: Pagination?         // Include pagination if needed
)




