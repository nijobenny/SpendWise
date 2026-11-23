package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.model.MonthKey
import com.nijobenny.spendwise.domain.model.Money

data class MonthTotal(val month: MonthKey, val total: Money)

/** Everything the dashboard needs for one selected month. */
data class MonthlyInsights(
    val month: MonthKey,
    val expenses: List<Expense>,
    val summary: ExpenseSummary,
    val previousMonthTotal: Money,
    /** Totals for the last [TREND_MONTHS] months, oldest first, ending with [month]. */
    val trend: List<MonthTotal>,
    /** True if the user has any expenses at all (in any month). */
    val hasAnyExpenses: Boolean,
) {
    /** Percentage change vs previous month, or null when there is nothing to compare against. */
    val changeVsPreviousPercent: Long?
        get() {
            val previous = previousMonthTotal.amountMinor
            if (previous <= 0) return null
            return (summary.total.amountMinor - previous) * 100 / previous
        }

    companion object {
        const val TREND_MONTHS = 6

        fun empty(month: MonthKey, currencyCode: String) = compute(emptyList(), month, currencyCode) { month }

        /**
         * Pure function: groups [all] expenses by month and builds the insights for [month].
         * [monthOf] converts a timestamp to its month (injected so tests don't depend on time zones).
         */
        fun compute(
            all: List<Expense>,
            month: MonthKey,
            currencyCode: String,
            monthOf: (Long) -> MonthKey,
        ): MonthlyInsights {
            val byMonth = all
                .filter { it.amount.currencyCode == currencyCode }
                .groupBy { monthOf(it.createdAtMillis) }

            fun totalOf(key: MonthKey): Money =
                byMonth[key].orEmpty().fold(Money.zero(currencyCode)) { acc, e -> acc + e.amount }

            val monthExpenses = byMonth[month].orEmpty().sortedByDescending { it.createdAtMillis }

            val trend = generateSequence(month) { it.previous() }
                .take(TREND_MONTHS)
                .toList()
                .reversed()
                .map { MonthTotal(it, totalOf(it)) }

            return MonthlyInsights(
                month = month,
                expenses = monthExpenses,
                summary = GetExpenseSummaryUseCase.summarize(monthExpenses, currencyCode),
                previousMonthTotal = totalOf(month.previous()),
                trend = trend,
                hasAnyExpenses = all.isNotEmpty(),
            )
        }
    }
}
