package com.sahiwal.callsredirect.interfaces

import com.fiver.clientapp.dataclasses.IceCandidatePayload
import com.fiver.clientapp.fragments.OfferRequest
import com.fiver.clientapp.fragments.OfferResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MillisAIApi {

    @POST("webrtc/offer")
    fun sendOffer(
        @Header("Authorization") authToken: String,
        @Body request: OfferRequest
    ): Call<OfferResponse>

    @POST("webrtc/ice-candidate")
    fun sendIceCandidate(
        @Header("Authorization") authToken: String,
        @Body candidate: IceCandidatePayload
    ): Call<Unit>


}
