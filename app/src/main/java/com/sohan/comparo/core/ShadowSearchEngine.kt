package com.sohan.comparo.core

import com.sohan.comparo.parser.PlatformParser
import com.sohan.comparo.parser.ProductInfo
import kotlin.math.abs

data class ShadowComparisonResult(
    val originalItem: PlatformParser.CartItem,
    val bestAlternative: ProductInfo?,
    val savingsAmount: Double,
    val isCheaper: Boolean
)

object ShadowSearchEngine {

    fun findBestMatch(
        cartItem: PlatformParser.CartItem,
        competitorProducts: List<ProductInfo>
    ): ShadowComparisonResult {
        
        // 1. Filter by Name Similarity (Simple "Contains" or Levenshtein could be used)
        val matches = competitorProducts.filter { product ->
            areNamesSimilar(cartItem.name, product.name)
        }
        
        if (matches.isEmpty()) {
            return ShadowComparisonResult(cartItem, null, 0.0, false)
        }
        
        // 2. Find lowest price among matches
        val bestMatch = matches.minByOrNull { it.price }
        
        if (bestMatch != null) {
            val savings = cartItem.unitPrice - bestMatch.price
            // We consider it "Cheaper" if savings are positive and significant (> ₹1)
            val isCheaper = savings > 1.0
            
            return ShadowComparisonResult(
                originalItem = cartItem,
                bestAlternative = bestMatch,
                savingsAmount = savings,
                isCheaper = isCheaper
            )
        }
        
        return ShadowComparisonResult(cartItem, null, 0.0, false)
    }

    private fun areNamesSimilar(name1: String, name2: String): Boolean {
        // Normalize: "Amul Gold Milk 500ml" -> "amul gold milk"
        // We strip non-alphanumeric to focus on key words
        val n1 = normalize(name1)
        val n2 = normalize(name2)
        
        // Check if one contains the other (robust enough for MVP)
        // e.g. "Amul Gold 500ml" contains "Amul Gold" -> True
        return n1.contains(n2) || n2.contains(n1)
    }
    
    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "") // Remove special chars
            .replace(Regex("\\d+(ml|g|kg|l)"), "") // Remove quantities like 500ml (risky but helps matching)
            .trim()
    }
}
