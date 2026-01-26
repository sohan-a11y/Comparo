package com.sohan.comparo.parser

import java.util.regex.Pattern

data class BankOffer(
    val bankName: String, // HDFC, SBI, AXIS, etc.
    val description: String, // "10% off up to ₹100"
    val discountDescription: String?, // "₹100" or "10%"
    val code: String? = null
)

object OfferParser {

    private val BANKS = mapOf(
        "HDFC" to listOf("HDFC", "H.D.F.C"),
        "SBI" to listOf("SBI", "State Bank"),
        "ICICI" to listOf("ICICI"),
        "AXIS" to listOf("AXIS"),
        "KOTAK" to listOf("KOTAK"),
        "CRED" to listOf("CRED")
    )

    // Regex for "10% off", "Flat ₹50 off"
    private val PERCENT_OFF_REGEX = Pattern.compile("(\\d+)%\\s*off", Pattern.CASE_INSENSITIVE)
    private val FLAT_OFF_REGEX = Pattern.compile("flat\\s*₹?(\\d+)\\s*off", Pattern.CASE_INSENSITIVE)
    private val UP_TO_REGEX = Pattern.compile("up\\s*to\\s*₹?(\\d+)", Pattern.CASE_INSENSITIVE)

    fun parseOffers(rawTextList: List<String>): List<BankOffer> {
        val offers = mutableListOf<BankOffer>()

        rawTextList.forEach { text ->
            val bank = detectBank(text)
            if (bank != null || isGenericOffer(text)) {
                val discount = extractDiscount(text)
                offers.add(BankOffer(
                    bankName = bank ?: "GENERIC",
                    description = text,
                    discountDescription = discount
                ))
            }
        }
        return offers
    }

    private fun detectBank(text: String): String? {
        val upperText = text.uppercase()
        for ((bank, keywords) in BANKS) {
            if (keywords.any { upperText.contains(it) }) {
                return bank
            }
        }
        return null
    }

    private fun isGenericOffer(text: String): Boolean {
        return text.contains("discount", ignoreCase = true) ||
               text.contains("cashback", ignoreCase = true) ||
               text.contains("off", ignoreCase = true)
    }

    private fun extractDiscount(text: String): String? {
        val percentMatcher = PERCENT_OFF_REGEX.matcher(text)
        if (percentMatcher.find()) {
            val percent = percentMatcher.group(1)
            // Check for "up to"
            val upToMatcher = UP_TO_REGEX.matcher(text)
            return if (upToMatcher.find()) {
                "$percent% OFF (upto ₹${upToMatcher.group(1)})"
            } else {
                "$percent% OFF"
            }
        }

        val flatMatcher = FLAT_OFF_REGEX.matcher(text)
        if (flatMatcher.find()) {
            return "Flat ₹${flatMatcher.group(1)} OFF"
        }

        return null
    }
}
