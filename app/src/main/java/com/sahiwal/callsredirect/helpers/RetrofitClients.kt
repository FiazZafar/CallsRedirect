package com.fiver.clientapp.helpers

import com.fiver.clientapp.interfaces.MillisSipApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClients {

    private const val BASE_URL = "https://api-west.millis.ai/"

    val instance: MillisSipApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MillisSipApi::class.java)
    }
}