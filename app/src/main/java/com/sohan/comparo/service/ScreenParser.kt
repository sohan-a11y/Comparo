package com.sohan.comparo.service

import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

object ScreenParser {
    private const val TAG = "ScreenParser"

    fun parseScreen(rootNode: AccessibilityNodeInfo?): ParsedData {
        if (rootNode == null) return ParsedData(emptyList(), 0.0)

        val textNodes = mutableListOf<String>()
        traverseNode(rootNode, textNodes)

        // Simple Heuristic Extraction
        // 1. Find Price like "₹120" or "₹ 120"
        // 2. Find "Cart Total" or similar keywords
        val prices = textNodes.mapNotNull { extractPrice(it) }
        val probableTotal = prices.maxOrNull() ?: 0.0 // Very naive total detection

        return ParsedData(textNodes, probableTotal)
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return

        if (node.text != null && node.text.isNotEmpty()) {
            list.add(node.text.toString())
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), list)
        }
    }

    private fun extractPrice(text: String): Double? {
        // Regex for "₹ 123" or "Rs 123" or just "123" if context allows
        // Simple regex: ₹\s*(\d+)
        val regex = Regex("₹\\s*([\\d,]+)")
        val match = regex.find(text) ?: return null
        
        return try {
            val numStr = match.groupValues[1].replace(",", "")
            numStr.toDouble()
        } catch (e: Exception) {
            null
        }
    }

    data class ParsedData(
        val allText: List<String>,
        val detectedTotal: Double
    )
}
