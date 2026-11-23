package com.nijobenny.spendwise.domain.model

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonthKeyTest {

    @Test
    fun previousAndNext_wrapAroundYear() {
        assertEquals(MonthKey(2025, 12), MonthKey(2026, 1).previous())
        assertEquals(MonthKey(2027, 1), MonthKey(2026, 12).next())
    }

    @Test
    fun ordering_comparesYearThenMonth() {
        assertTrue(MonthKey(2025, 12) < MonthKey(2026, 1))
        assertTrue(MonthKey(2026, 3) > MonthKey(2026, 2))
    }

    @Test
    fun fromEpochMillis_usesGivenTimeZone() {
        // 2026-11-01T00:00:00Z
        val millis = 1_793_491_200_000L
        assertEquals(MonthKey(2026, 11), MonthKey.fromEpochMillis(millis, TimeZone.UTC))
        assertEquals(MonthKey(2026, 10), MonthKey.fromEpochMillis(millis - 1, TimeZone.UTC))
    }
}
