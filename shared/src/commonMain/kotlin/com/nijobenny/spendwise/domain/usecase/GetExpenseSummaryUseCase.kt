package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.model.Money
import com.nijobenny.spendwise.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ExpenseSummary(
    val total: Money,
    val byCategory: Map<Category, Money>,
    val count: Int,
)

class GetExpenseSummaryUseCase(private val repository: ExpenseRepository) {

    operator fun invoke(currencyCode: String = "INR"): Flow<ExpenseSummary> =
        repository.observeExpenses().map { summarize(it, currencyCode) }

    companion object {
        /** Pure function — easy to unit test. Only counts expenses in [currencyCode]. */
        fun summarize(expenses: List<Expense>, currencyCode: String): ExpenseSummary {
            val matching = expenses.filter { it.amount.currencyCode == currencyCode }
            val byCategory = matching
                .groupBy { it.category }
                .mapValues { (_, items) -> items.fold(Money.zero(currencyCode)) { acc, e -> acc + e.amount } }
                .toList()
                .sortedByDescending { it.second.amountMinor }
                .toMap()
            val total = byCategory.values.fold(Money.zero(currencyCode)) { acc, m -> acc + m }
            return ExpenseSummary(total, byCategory, matching.size)
        }
    }
}
