package com.fiver.clientapp.helpers

import com.sahiwal.callsredirect.interfaces.MillisAIApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {

    // Default URL, can be overridden
    private var baseUrl: String = "https://api-west.millis.ai/"

    // This function sets the base URL dynamically
    fun setBaseUrl(url: String) {
        baseUrl = url
    }

    // Retrofit instance with the dynamic base URL
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl) // Using the dynamic base URL
            .addConverterFactory(GsonConverterFactory.create()) // Gson for parsing
            .build()
    }

    val millisApiService: MillisAIApi by lazy {
        retrofit.create(MillisAIApi::class.java)
    }
}
