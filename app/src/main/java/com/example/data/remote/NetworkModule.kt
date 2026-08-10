package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
    // Using the JioSaavn API at jiosaavn-api-2s17.onrender.com — no API key required!
    val jioSaavnBaseUrl = "https://jiosaavn-api-2s17.onrender.com/"

    // API keys — replace these with real keys when available
    var musixmatchApiKey = "PLACEHOLDER_MUSIXMATCH_KEY"
    var lastFmApiKey = "f7abdef240746476745f392862da5419"
    var lastFmSharedSecret = "4a07e3dba9910c827b06db816d77f488"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val jioSaavnApiService: JioSaavnApiService by lazy {
        Retrofit.Builder()
            .baseUrl(jioSaavnBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JioSaavnApiService::class.java)
    }

    val musixmatchApiService: MusixmatchApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.musixmatch.com/ws/1.1/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MusixmatchApiService::class.java)
    }

    val lastFmApiService: LastFmApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://ws.audioscrobbler.com/2.0/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LastFmApiService::class.java)
    }
}
