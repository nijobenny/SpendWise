package com.nijobenny.spendwise.domain.model

/**
 * Money is stored in minor units (paise, cents) as a Long to avoid
 * floating-point rounding errors — the standard approach in fintech apps.
 */
data class Money(
    val amountMinor: Long,
    val currencyCode: String,
) {
    init {
        require(currencyCode.length == 3) { "Currency code must be ISO 4217 (e.g. INR, EUR)" }
    }

    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot add $currencyCode and ${other.currencyCode}" }
        return copy(amountMinor = amountMinor + other.amountMinor)
    }

    /** Formats as e.g. "INR 1,250.50". */
    fun format(): String {
        val major = amountMinor / 100
        val minor = (amountMinor % 100).toString().padStart(2, '0')
        val grouped = major.toString().reversed().chunked(3).joinToString(",").reversed()
        return "$currencyCode $grouped.$minor"
    }

    companion object {
        fun zero(currencyCode: String) = Money(0, currencyCode)
    }
}
