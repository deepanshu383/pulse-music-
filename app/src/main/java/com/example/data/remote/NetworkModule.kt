package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
    // We are using the free, open-source Saavn.dev API which requires NO API KEY!
    val jioSaavnBaseUrl = "https://saavn.dev/"

    // API keys — replace these with real keys when available
    var musixmatchApiKey = "PLACEHOLDER_MUSIXMATCH_KEY"
    var lastFmApiKey = "PLACEHOLDER_LASTFM_KEY"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val jioSaavnApiService: JioSaavnApiService by lazy {
        Retrofit.Builder()
            .baseUrl(jioSaavnBaseUrl)
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
