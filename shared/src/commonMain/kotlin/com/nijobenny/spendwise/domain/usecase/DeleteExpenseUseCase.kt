package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.repository.ExpenseRepository

class DeleteExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteExpense(id)
}
