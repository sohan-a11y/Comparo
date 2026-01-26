package com.sohan.comparo.core

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.sohan.comparo.interceptor.NetworkInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HeadlessBrowserManager(
    private val context: Context,
    private val container: ViewGroup,
    private val onApiResponse: (String, String) -> Unit,
    private val onCartUpdated: (String, String) -> Unit, // New
    private val onAuthRequired: (String) -> Unit
) {

    companion object {
        private const val TAG = "HeadlessManager"
        private const val SEARCH_TIMEOUT_MS = 8000L
    }

    private val webViews = mutableMapOf<String, WebView>()
    private val platforms = listOf("Swiggy", "Zepto", "Blinkit")

    init {
        initializeWebViews()
    }

    private fun initializeWebViews() {
        platforms.forEach { platform ->
            val webView = createWebView(platform)
            webViews[platform] = webView
        }
    }

    private fun createWebView(platform: String): WebView {
        val webView = WebView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                // Initially block images to save data, unblock if resurfaced
                loadsImagesAutomatically = false
                blockNetworkImage = true
                userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }
        }

        // Attach Interceptor
        webView.webViewClient = NetworkInterceptor(
            onApiResponse = { p, json -> onApiResponse(p, json) },
            onAuthRequired = { p -> 
                Log.d(TAG, "Auth/CAPTCHA required for $p. Resurfacing...")
                onAuthRequired(p) 
            },
            onCartUpdated = { p, json -> onCartUpdated(p, json) }
        )

        // Enable Third-Party Cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        container.addView(webView)
        
        // Hide by default (but keep attached)
        webView.visibility = android.view.View.INVISIBLE
        // Set layout params to 1x1 pixel to effectively hide it but keep it active
        webView.layoutParams = LinearLayout.LayoutParams(1, 1)

        return webView
    }
    
    fun getWebViewForUi(platform: String): WebView? {
        val webView = webViews[platform] ?: return null
        // Detach from hidden container
        container.removeView(webView)
        // Reset layout params for UI usage
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.visibility = android.view.View.VISIBLE
        
        // Enable images for interaction
        webView.settings.loadsImagesAutomatically = true
        webView.settings.blockNetworkImage = false
        
        return webView
    }
    
    fun listenToWebView(platform: String) {
        val webView = webViews[platform] ?: return
        // Re-attach to hidden container
        if (webView.parent != null) {
            (webView.parent as ViewGroup).removeView(webView)
        }
        container.addView(webView)
        
        // Hide again
        webView.visibility = android.view.View.INVISIBLE
        webView.layoutParams = LinearLayout.LayoutParams(1, 1)
        
        // Disable images
        webView.settings.loadsImagesAutomatically = false
        webView.settings.blockNetworkImage = true
    }

    suspend fun performSearch(query: String) = withContext(Dispatchers.Main) {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF_8")
        Log.d(TAG, "Starting search for: $query")

        webViews.forEach { (platform, webView) ->
            val url = getSearchUrl(platform, encodedQuery)
            webView.loadUrl(url)
        }
    }
    
    suspend fun searchPlatform(platform: String, query: String) = withContext(Dispatchers.Main) {
        val webView = webViews[platform] ?: return@withContext
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF_8")
        val url = getSearchUrl(platform, encodedQuery)
        Log.d(TAG, "Targeted Shadow Search ($platform): $query")
        webView.loadUrl(url)
    }
    
    fun resurfaceWebView(platform: String) {
        val webView = webViews[platform] ?: return
        
        // Make it visible and full size
        webView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.visibility = android.view.View.VISIBLE
        
        // Enable images for human usage
        webView.settings.loadsImagesAutomatically = true
        webView.settings.blockNetworkImage = false
        
        // Bring container to front if needed (handled by parent usually)
    }

    fun hideWebView(platform: String) {
        val webView = webViews[platform] ?: return
        
        // Hide it again
        webView.layoutParams = LinearLayout.LayoutParams(1, 1)
        webView.visibility = android.view.View.INVISIBLE // or GONE if we are careful
        
        // Re-disable images
        webView.settings.loadsImagesAutomatically = false
        webView.settings.blockNetworkImage = true
    }

    fun loadLoginPage(platform: String) {
        val webView = webViews[platform] ?: return
        val url = getLoginUrl(platform)
        if (url.isNotEmpty()) {
            Log.d(TAG, "Loading login page for $platform: $url")
            webView.loadUrl(url)
        }
    }

    private fun getSearchUrl(platform: String, query: String): String {
        return when (platform) {
            "Swiggy" -> "https://www.swiggy.com/instamart/search?q=$query"
            "Zepto" -> "https://www.zepto.com/search?query=$query"
            "Blinkit" -> "https://blinkit.com/search?q=$query"
            else -> ""
        }
    }

    private fun getLoginUrl(platform: String): String {
        return when (platform) {
            "Swiggy" -> "https://www.swiggy.com/login" // Swiggy specific login page
            "Zepto" -> "https://www.zepto.com"         // Zepto usually has login on home/sidebar
            "Blinkit" -> "https://blinkit.com/login"   // Blinkit specific login page
            else -> ""
        }
    }
    
    fun destroy() {
        webViews.values.forEach { it.destroy() }
        container.removeAllViews()
    }
}
