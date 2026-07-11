package com.example.data.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Request Models ---

@JsonClass(generateAdapter = true)
data class SafetySetting(
    val category: String,
    val threshold: String
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    val thinkingBudget: Int? = null
)

@JsonClass(generateAdapter = true)
data class Tool(
    val googleSearch: GoogleSearch? = null
)

@JsonClass(generateAdapter = true)
class GoogleSearch

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null,
    val safetySettings: List<SafetySetting>? = null,
    val tools: List<Tool>? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null,
    val fileData: FileData? = null
)

@JsonClass(generateAdapter = true)
data class FileData(
    val fileUri: String,
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class UploadFileResponse(
    val file: GeminiFile
)

@JsonClass(generateAdapter = true)
data class GeminiFile(
    val name: String,
    val displayName: String?,
    val mimeType: String,
    val sizeBytes: String?,
    val uri: String,
    val state: String? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 string
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: ThinkingConfig? = null
)

// --- Response Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @retrofit2.http.GET("v1beta/{fileName}")
    suspend fun getFile(
        @Path("fileName", encoded = true) fileName: String,
        @Query("key") apiKey: String
    ): GeminiFile
}

// --- Retrofit Client ---

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun uploadFile(
        context: android.content.Context,
        uri: android.net.Uri,
        displayName: String,
        mimeType: String,
        apiKey: String
    ): GeminiFile? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val mediaTypeJson = "application/json; charset=UTF-8".toMediaTypeOrNull()
                val mediaTypeFile = mimeType.toMediaTypeOrNull()

                val metadataJson = """{"file": {"displayName": "$displayName"}}"""

                val metadataRequestBody = okhttp3.RequestBody.create(mediaTypeJson, metadataJson)
                
                // Stream the file content to prevent OutOfMemoryError
                val fileRequestBody = object : okhttp3.RequestBody() {
                    override fun contentType(): okhttp3.MediaType? = mediaTypeFile
                    
                    override fun contentLength(): Long {
                        return try {
                            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { 
                                it.length 
                            } ?: -1L
                        } catch (e: Exception) {
                            -1L
                        }
                    }

                    override fun writeTo(sink: okio.BufferedSink) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val buffer = ByteArray(65536) // 64KB chunks
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                sink.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                }

                val boundary = "Boundary_${System.currentTimeMillis()}"
                val requestBody = okhttp3.MultipartBody.Builder(boundary)
                    .setType("multipart/related".toMediaTypeOrNull()!!)
                    .addPart(metadataRequestBody)
                    .addPart(fileRequestBody)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url("${BASE_URL}upload/v1beta/files?key=$apiKey")
                    .header("X-Goog-Upload-Protocol", "multipart")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val adapter = moshi.adapter(UploadFileResponse::class.java)
                    adapter.fromJson(bodyString)?.file
                } else {
                    android.util.Log.e("GeminiClient", "Upload failed with status code ${response.code}: ${response.message}")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
