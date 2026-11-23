package com.nijobenny.spendwise.domain.model

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** A calendar month, e.g. MonthKey(2026, 11) = November 2026. [month] is 1..12. */
data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {

    init {
        require(month in 1..12) { "month must be 1..12" }
    }

    fun previous(): MonthKey = if (month == 1) MonthKey(year - 1, 12) else MonthKey(year, month - 1)

    fun next(): MonthKey = if (month == 12) MonthKey(year + 1, 1) else MonthKey(year, month + 1)

    override fun compareTo(other: MonthKey): Int =
        compareValuesBy(this, other, { it.year }, { it.month })

    companion object {
        @OptIn(ExperimentalTime::class)
        fun fromEpochMillis(millis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): MonthKey {
            val dateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone)
            return MonthKey(dateTime.year, dateTime.month.ordinal + 1)
        }
    }
}
