package com.sahiwal.callsredirect.dataclasses

data class Pagination(
    val next_start_at: Double?, // Changed to Double to handle API response properly
    val limit: Int?
)