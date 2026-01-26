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
            // Check stock status if available
            if (item.has("in_stock")) {
                val inStock = item.optInt("in_stock") == 1 || item.optBoolean("in_stock")
                if (!inStock) return null
            }
            // Some swiggy APIs use 'inventory' object
            if (item.has("inventory")) {
                val inventory = item.getJSONObject("inventory")
                val inStock = inventory.optBoolean("in_stock", true)
                if (!inStock) return null
            }

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

    // --- Cart Parsing ---
    
    data class CartItem(
        val name: String,
        val quantity: Int,
        val unitPrice: Double,
        val totalPrice: Double,
        val platform: String
    )
    
    fun parseSwiggyCart(jsonResponse: String): List<CartItem> {
        return try {
            val json = JSONObject(jsonResponse)
            val items = mutableListOf<CartItem>()
            
            // Common path: data.cart_items or data.cart.items
            val data = json.optJSONObject("data") ?: return emptyList()
            
            // Path 1: data.cart.items
            val cart = data.optJSONObject("cart")
            val cartItems = cart?.optJSONArray("items") ?: data.optJSONArray("cart_items")
            
            if (cartItems != null) {
                for (i in 0 until cartItems.length()) {
                    val item = cartItems.getJSONObject(i)
                    val name = item.optString("name", "Unknown Item")
                    val qty = item.optInt("quantity", 1)
                    val price = item.optDouble("price", 0.0) / 100 // Swiggy often uses paise
                    val total = item.optDouble("item_total", 0.0) / 100
                    
                    items.add(CartItem(name, qty, price, total, "Swiggy"))
                }
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseZeptoCart(jsonResponse: String): List<CartItem> {
        return try {
            val json = JSONObject(jsonResponse)
            val items = mutableListOf<CartItem>()
            
            // Path: store_cart.items
            val storeCart = json.optJSONObject("store_cart") ?: json.optJSONObject("cart")
            val cartItems = storeCart?.optJSONArray("items")
            
            if (cartItems != null) {
                for (i in 0 until cartItems.length()) {
                    val item = cartItems.getJSONObject(i)
                    val product = item.optJSONObject("product")
                    val name = product?.optString("name") ?: item.optString("name", "Unknown")
                    val qty = item.optInt("quantity", 1)
                    val price = product?.optDouble("selling_price", 0.0) ?: item.optDouble("price", 0.0) / 100
                    
                    items.add(CartItem(name, qty, price, price * qty, "Zepto"))
                }
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseBlinkitCart(jsonResponse: String): List<CartItem> {
        return try {
            val json = JSONObject(jsonResponse)
            val items = mutableListOf<CartItem>()
            
            // Path: cart.items
            val cart = json.optJSONObject("cart") ?: json
            val cartItems = cart.optJSONArray("items")
            
            if (cartItems != null) {
                for (i in 0 until cartItems.length()) {
                    val item = cartItems.getJSONObject(i)
                    val name = item.optString("name", "Unknown")
                    val qty = item.optInt("quantity", 1)
                    val price = item.optDouble("price", 0.0) / 100
                    
                    items.add(CartItem(name, qty, price, price * qty, "Blinkit"))
                }
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }
