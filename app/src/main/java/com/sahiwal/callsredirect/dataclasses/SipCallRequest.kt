package com.fiver.clientapp.dataclasses

data class SipCallRequest(
    val agent_id: String? = null,
    val agent_config: Map<String, Any>? = null,
    val metadata: Map<String, Any>? = null,
    val include_metadata_in_prompt: Boolean? = null
)

