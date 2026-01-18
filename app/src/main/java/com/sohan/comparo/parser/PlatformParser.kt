package com.sohan.comparo.parser

import org.json.JSONArray
import org.json.JSONObject

data class ProductInfo(
    val name: String,
    val price: Double,
    val etaMinutes: Int?,
    val platform: String,
    val inStock: Boolean,
    val imageUrl: String?,
    val originalPrice: Double?
)

object PlatformParser {
    
    private val DELIVERY_TIME_REGEX = Regex("(\\d+)")
    
    fun parseSwiggy(jsonResponse: String): List<ProductInfo> {
        return try {
            val json = JSONObject(jsonResponse)
            val products = mutableListOf<ProductInfo>()
            
            // Try data.widgets array
            if (json.has("data")) {
                val data = json.getJSONObject("data")
                
                // Try widgets array
                if (data.has("widgets")) {
                    val widgets = data.getJSONArray("widgets")
                    for (i in 0 until widgets.length()) {
                        val widget = widgets.getJSONObject(i)
                        if (widget.has("data")) {
                            val widgetData = widget.getJSONArray("data")
                            for (j in 0 until widgetData.length()) {
                                val item = widgetData.getJSONObject(j)
                                parseSwiggyItem(item)?.let { products.add(it) }
                            }
                        }
                    }
                }
                
                // Try items array
                if (data.has("items")) {
                    val items = data.getJSONArray("items")
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        parseSwiggyItem(item)?.let { products.add(it) }
                    }
                }
            }
            
            products
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseSwiggyItem(item: JSONObject): ProductInfo? {
        return try {
            val name = item.optString("display_name", item.optString("name", ""))
            if (name.isEmpty()) return null
            
            val priceObj = item.optJSONObject("price")
            val offerPrice = priceObj?.optDouble("offer_price") ?: priceObj?.optDouble("actual_price") ?: 0.0
            val actualPrice = priceObj?.optDouble("actual_price")
            
            if (offerPrice <= 0) return null
            
            val etaObj = item.optJSONObject("eta")
            val etaMinutes = etaObj?.optInt("eta_in_mins")
            
            val imageUrl = item.optString("image_url", null)
            
            ProductInfo(
                name = name,
                price = offerPrice,
                etaMinutes = etaMinutes,
                platform = "Swiggy",
                inStock = true,
                imageUrl = imageUrl,
                originalPrice = if (actualPrice != null && actualPrice > offerPrice) actualPrice else null
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun parseZepto(jsonResponse: String): List<ProductInfo> {
        return try {
            val json = JSONObject(jsonResponse)
            val products = mutableListOf<ProductInfo>()
            
            if (json.has("products")) {
                val productsArray = json.getJSONArray("products")
                for (i in 0 until productsArray.length()) {
                    val item = productsArray.getJSONObject(i)
                    parseZeptoItem(item)?.let { products.add(it) }
                }
            }
            
            products
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseZeptoItem(item: JSONObject): ProductInfo? {
        return try {
            val outOfStock = item.optBoolean("out_of_stock", false)
            if (outOfStock) return null
            
            val name = item.optString("name", "")
            if (name.isEmpty()) return null
            
            val price = item.optDouble("price", 0.0)
            if (price <= 0) return null
            
            val mrp = item.optDouble("mrp")
            val etaMinutes = item.optInt("eta_in_mins")
            val imageUrl = item.optString("image_url", null)
            
            ProductInfo(
                name = name,
                price = price,
                etaMinutes = if (etaMinutes > 0) etaMinutes else null,
                platform = "Zepto",
                inStock = true,
                imageUrl = imageUrl,
                originalPrice = if (mrp > price) mrp else null
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun parseBlinkit(jsonResponse: String): List<ProductInfo> {
        return try {
            val json = JSONObject(jsonResponse)
            val products = mutableListOf<ProductInfo>()
            
            // Try products array directly
            if (json.has("products")) {
                val productsArray = json.getJSONArray("products")
                for (i in 0 until productsArray.length()) {
                    val item = productsArray.getJSONObject(i)
                    parseBlinkitItem(item)?.let { products.add(it) }
                }
            }
            
            // Try objects.products array
            if (json.has("objects")) {
                val objects = json.getJSONObject("objects")
                if (objects.has("products")) {
                    val productsArray = objects.getJSONArray("products")
                    for (i in 0 until productsArray.length()) {
                        val item = productsArray.getJSONObject(i)
                        parseBlinkitItem(item)?.let { products.add(it) }
                    }
                }
            }
            
            products
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseBlinkitItem(item: JSONObject): ProductInfo? {
        return try {
            val available = item.optBoolean("available", true)
            if (!available) return null
            
            val name = item.optString("name", "")
            if (name.isEmpty()) return null
            
            val price = item.optDouble("price", 0.0)
            if (price <= 0) return null
            
            val mrp = item.optDouble("mrp")
            val estimatedDeliveryTime = item.optString("estimated_delivery_time", null)
            val imageUrl = item.optString("image_url", null)
            
            // Parse delivery time if it's a string like "10 mins"
            val etaMinutes = estimatedDeliveryTime?.let {
                DELIVERY_TIME_REGEX.find(it)?.value?.toIntOrNull()
            }
            
            ProductInfo(
                name = name,
                price = price,
                etaMinutes = etaMinutes,
                platform = "Blinkit",
                inStock = true,
                imageUrl = imageUrl,
                originalPrice = if (mrp > price) mrp else null
            )
        } catch (e: Exception) {
            null
        }
    }
}
