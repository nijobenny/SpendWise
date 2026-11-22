package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.model.Money
import com.nijobenny.spendwise.domain.repository.ExpenseRepository

class AddExpenseUseCase(
    private val repository: ExpenseRepository,
    private val currentTimeMillis: () -> Long,
) {
    suspend operator fun invoke(
        title: String,
        amount: String,
        category: Category,
        currencyCode: String = "INR",
        note: String? = null,
    ): ExpenseValidator.Result {
        val result = ExpenseValidator.validate(title, amount)
        if (result is ExpenseValidator.Result.Valid) {
            repository.addExpense(
                Expense(
                    title = result.title,
                    amount = Money(result.amountMinor, currencyCode),
                    category = category,
                    createdAtMillis = currentTimeMillis(),
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                )
            )
        }
        return result
    }
}
