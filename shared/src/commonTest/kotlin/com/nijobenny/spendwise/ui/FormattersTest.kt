package com.nijobenny.spendwise.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class FormattersTest {

    @Test
    fun inr_usesIndianGrouping() {
        assertEquals("₹1,25,500.00", formatMoney(12_550_000, "INR"))
        assertEquals("₹999.50", formatMoney(99_950, "INR"))
    }

    @Test
    fun eur_usesThousandsGrouping() {
        assertEquals("€1,250,000.00", formatMoney(125_000_000, "EUR"))
    }

    @Test
    fun relativeTime_buckets() {
        val now = 10_000_000_000L
        assertEquals("Just now", relativeTime(now - 30_000, now))
        assertEquals("5 min ago", relativeTime(now - 5 * 60_000, now))
        assertEquals("3 h ago", relativeTime(now - 3 * 3_600_000, now))
        assertEquals("Yesterday", relativeTime(now - 30 * 3_600_000L, now))
    }
}
