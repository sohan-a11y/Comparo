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
import com.sohan.comparo.core.ShadowComparisonResult
import com.sohan.comparo.parser.BankOffer
import com.sohan.comparo.parser.PlatformParser
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
    onPermissionGrant: (PermissionType) -> Unit,
    isOverlayGranted: Boolean,
    isAccessibilityGranted: Boolean,
    onStartService: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.SETUP) }
    
    // Auto-advance if permissions granted
    LaunchedEffect(isOverlayGranted, isAccessibilityGranted) {
        if (isOverlayGranted && isAccessibilityGranted) {
            // We can perhaps move to HOME or just stay on Setup with a "Start" button
        }
    }
    
    ComparoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                Screen.SETUP -> SetupScreen(
                    onPermissionGrant = onPermissionGrant,
                    onContinue = {
                        onStartService()
                        currentScreen = Screen.HOME
                    },
                    isOverlayGranted = isOverlayGranted,
                    isAccessibilityGranted = isAccessibilityGranted
                )
                Screen.HOME -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Service is running! Open Swiggy/Zepto/Blinkit to compare.")
                }
            }
        }
    }
}

@Composable
fun LoginWebViewContainer(
    webView: android.webkit.WebView?, // The actual detached WebView
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .zIndex(10f)
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
                    text = "Login / Solve CAPTCHA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Button(onClick = onDone) {
                    Text("Done")
                }
            }
            
            // Container for the detached WebView
            if (webView != null) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = {
                        webView
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Fallback / Loading
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onPermissionGrant: (PermissionType) -> Unit,
    onContinue: () -> Unit,
    isOverlayGranted: Boolean,
    isAccessibilityGranted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Comparo Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        PermissionCard(
            title = "1. Enable Overlay",
            description = "Required to show price comparison on top of other apps.",
            isGranted = isOverlayGranted,
            onClick = { onPermissionGrant(PermissionType.OVERLAY) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionCard(
            title = "2. Enable Accessibility",
            description = "Required to read prices from Swiggy, Zepto, and Blinkit.",
            isGranted = isAccessibilityGranted,
            onClick = { onPermissionGrant(PermissionType.ACCESSIBILITY) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            enabled = isOverlayGranted && isAccessibilityGranted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Auto-Comparison")
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isGranted) {
                Text(
                    text = "✅",
                    fontSize = 24.sp
                )
            } else {
                Button(onClick = onClick) {
                    Text("Enable")
                }
            }
        }
    }
}

enum class PermissionType {
    OVERLAY, ACCESSIBILITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearch: (String) -> Unit,
    searchResults: List<ProductInfo>,
    isSearching: Boolean,
    searchError: String? = null,
    cartItems: List<PlatformParser.CartItem>,
    bankOffers: List<BankOffer>,
    shadowResults: List<ShadowComparisonResult>,
    isShadowSearching: Boolean
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
        
        // --- Bank Offers Section ---
        if (bankOffers.isNotEmpty()) {
            Text(
                text = "Detected Offers",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.foundation.lazy.LazyRow {
                items(bankOffers) { offer ->
                    Card(
                        modifier = Modifier
                            .padding(end = 8.dp, bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(offer.bankName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(offer.discountDescription ?: "Offer", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- Cart Comparison Section ---
        if (cartItems.isNotEmpty()) {
             Text(
                text = "Live Cart Comparison (${cartItems[0].platform})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (isShadowSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Checking other apps...", style = MaterialTheme.typography.bodySmall)
            }
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(shadowResults) { result ->
                     ShadowResultCard(result)
                     Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
        } else {
            // Normal Search Results
            if (searchResults.isNotEmpty()) {
                Text(
                    text = "Comparison Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val groupedProducts = groupProductsByName(searchResults)
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(groupedProducts) { group ->
                        ProductComparisonGroup(products = group)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else if (searchError != null && !isSearching) {
                // Error State
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 48.sp)
                        Text(searchError, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else if (!isSearching) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add items to your Swiggy/Zepto cart to compare automatically!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ShadowResultCard(result: ShadowComparisonResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isCheaper) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(result.originalItem.name, fontWeight = FontWeight.Bold)
                if (result.isCheaper) {
                    Text("Save ₹${String.format("%.0f", result.savingsAmount)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(result.originalItem.platform, style = MaterialTheme.typography.bodySmall)
                    Text("₹${result.originalItem.unitPrice}", fontWeight = FontWeight.Medium)
                }
                
                if (result.bestAlternative != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(result.bestAlternative.platform, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("₹${result.bestAlternative.price}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                } else {
                     Text("No better deal", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
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
