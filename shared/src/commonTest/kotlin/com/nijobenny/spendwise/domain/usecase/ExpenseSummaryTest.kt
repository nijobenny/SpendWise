package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpenseSummaryTest {

    private fun expense(amountMinor: Long, category: Category, currency: String = "INR") =
        Expense(title = "x", amount = Money(amountMinor, currency), category = category, createdAtMillis = 0)

    @Test
    fun emptyList_givesZeroTotal() {
        val summary = GetExpenseSummaryUseCase.summarize(emptyList(), "INR")
        assertEquals(Money.zero("INR"), summary.total)
        assertEquals(0, summary.count)
    }

    @Test
    fun totals_areGroupedByCategory_largestFirst() {
        val summary = GetExpenseSummaryUseCase.summarize(
            listOf(
                expense(10000, Category.FOOD),
                expense(5000, Category.TRANSPORT),
                expense(2500, Category.FOOD),
            ),
            "INR",
        )
        assertEquals(Money(17500, "INR"), summary.total)
        assertEquals(Money(12500, "INR"), summary.byCategory[Category.FOOD])
        assertEquals(Category.FOOD, summary.byCategory.keys.first())
        assertEquals(3, summary.count)
    }

    @Test
    fun otherCurrencies_areExcluded() {
        val summary = GetExpenseSummaryUseCase.summarize(
            listOf(expense(10000, Category.FOOD), expense(999, Category.FOOD, "EUR")),
            "INR",
        )
        assertEquals(Money(10000, "INR"), summary.total)
        assertEquals(1, summary.count)
    }

    @Test
    fun money_formatsWithThousandsSeparator() {
        assertEquals("INR 1,250.50", Money(125050, "INR").format())
        assertEquals("EUR 0.05", Money(5, "EUR").format())
    }
}
