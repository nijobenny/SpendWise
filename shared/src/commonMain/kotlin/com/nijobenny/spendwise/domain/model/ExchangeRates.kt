package com.nijobenny.spendwise.domain.model

import kotlin.math.roundToLong

/** Exchange rates from [base] to other currencies, e.g. base = INR, rates["EUR"] = 0.0098. */
data class ExchangeRates(
    val base: String,
    val rates: Map<String, Double>,
    /** ISO date of the rates, e.g. "2026-11-23". */
    val date: String,
) {
    /** Converts [money] (in [base]) to [targetCurrency], or null if no rate is available. */
    fun convert(money: Money, targetCurrency: String): Money? {
        require(money.currencyCode == base) { "Expected $base but got ${money.currencyCode}" }
        val rate = rates[targetCurrency] ?: return null
        // Both currencies use 2 decimal places, so minor units convert directly.
        return Money((money.amountMinor * rate).roundToLong(), targetCurrency)
    }
}
