package com.sohan.comparo.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GroqService {

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun getChatCompletion(@Body request: ChatRequest): ChatResponse
}

data class ChatRequest(
    val messages: List<Message>,
    val model: String = "llama3-8b-8192", // Faster and lightweight
    val temperature: Double = 0.0
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
