package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.repository.ExpenseRepository

/** Re-inserts an existing expense as-is (used for "Undo" after delete and for sample data). */
class RestoreExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(expense: Expense): Long = repository.addExpense(expense)
}
