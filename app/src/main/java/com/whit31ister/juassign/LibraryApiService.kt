package com.whit31ister.juassign

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface LibraryApiService {
    @GET("assignments/manifest.json")
    suspend fun getManifest(): Manifest

    companion object {
        private const val BASE_URL = "https://whit31ister.github.io/JU_ASSIGN/"

        fun create(): LibraryApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LibraryApiService::class.java)
        }
    }
}
