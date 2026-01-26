package com.sohan.comparo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.view.ViewGroup
import com.sohan.comparo.parser.ProductInfo

enum class Screen {
    SETUP, HOME
}

data class PlatformLoginState(
    val name: String,
    val isLoggedIn: Boolean = false
)

@Composable
fun ComparoApp(
    onPlatformLogin: (String) -> Unit,
    onSearch: (String) -> Unit,
    platformStates: List<PlatformLoginState>,
    searchResults: List<ProductInfo>,
    isSearching: Boolean,
    searchError: String? = null,
    activeLoginUrl: String? = null,
    onLoginFinished: () -> Unit = {},
    onLoginDismissed: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(Screen.SETUP) }
    val allLoggedIn = platformStates.all { it.isLoggedIn }
    
    // Switch to home screen when all platforms are logged in
    LaunchedEffect(allLoggedIn) {
        if (allLoggedIn && currentScreen == Screen.SETUP) {
            currentScreen = Screen.HOME
        }
    }
    
    // Handle Back Press when Login is active
    androidx.activity.compose.BackHandler(enabled = activeLoginUrl != null) {
        onLoginDismissed()
    }
    
    ComparoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content
                when (currentScreen) {
                    Screen.SETUP -> SetupScreen(
                        platformStates = platformStates,
                        onPlatformLogin = onPlatformLogin,
                        onContinue = { currentScreen = Screen.HOME },
                        canContinue = allLoggedIn
                    )
                    Screen.HOME -> HomeScreen(
                        onSearch = onSearch,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        searchError = searchError
                    )
                }
                
                // Login Overlay
                if (activeLoginUrl != null) {
                    LoginWebViewContainer(
                        url = activeLoginUrl,
                        onDone = onLoginFinished,
                        onDismiss = onLoginDismissed
                    )
                }
            }
        }
    }
}

@Composable
fun LoginWebViewContainer(
    url: String,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .zIndex(10f) // Ensure it's on top
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Button(onClick = onDone) {
                    Text("Done")
                }
            }
            
            // WebView
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                        }
                        webViewClient = android.webkit.WebViewClient()
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    // Avoid reloading on recomposition if URL hasn't changed effectively
                    if (webView.url != url && webView.originalUrl != url) {
                        webView.loadUrl(url)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    platformStates: List<PlatformLoginState>,
    onPlatformLogin: (String) -> Unit,
    onContinue: () -> Unit,
    canContinue: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Comparo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "Please login to all platforms to continue",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Platform login list
        platformStates.forEach { platform ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = platform.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (platform.isLoggedIn) {
                        Text(
                            text = "✓",
                            fontSize = 24.sp,
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Button(onClick = { onPlatformLogin(platform.name) }) {
                            Text("Login")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue to Search")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearch: (String) -> Unit,
    searchResults: List<ProductInfo>,
    isSearching: Boolean,
    searchError: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Comparo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search for products") },
            placeholder = { Text("e.g., Milk, Eggs, Bread") },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { onSearch(searchQuery) },
            modifier = Modifier.fillMaxWidth(),
            enabled = searchQuery.isNotBlank() && !isSearching
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isSearching) "Searching..." else "Compare Prices")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Results
        if (searchResults.isNotEmpty()) {
            Text(
                text = "Comparison Results",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val groupedProducts = groupProductsByName(searchResults)
            
            LazyColumn {
                items(groupedProducts) { group ->
                    ProductComparisonGroup(products = group)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else if (searchError != null && !isSearching) {
            // Error State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = searchError,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (!isSearching) {
            // Empty / Initial State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Search for products to compare prices",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProductComparisonGroup(products: List<ProductInfo>) {
    if (products.isEmpty()) return
    
    val cheapest = products.minByOrNull { it.price }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = products.first().name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            products.forEach { product ->
                ComparisonCard(
                    product = product,
                    isCheapest = product == cheapest
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun ComparisonCard(product: ProductInfo, isCheapest: Boolean) {
    val backgroundColor = if (isCheapest) {
        Color(0xFFE8F5E9)  // Light green
    } else {
        Color.Transparent
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.platform,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            if (product.etaMinutes != null) {
                Text(
                    text = "${product.etaMinutes} mins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCheapest) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
                if (isCheapest) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "✓",
                        fontSize = 18.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            if (product.originalPrice != null && product.originalPrice > product.price) {
                Text(
                    text = "₹${String.format("%.2f", product.originalPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
            }
        }
    }
}

fun groupProductsByName(products: List<ProductInfo>): List<List<ProductInfo>> {
    // Group products by similar names
    val groups = mutableMapOf<String, MutableList<ProductInfo>>()
    
    products.forEach { product ->
        val normalizedName = product.name.lowercase().trim()
        var foundGroup = false
        
        // Try to find existing group with similar name
        for ((groupName, group) in groups) {
            if (areSimilarNames(normalizedName, groupName)) {
                group.add(product)
                foundGroup = true
                break
            }
        }
        
        if (!foundGroup) {
            groups[normalizedName] = mutableListOf(product)
        }
    }
    
    // Sort groups by cheapest price
    return groups.values
        .map { it.sortedBy { product -> product.price } }
        .sortedBy { it.first().price }
}

fun areSimilarNames(name1: String, name2: String): Boolean {
    // Simple similarity check - can be improved with better algorithms
    val minWordLength = 2  // Minimum word length to consider for matching
    val words1 = name1.split(" ").filter { it.length > minWordLength }
    val words2 = name2.split(" ").filter { it.length > minWordLength }
    
    if (words1.isEmpty() || words2.isEmpty()) {
        return name1 == name2
    }
    
    // Check if they share significant words
    val commonWords = words1.intersect(words2.toSet())
    return commonWords.isNotEmpty()
}
