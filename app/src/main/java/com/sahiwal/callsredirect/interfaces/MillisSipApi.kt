package com.fiver.clientapp.interfaces

import com.fiver.clientapp.dataclasses.IceCandidatePayload
import com.fiver.clientapp.dataclasses.SipCallRequest
import com.fiver.clientapp.dataclasses.SipCallResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MillisSipApi {

    @POST("register_sip_call")
    suspend fun registerSipCall(
        @Header("Authorization") authToken: String,
        @Body request: SipCallRequest
    ): Response<SipCallResponse>
}