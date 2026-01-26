package com.sohan.comparo.parser

import org.junit.Assert.*
import org.junit.Test

class PlatformParserTest {

    @Test
    fun testParseSwiggy_ValidJson() {
        val json = """
            {
                "data": {
                    "widgets": [
                        {
                            "data": [
                                {
                                    "display_name": "Amul Milk",
                                    "price": {
                                        "offer_price": 32.0,
                                        "actual_price": 34.0
                                    },
                                    "in_stock": true,
                                    "eta": {
                                        "eta_in_mins": 15
                                    }
                                }
                            ]
                        }
                    ]
                }
            }
        """.trimIndent()

        val results = PlatformParser.parseSwiggy(json)
        assertEquals(1, results.size)
        assertEquals("Amul Milk", results[0].name)
        assertEquals(32.0, results[0].price, 0.01)
        assertEquals(15, results[0].etaMinutes)
        assertEquals("Swiggy", results[0].platform)
    }

    @Test
    fun testParseZepto_ValidJson() {
        val json = """
            {
                "products": [
                    {
                        "name": "Bread",
                        "price": 40.0,
                        "mrp": 45.0,
                        "out_of_stock": false,
                        "eta_in_mins": 10
                    }
                ]
            }
        """.trimIndent()

        val results = PlatformParser.parseZepto(json)
        assertEquals(1, results.size)
        assertEquals("Bread", results[0].name)
        assertEquals(40.0, results[0].price, 0.01)
        assertEquals("Zepto", results[0].platform)
    }

    @Test
    fun testParseBlinkit_ValidJson() {
        val json = """
            {
                "products": [
                    {
                        "name": "Eggs",
                        "price": 100.0,
                        "mrp": 120.0,
                        "available": true,
                        "estimated_delivery_time": "12 mins"
                    }
                ]
            }
        """.trimIndent()

        val results = PlatformParser.parseBlinkit(json)
        assertEquals(1, results.size)
        assertEquals("Eggs", results[0].name)
        assertEquals(100.0, results[0].price, 0.01)
        assertEquals(12, results[0].etaMinutes)
        assertEquals("Blinkit", results[0].platform)
    }

    @Test
    fun testParse_EmptyOrInvalidJson() {
        assertEquals(0, PlatformParser.parseSwiggy("{}").size)
        assertEquals(0, PlatformParser.parseZepto("invalid").size)
    }
}
