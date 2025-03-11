package com.sahiwal.callsredirect.interfaces

import com.fiver.clientapp.dataclasses.IceCandidatePayload
import com.fiver.clientapp.fragments.OfferRequest
import com.fiver.clientapp.fragments.OfferResponse
import com.sahiwal.callsredirect.dataclasses.ApiResponse
import com.sahiwal.callsredirect.dataclasses.CallHistoryResponse
import com.sahiwal.callsredirect.dataclasses.CallLogResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MillisAIApi {

    @POST("webrtc/offer")
    suspend fun sendOffer(
        @Header("Authorization") authToken: String, // Raw API key
        @Body request: OfferRequest
    ): Response<OfferResponse>

    @POST("webrtc/ice-candidate")
    suspend fun sendIceCandidate(
        @Header("Authorization") authToken: String,
        @Body candidate: IceCandidatePayload
    ): Response<Unit>

    @GET("agents/{agentId}/call-histories")
    suspend fun getCallHistories(
        @Path("agentId") agentId: String,
        @Header("Authorization") authToken: String,
        @Query("limit") limit: Int
    ): Response<ApiResponse>

}
