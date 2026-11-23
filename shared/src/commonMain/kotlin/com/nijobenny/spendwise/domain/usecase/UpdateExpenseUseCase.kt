package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.model.Money
import com.nijobenny.spendwise.domain.repository.ExpenseRepository

/** Validates and saves changes to an existing expense, keeping its id and original date. */
class UpdateExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(
        existing: Expense,
        title: String,
        amount: String,
        category: Category,
        note: String? = null,
    ): ExpenseValidator.Result {
        val result = ExpenseValidator.validate(title, amount)
        if (result is ExpenseValidator.Result.Valid) {
            repository.addExpense(
                existing.copy(
                    title = result.title,
                    amount = Money(result.amountMinor, existing.amount.currencyCode),
                    category = category,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                )
            )
        }
        return result
    }
}
