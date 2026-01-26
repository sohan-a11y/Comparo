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
    private val onApiResponse: (String, String) -> Unit, // (platform, jsonResponse)
    private val onAuthRequired: (String) -> Unit, // (platform)
    private val onCartUpdated: (String, String) -> Unit // New: (platform, cartJson)
) : WebViewClient() {
    
    companion object {
        private const val TAG = "NetworkInterceptor"
        
        // Share OkHttpClient across all instances
        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        
        // Detect API patterns for each platform
        val (platform, type) = when {
            // Search APIs
            isSwiggySearchApi(url) -> "Swiggy" to "SEARCH"
            isZeptoSearchApi(url) -> "Zepto" to "SEARCH"
            isBlinkitSearchApi(url) -> "Blinkit" to "SEARCH"
            
            // Cart APIs
            isSwiggyCartApi(url) -> "Swiggy" to "CART"
            isZeptoCartApi(url) -> "Zepto" to "CART"
            isBlinkitCartApi(url) -> "Blinkit" to "CART"
            
            else -> null to null
        }
        
        if (platform != null && type != null) {
            Log.d(TAG, "MATCHED $platform $type API: $url")
            if (view != null) {
                // Clone and execute request in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        cloneAndExecuteRequest(view, request, platform, type)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cloning request for $platform: ${e.message}", e)
                    }
                }
            }
        }
        
        // Return null to let WebView handle the original request
        return null
    }
    
    private fun isSwiggySearchApi(url: String): Boolean {
        return url.contains("api/instamart/search") ||
                (url.contains("api/v1/search") && url.contains("swiggy.com")) ||
                url.contains("swiggy.com/api/instamart")
    }
    
    private fun isSwiggyCartApi(url: String): Boolean {
        return url.contains("instamart/cart") || url.contains("checkout/get")
    }
    
    private fun isZeptoSearchApi(url: String): Boolean {
        return (url.contains("api/v1/search") || url.contains("api/v2/search")) && url.contains("zepto")
    }

    private fun isZeptoCartApi(url: String): Boolean {
        return url.contains("api/v1/cart") || url.contains("api/v2/cart")
    }
    
    private fun isBlinkitSearchApi(url: String): Boolean {
        return (url.contains("api/v1/search") || url.contains("v2/search")) && 
               (url.contains("blinkit.com") || url.contains("grofers.com"))
    }

    private fun isBlinkitCartApi(url: String): Boolean {
        // Blinkit often uses v1/cart/detail or similar
        return url.contains("cart/detail") || url.contains("v2/cart")
    }
    
    private fun cloneAndExecuteRequest(view: WebView, originalRequest: WebResourceRequest, platform: String, type: String) {
        try {
            val url = originalRequest.url.toString()
            
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookies = cookieManager.getCookie(url) ?: ""
            val userAgent = view.settings.userAgentString
            
            val requestBuilder = Request.Builder()
                .url(url)
                .method(originalRequest.method, null)
                .addHeader("User-Agent", userAgent)
            
            if (cookies.isNotEmpty()) {
                requestBuilder.addHeader("Cookie", cookies)
            } else {
                Log.w(TAG, "No Cookies found for $platform. Request might fail.")
            }
            
            originalRequest.requestHeaders?.forEach { (key, value) ->
                val lowerKey = key.lowercase()
                if (lowerKey != "cookie" && lowerKey != "user-agent") {
                    requestBuilder.addHeader(key, value)
                }
            }
            
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            
            if (response.isSuccessful) {
                val jsonResponse = response.body?.string()
                if (!jsonResponse.isNullOrEmpty()) {
                    Log.d(TAG, "Success ($platform $type): Got ${jsonResponse.length} chars response")
                    if (type == "SEARCH") {
                        onApiResponse(platform, jsonResponse)
                    } else if (type == "CART") {
                        onCartUpdated(platform, jsonResponse)
                    }
                }
            } else {
                Log.e(TAG, "Failed ($platform): Code ${response.code}")
                // Check for Auth/CAPTCHA challenges
                if (response.code == 401 || response.code == 403) {
                    onAuthRequired(platform)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing cloned request for $platform: ${e.message}", e)
        }
    }
}
