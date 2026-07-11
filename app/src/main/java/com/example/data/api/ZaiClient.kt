package com.example.data.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// --- Request Models ---

@JsonClass(generateAdapter = true)
data class ZaiChatRequest(
    val model: String,
    val messages: List<ZaiMessage>,
    val temperature: Float? = null,
    val max_tokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class ZaiMessage(
    val role: String,
    val content: String
)

// --- Response Models ---

@JsonClass(generateAdapter = true)
data class ZaiChatResponse(
    val choices: List<ZaiChoice>? = null,
    val error: ZaiError? = null
)

@JsonClass(generateAdapter = true)
data class ZaiChoice(
    val message: ZaiMessage? = null,
    val finish_reason: String? = null
)

@JsonClass(generateAdapter = true)
data class ZaiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

// --- Retrofit Interface ---

interface ZaiApiService {
    @POST("chat/completions")
    suspend fun generateChat(
        @Header("Authorization") authorization: String,
        @Body request: ZaiChatRequest
    ): ZaiChatResponse
}

// --- Retrofit Client ---

object ZaiClient {
    private const val BASE_URL = "https://api.z.ai/api/paas/v4/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val apiService: ZaiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ZaiApiService::class.java)
    }
}
