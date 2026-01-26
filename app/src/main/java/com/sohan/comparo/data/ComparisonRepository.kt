package com.sohan.comparo.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScannedItem(
    val appName: String, // Swiggy, Zepto, Blinkit
    val productName: String,
    val price: Double,
    val isTotal: Boolean = false
)

object ComparisonRepository {
    
    private val _scannedItems = MutableStateFlow<List<ScannedItem>>(emptyList())
    val scannedItems = _scannedItems.asStateFlow()
    
    private val _status = MutableStateFlow("Idle")
    val status = _status.asStateFlow()

    fun addItem(item: ScannedItem) {
        val current = _scannedItems.value.toMutableList()
        // Remove old entry for same app if exists
        current.removeAll { it.appName == item.appName && it.isTotal == item.isTotal }
        current.add(item)
        _scannedItems.value = current
    }
    
    fun clear() {
        _scannedItems.value = emptyList()
        setStatus("Idle")
    }
    
    fun setStatus(msg: String) {
        _status.value = msg
    }
    
    fun getCheapestApp(): String? {
        val totals = _scannedItems.value.filter { it.isTotal }
        if (totals.isEmpty()) return null
        return totals.minByOrNull { it.price }?.appName
    }
}
