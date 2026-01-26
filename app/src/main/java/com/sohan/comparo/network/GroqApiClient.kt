package com.sohan.comparo.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GroqApiClient {
    private const val BASE_URL = "https://api.groq.com/openai/v1/"
    private var retrofit: Retrofit? = null
    var apiKey: String = ""

    fun getService(): GroqService? {
        if (apiKey.isEmpty()) return null
        
        if (retrofit == null) {
            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            }
            
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        
        return retrofit!!.create(GroqService::class.java)
    }
    
    fun resetClient() {
        retrofit = null // Force recreate if key changes
    }
}
