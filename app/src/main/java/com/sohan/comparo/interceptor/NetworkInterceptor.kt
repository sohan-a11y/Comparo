package com.sohan.comparo.interceptor

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NetworkInterceptor(
    private val onApiResponse: (String, String) -> Unit  // (platform, jsonResponse)
) : WebViewClient() {
    
    companion object {
        private const val TAG = "NetworkInterceptor"
        
        // Share OkHttpClient across all instances
        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS) // Increased timeout
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        
        // Detect API patterns for each platform
        val platform = when {
            isSwiggyApi(url) -> "Swiggy"
            isZeptoApi(url) -> "Zepto"
            isBlinkitApi(url) -> "Blinkit"
            else -> null
        }
        
        if (platform != null) {
            Log.d(TAG, "MATCHED $platform API: $url")
            if (view != null) {
                // Clone and execute request in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        cloneAndExecuteRequest(view, request, platform)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cloning request for $platform: ${e.message}", e)
                    }
                }
            }
        } else {
             // Optional: Log ignored URLs to help debug if we are missing something
             // Log.v(TAG, "Ignored: $url")
        }
        
        // Return null to let WebView handle the original request
        return null
    }
    
    private fun isSwiggyApi(url: String): Boolean {
        return url.contains("api/instamart/search") ||
                (url.contains("api/v1/search") && url.contains("swiggy.com")) ||
                url.contains("swiggy.com/api/instamart")
    }
    
    private fun isZeptoApi(url: String): Boolean {
        // Zepto often uses /api/v1/search or /api/v2/search
        return (url.contains("api/v1/search") || url.contains("api/v2/search")) && url.contains("zepto")
    }
    
    private fun isBlinkitApi(url: String): Boolean {
        return (url.contains("api/v1/search") || url.contains("v2/search")) && 
               (url.contains("blinkit.com") || url.contains("grofers.com"))
    }
    
    private fun cloneAndExecuteRequest(view: WebView, originalRequest: WebResourceRequest, platform: String) {
        try {
            val url = originalRequest.url.toString()
            
            // Get cookies from WebView
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookies = cookieManager.getCookie(url) ?: ""
            
            // Get User-Agent from WebView settings
            val userAgent = view.settings.userAgentString
            
            // Build OkHttp request with headers
            val requestBuilder = Request.Builder()
                .url(url)
                .method(originalRequest.method, null)
                .addHeader("User-Agent", userAgent)
            
            if (cookies.isNotEmpty()) {
                requestBuilder.addHeader("Cookie", cookies)
                Log.d(TAG, "Using Cookies for $platform: ${cookies.take(50)}...")
            } else {
                Log.w(TAG, "No Cookies found for $platform. Request might fail.")
            }
            
            // Copy other headers from original request
            originalRequest.requestHeaders?.forEach { (key, value) ->
                val lowerKey = key.lowercase()
                if (lowerKey != "cookie" && lowerKey != "user-agent") {
                    requestBuilder.addHeader(key, value)
                }
            }
            
            // Execute request
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            
            if (response.isSuccessful) {
                val jsonResponse = response.body?.string()
                if (!jsonResponse.isNullOrEmpty()) {
                    Log.d(TAG, "Success ($platform): Got ${jsonResponse.length} chars response")
                    // Broadcast JSON to MainActivity
                    onApiResponse(platform, jsonResponse)
                } else {
                    Log.w(TAG, "Empty response body from $platform")
                }
            } else {
                Log.e(TAG, "Failed ($platform): Code ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing cloned request for $platform: ${e.message}", e)
        }
    }
}
