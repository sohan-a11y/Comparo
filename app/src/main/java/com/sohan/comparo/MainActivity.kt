package com.sohan.comparo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.sohan.comparo.interceptor.NetworkInterceptor
import com.sohan.comparo.parser.PlatformParser
import com.sohan.comparo.parser.ProductInfo
import com.sohan.comparo.ui.ComparoApp
import com.sohan.comparo.ui.PlatformLoginState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "Comparo"
        private const val SEARCH_TIMEOUT_MS = 6000L
    }
    
    // Headless WebViews
    private lateinit var swiggyWebView: WebView
    private lateinit var zeptoWebView: WebView
    private lateinit var blinkitWebView: WebView
    
    // Hidden container for Headless WebViews
    private lateinit var hiddenWebViewContainer: android.widget.LinearLayout
    
    // State
    private val platformStates = mutableStateListOf(
        PlatformLoginState("Swiggy", false),
        PlatformLoginState("Zepto", false),
        PlatformLoginState("Blinkit", false)
    )
    
    private val searchResults = mutableStateListOf<ProductInfo>()
    private var isSearching = mutableStateOf(false)
    private var searchError = mutableStateOf<String?>(null)
    
    // Active Login URL (null means no login screen is visible)
    private var activeLoginUrl = mutableStateOf<String?>(null)
    private var activeLoginPlatform = mutableStateOf<String?>(null)
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        
        // Initialize root layout to hold both Compose and Hidden WebViews
        val rootLayout = android.widget.FrameLayout(this)
        rootLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // 1. Create hidden container for Headless WebViews
        // Crucial: Must be visible (not GONE) but 1x1 pixel to ensure Android prioritizes execution
        hiddenWebViewContainer = android.widget.LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            orientation = android.widget.LinearLayout.VERTICAL
            visibility = android.view.View.VISIBLE 
            alpha = 0.01f // Almost invisible
        }
        rootLayout.addView(hiddenWebViewContainer)
        
        // 2. Initialize and attach WebViews
        initializeHeadlessWebViews()
        
        // 3. Set Content using ComposeView
        val composeView = androidx.compose.ui.platform.ComposeView(this).apply {
            setContent {
                val scope = rememberCoroutineScope()
                
                ComparoApp(
                    onPlatformLogin = { platform ->
                        startLogin(platform)
                    },
                    onSearch = { query ->
                        scope.launch {
                            searchError.value = null // Clear previous error
                            performSearch(query)
                        }
                    },
                    platformStates = platformStates.toList(),
                    searchResults = searchResults.toList(),
                    isSearching = isSearching.value,
                    searchError = searchError.value,
                    activeLoginUrl = activeLoginUrl.value,
                    onLoginFinished = {
                        finishLogin()
                    },
                    onLoginDismissed = {
                         activeLoginUrl.value = null
                         activeLoginPlatform.value = null
                    }
                )
            }
        }
        rootLayout.addView(composeView)
        
        setContentView(rootLayout)
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeHeadlessWebViews() {
        // Helper to create and attach a WebView
        fun createAttachedWebView(): WebView {
            val webView = WebView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    // Crucial for headless operation
                    loadsImagesAutomatically = false
                    blockNetworkImage = true
                }
            }
            hiddenWebViewContainer.addView(webView)
            return webView
        }
        
        swiggyWebView = createAttachedWebView()
        swiggyWebView.webViewClient = NetworkInterceptor { platform, jsonResponse ->
            handleApiResponse(platform, jsonResponse)
        }
        
        zeptoWebView = createAttachedWebView()
        zeptoWebView.webViewClient = NetworkInterceptor { platform, jsonResponse ->
            handleApiResponse(platform, jsonResponse)
        }
        
        blinkitWebView = createAttachedWebView()
        blinkitWebView.webViewClient = NetworkInterceptor { platform, jsonResponse ->
            handleApiResponse(platform, jsonResponse)
        }
        
        // Enable third-party cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(swiggyWebView, true)
        cookieManager.setAcceptThirdPartyCookies(zeptoWebView, true)
        cookieManager.setAcceptThirdPartyCookies(blinkitWebView, true)
    }
    
    private fun handleApiResponse(platform: String, jsonResponse: String) {
        val products = when (platform) {
            "Swiggy" -> PlatformParser.parseSwiggy(jsonResponse)
            "Zepto" -> PlatformParser.parseZepto(jsonResponse)
            "Blinkit" -> PlatformParser.parseBlinkit(jsonResponse)
            else -> emptyList()
        }
        
        runOnUiThread {
            // Add new results
            searchResults.addAll(products)
        }
    }
    
    private fun startLogin(platform: String) {
        val url = when (platform) {
            "Swiggy" -> "https://www.swiggy.com/instamart"
            "Zepto" -> "https://www.zepto.com"
            "Blinkit" -> "https://www.blinkit.com"
            else -> return
        }
        
        activeLoginPlatform.value = platform
        activeLoginUrl.value = url
    }
    
    private fun finishLogin() {
        val platform = activeLoginPlatform.value ?: return
        
        // Save login state
        val index = platformStates.indexOfFirst { it.name == platform }
        if (index >= 0) {
            platformStates[index] = platformStates[index].copy(isLoggedIn = true)
        }
        
        // Persist cookies
        CookieManager.getInstance().flush()
        
        // Close overlay
        activeLoginUrl.value = null
        activeLoginPlatform.value = null
    }
    
    private suspend fun performSearch(query: String) {
        if (query.isBlank()) return
        
        isSearching.value = true
        searchResults.clear()
        
        // Load search URLs in all three WebViews simultaneously
        val encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8.toString())
        
        Log.d(TAG, "Starting search for: $query")
        
        // Swiggy search
        swiggyWebView.loadUrl("https://www.swiggy.com/instamart/search?q=$encodedQuery")
        
        // Zepto search
        zeptoWebView.loadUrl("https://www.zepto.com/search?query=$encodedQuery")
        
        // Blinkit search
        blinkitWebView.loadUrl("https://www.blinkit.com/search?q=$encodedQuery")
        
        // Wait for responses (timeout)
        delay(SEARCH_TIMEOUT_MS)
        
        isSearching.value = false
        
        if (searchResults.isEmpty()) {
            Log.w(TAG, "Search completed with NO results.")
            searchError.value = "No products found. Please check if you are logged in to the platforms."
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup to prevent memory leaks
        hiddenWebViewContainer.removeAllViews()
        swiggyWebView.destroy()
        zeptoWebView.destroy()
        blinkitWebView.destroy()
    }
}
