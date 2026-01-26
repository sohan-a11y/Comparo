package com.sohan.comparo

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import com.sohan.comparo.core.HeadlessBrowserManager
import com.sohan.comparo.core.ShadowComparisonResult
import com.sohan.comparo.core.ShadowSearchEngine
import com.sohan.comparo.parser.BankOffer
import com.sohan.comparo.parser.OfferParser
import com.sohan.comparo.parser.PlatformParser
import com.sohan.comparo.parser.ProductInfo
import com.sohan.comparo.ui.ComparoApp
import com.sohan.comparo.ui.PlatformLoginState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ComparoMain"
    }

    private lateinit var headlessManager: HeadlessBrowserManager
    private lateinit var hiddenContainer: LinearLayout

    // State
    private val platformStates = mutableStateListOf(
        PlatformLoginState("Swiggy", false),
        PlatformLoginState("Zepto", false),
        PlatformLoginState("Blinkit", false)
    )

    private val searchResults = mutableStateListOf<ProductInfo>()
    private var isSearching = mutableStateOf(false)
    private var searchError = mutableStateOf<String?>(null)
    private var authRequiredPlatform = mutableStateOf<String?>(null)

    // Active Login State
    private var activeLoginPlatform = mutableStateOf<String?>(null)
    private var activeWebView = mutableStateOf<WebView?>(null)
    
    // Shadow Search & Offers State
    private val cartItems = mutableStateListOf<PlatformParser.CartItem>()
    private val bankOffers = mutableStateListOf<BankOffer>()
    private val shadowResults = mutableStateListOf<ShadowComparisonResult>()
    private var isShadowSearching = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        val rootLayout = FrameLayout(this)
        rootLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // 1. Create hidden container
        hiddenContainer = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            orientation = LinearLayout.VERTICAL
            visibility = android.view.View.VISIBLE
        }
        rootLayout.addView(hiddenContainer)

        // 2. Initialize Manager
        headlessManager = HeadlessBrowserManager(
            context = this,
            container = hiddenContainer,
            onApiResponse = { platform, json ->
                handleApiResponse(platform, json)
            },
            onCartUpdated = { platform, json ->
                handleCartUpdated(platform, json)
            },
            onAuthRequired = { platform ->
                handleAuthRequired(platform)
            }
        )

        // 3. Set Content
        val composeView = ComposeView(this).apply {
            setContent {
                val scope = rememberCoroutineScope()
                
                // Show Auth Dialog if needed
                if (authRequiredPlatform.value != null) {
                    val platform = authRequiredPlatform.value!!
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { authRequiredPlatform.value = null },
                        title = { androidx.compose.material3.Text("Verification Required") },
                        text = { androidx.compose.material3.Text("Comparo needs to verify you on $platform. Please resolve the CAPTCHA or Login.") },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    authRequiredPlatform.value = null
                                    startLogin(platform)
                                }
                            ) {
                                androidx.compose.material3.Text("Solve Now")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { authRequiredPlatform.value = null }) {
                                androidx.compose.material3.Text("Cancel")
                            }
                        }
                    )
                }

                ComparoApp(
                    onPlatformLogin = { platform ->
                        startLogin(platform)
                        headlessManager.resurfaceWebView(platform)
                    },
                    onSearch = { query ->
                        scope.launch {
                            searchError.value = null
                            isSearching.value = true
                            searchResults.clear()
                            
                            try {
                                headlessManager.performSearch(query)
                                kotlinx.coroutines.delay(6000) // Simple wait
                            } finally {
                                isSearching.value = false
                                if (searchResults.isEmpty()) {
                                    searchError.value = "No results found. Check connections or logins."
                                }
                            }
                        }
                    },
                    platformStates = platformStates,
                    searchResults = searchResults,
                    isSearching = isSearching.value,
                    searchError = searchError.value,
                    activeLoginUrl = null,
                    activeWebView = activeWebView.value,
                    cartItems = cartItems,
                    bankOffers = bankOffers,
                    shadowResults = shadowResults,
                    isShadowSearching = isShadowSearching.value,
                    onLoginFinished = {
                        finishLogin()
                    },
                    onLoginDismissed = {
                        dismissLogin()
                    }
                )
            }
        }
        rootLayout.addView(composeView)
        setContentView(rootLayout)
    }

    private fun handleApiResponse(platform: String, jsonResponse: String) {
        val products = when (platform) {
            "Swiggy" -> PlatformParser.parseSwiggy(jsonResponse)
            "Zepto" -> PlatformParser.parseZepto(jsonResponse)
            "Blinkit" -> PlatformParser.parseBlinkit(jsonResponse)
            else -> emptyList()
        }

        runOnUiThread {
            if (isShadowSearching.value) {
                // Check if this result matches any cart item
                products.forEach { product ->
                    // Find cart items that vaguely match this product
                    val cartItem = cartItems.find { it.name.contains(product.name, ignoreCase = true) || product.name.contains(it.name, ignoreCase = true) }
                    
                    if (cartItem != null && cartItem.platform != platform) {
                        // Compare!
                        val comparison = ShadowSearchEngine.findBestMatch(cartItem, listOf(product))
                        // Add or Update result
                         val existingIdx = shadowResults.indexOfFirst { it.originalItem.name == cartItem.name }
                         if (existingIdx >= 0) {
                             // Update if better
                             val existing = shadowResults[existingIdx]
                             if (comparison.isCheaper && (!existing.isCheaper || comparison.savingsAmount > existing.savingsAmount)) {
                                 shadowResults[existingIdx] = comparison
                             }
                         } else {
                             shadowResults.add(comparison)
                         }
                    }
                }
            } else {
                searchResults.addAll(products)
            }
        }
    }
    
    private fun handleCartUpdated(platform: String, jsonResponse: String) {
        val newItems = when (platform) {
            "Swiggy" -> PlatformParser.parseSwiggyCart(jsonResponse)
            "Zepto" -> PlatformParser.parseZeptoCart(jsonResponse)
            "Blinkit" -> PlatformParser.parseBlinkitCart(jsonResponse)
            else -> emptyList()
        }
        
        // Parse Offers based on heuristic text extraction
        val offers = OfferParser.parseOffers(extractStringsFromJson(jsonResponse))

        runOnUiThread {
            // Update Cart Items
            cartItems.clear()
            cartItems.addAll(newItems)
            
            // Update Offers
            bankOffers.clear()
            bankOffers.addAll(offers)
            
            if (cartItems.isNotEmpty()) {
                triggerShadowSearch(cartItems, platform)
            }
        }
    }
    
    private fun extractStringsFromJson(json: String): List<String> {
        val strings = mutableListOf<String>()
        val regex = Regex("\"([^\"]*)\"")
        regex.findAll(json).forEach { 
            val s = it.groupValues[1]
            if (s.length > 10 && (s.contains("off", true) || s.contains("cashback", true))) {
                strings.add(s)
            }
        }
        return strings
    }
    
    private fun triggerShadowSearch(items: List<PlatformParser.CartItem>, sourcePlatform: String) {
        isShadowSearching.value = true
        shadowResults.clear()
        
        // Launch in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Determine competitors
                val competitors = listOf("Swiggy", "Zepto", "Blinkit").filter { it != sourcePlatform }
                
                items.take(5).forEach { item -> // Limit to 5 items to prevent spam
                     competitors.forEach { competitor ->
                         headlessManager.searchPlatform(competitor, item.name)
                         // Wait a bit between requests
                         kotlinx.coroutines.delay(1000)
                     }
                }
                
                kotlinx.coroutines.delay(5000) // Wait for results to settle
            } finally {
                runOnUiThread {
                    isShadowSearching.value = false
                }
            }
        }
    }
    
    private fun handleAuthRequired(platform: String) {
        runOnUiThread {
            if (activeLoginPlatform.value != platform) {
                authRequiredPlatform.value = platform
            }
        }
    }

    private fun startLogin(platform: String) {
        activeLoginPlatform.value = platform
        // Load the login page specifically
        headlessManager.loadLoginPage(platform)
        // Detach WebView from manager and put into state
        activeWebView.value = headlessManager.getWebViewForUi(platform)
    }
    
    private fun dismissLogin() {
        val platform = activeLoginPlatform.value ?: return
        headlessManager.listenToWebView(platform) // Return ownership
        activeLoginPlatform.value = null
        activeWebView.value = null
    }

    private fun finishLogin() {
        val platform = activeLoginPlatform.value ?: return
        
        dismissLogin()

        // Save login state
        val index = platformStates.indexOfFirst { it.name == platform }
        if (index >= 0) {
            platformStates[index] = platformStates[index].copy(isLoggedIn = true)
        }

        android.webkit.CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        super.onDestroy()
        headlessManager.destroy()
    }
}
