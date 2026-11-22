package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Expenses, newest first. */
class ObserveExpensesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<Expense>> =
        repository.observeExpenses().map { list -> list.sortedByDescending { it.createdAtMillis } }
}
