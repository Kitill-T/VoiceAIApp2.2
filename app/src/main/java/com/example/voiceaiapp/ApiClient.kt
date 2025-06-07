package com.example.voiceaiapp

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val AUDIO_TYPE = "audio/wav".toMediaTypeOrNull()
    private const val BASE_URL = "https://scaling-spork-pjgjxq95vg6c7rvp-8000.app.github.dev" // Укажите ваш адрес сервера

    fun uploadAudio(
        file: File,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", // Имя параметра, которое ожидает сервер
                file.name,
                file.asRequestBody(AUDIO_TYPE)
            )
            .build()

        val request = Request.Builder()
            .url("https://scaling-spork-pjgjxq95vg6c7rvp-8000.app.github.dev/api/process_audio") // Укажите ваш endpoint
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Ошибка сети: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("Ошибка сервера: ${response.code}")
                    return
                }
                try {
                    val json = response.body?.string() ?: return
                    val transcription = json.substringAfter("\"transcription\":\"").substringBefore("\"")
                    val aiResponse = json.substringAfter("\"ai_response\":\"").substringBefore("\"")
                    onSuccess(transcription, aiResponse)
                } catch (e: Exception) {
                    onError("Ошибка обработки ответа")
                }
            }
        })
    }

    fun checkServerConnection(callback: (Boolean) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/health")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }
}