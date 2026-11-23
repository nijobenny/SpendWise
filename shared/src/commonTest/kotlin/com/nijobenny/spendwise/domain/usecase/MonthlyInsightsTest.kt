package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.model.MonthKey
import com.nijobenny.spendwise.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MonthlyInsightsTest {

    // Fake clock: the timestamp *is* the month number in 2026, so tests are time-zone independent.
    private val monthOf: (Long) -> MonthKey = { MonthKey(2026, it.toInt()) }

    private fun expense(month: Int, amountMinor: Long) = Expense(
        title = "x",
        amount = Money(amountMinor, "INR"),
        category = Category.FOOD,
        createdAtMillis = month.toLong(),
    )

    @Test
    fun selectedMonth_onlyIncludesItsExpenses() {
        val all = listOf(expense(10, 5000), expense(11, 1000), expense(11, 2000))
        val insights = MonthlyInsights.compute(all, MonthKey(2026, 11), "INR", monthOf)
        assertEquals(2, insights.expenses.size)
        assertEquals(Money(3000, "INR"), insights.summary.total)
        assertEquals(Money(5000, "INR"), insights.previousMonthTotal)
    }

    @Test
    fun changeVsPrevious_isPercentage() {
        val all = listOf(expense(10, 10000), expense(11, 15000))
        val insights = MonthlyInsights.compute(all, MonthKey(2026, 11), "INR", monthOf)
        assertEquals(50, insights.changeVsPreviousPercent)
    }

    @Test
    fun changeVsPrevious_isNullWithoutHistory() {
        val insights = MonthlyInsights.compute(listOf(expense(11, 100)), MonthKey(2026, 11), "INR", monthOf)
        assertNull(insights.changeVsPreviousPercent)
    }

    @Test
    fun trend_hasSixMonthsOldestFirst() {
        val all = listOf(expense(6, 600), expense(11, 1100))
        val trend = MonthlyInsights.compute(all, MonthKey(2026, 11), "INR", monthOf).trend
        assertEquals((6..11).map { MonthKey(2026, it) }, trend.map { it.month })
        assertEquals(600, trend.first().total.amountMinor)
        assertEquals(1100, trend.last().total.amountMinor)
        assertEquals(0, trend[2].total.amountMinor)
    }
}
