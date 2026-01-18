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
        private const val SEARCH_TIMEOUT_MS = 5000L
    }
    
    private lateinit var swiggyWebView: WebView
    private lateinit var zeptoWebView: WebView
    private lateinit var blinkitWebView: WebView
    
    private var loginDialogWebView: WebView? = null
    
    private val platformStates = mutableStateListOf(
        PlatformLoginState("Swiggy", false),
        PlatformLoginState("Zepto", false),
        PlatformLoginState("Blinkit", false)
    )
    
    private val searchResults = mutableStateListOf<ProductInfo>()
    private var isSearching = mutableStateOf(false)
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable WebView debugging only in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        
        // Initialize cookie manager
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        
        // Create headless WebViews
        initializeHeadlessWebViews()
        
        setContent {
            val scope = rememberCoroutineScope()
            
            ComparoApp(
                onPlatformLogin = { platform ->
                    scope.launch {
                        openLoginDialog(platform)
                    }
                },
                onSearch = { query ->
                    scope.launch {
                        performSearch(query)
                    }
                },
                platformStates = platformStates.toList(),
                searchResults = searchResults.toList(),
                isSearching = isSearching.value
            )
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeHeadlessWebViews() {
        // Swiggy WebView
        swiggyWebView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(0, 0)  // Headless
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webViewClient = NetworkInterceptor { platform, jsonResponse ->
                handleApiResponse(platform, jsonResponse)
            }
        }
        
        // Zepto WebView
        zeptoWebView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(0, 0)  // Headless
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webViewClient = NetworkInterceptor { platform, jsonResponse ->
                handleApiResponse(platform, jsonResponse)
            }
        }
        
        // Blinkit WebView
        blinkitWebView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(0, 0)  // Headless
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webViewClient = NetworkInterceptor { platform, jsonResponse ->
                handleApiResponse(platform, jsonResponse)
            }
        }
        
        // Enable third-party cookies
        val cookieManager = CookieManager.getInstance()
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
            searchResults.addAll(products)
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun openLoginDialog(platform: String) {
        val url = when (platform) {
            "Swiggy" -> "https://www.swiggy.com/instamart"
            "Zepto" -> "https://www.zepto.com"
            "Blinkit" -> "https://www.blinkit.com"
            else -> return
        }
        
        // Create a dialog with WebView for login
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Login to $platform")
            .create()
        
        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
            }
        }
        
        dialog.setView(webView)
        dialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, "Done") { _, _ ->
            // Save login state
            val index = platformStates.indexOfFirst { it.name == platform }
            if (index >= 0) {
                platformStates[index] = platformStates[index].copy(isLoggedIn = true)
            }
            
            // Save cookies
            CookieManager.getInstance().flush()
        }
        dialog.setButton(android.app.AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ ->
            dialog.dismiss()
        }
        
        webView.loadUrl(url)
        dialog.show()
        
        loginDialogWebView = webView
    }
    
    private suspend fun performSearch(query: String) {
        if (query.isBlank()) return
        
        isSearching.value = true
        searchResults.clear()
        
        // Load search URLs in all three WebViews simultaneously
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        
        // Swiggy search
        swiggyWebView.loadUrl("https://www.swiggy.com/instamart/search?q=$encodedQuery")
        
        // Zepto search
        zeptoWebView.loadUrl("https://www.zepto.com/search?query=$encodedQuery")
        
        // Blinkit search
        blinkitWebView.loadUrl("https://www.blinkit.com/search?q=$encodedQuery")
        
        // Wait for responses (timeout)
        delay(SEARCH_TIMEOUT_MS)
        
        isSearching.value = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        swiggyWebView.destroy()
        zeptoWebView.destroy()
        blinkitWebView.destroy()
        loginDialogWebView?.destroy()
    }
}
