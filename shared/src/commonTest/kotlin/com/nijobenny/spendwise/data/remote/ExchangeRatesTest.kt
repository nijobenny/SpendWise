package com.nijobenny.spendwise.data.remote

import com.nijobenny.spendwise.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExchangeRatesTest {

    private val dtos = listOf(
        RateDto(date = "2026-11-22", base = "INR", quote = "CHF", rate = 0.009),
        RateDto(date = "2026-11-23", base = "INR", quote = "EUR", rate = 0.01),
    )

    @Test
    fun mapping_usesUppercaseCodesAndLatestDate() {
        val rates = dtos.toExchangeRates("inr")
        assertEquals("INR", rates.base)
        assertEquals(setOf("CHF", "EUR"), rates.rates.keys)
        assertEquals("2026-11-23", rates.date)
    }

    @Test
    fun convert_roundsToMinorUnits() {
        val rates = dtos.toExchangeRates("INR")
        // ₹9,004.50 * 0.01 = €90.045 -> €90.05 (rounded half up)
        assertEquals(Money(9005, "EUR"), rates.convert(Money(900450, "INR"), "EUR"))
    }

    @Test
    fun convert_returnsNullForUnknownCurrency() {
        assertNull(dtos.toExchangeRates("INR").convert(Money(100, "INR"), "JPY"))
    }
}
