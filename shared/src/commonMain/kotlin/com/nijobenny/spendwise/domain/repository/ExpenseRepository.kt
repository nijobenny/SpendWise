package com.nijobenny.spendwise.domain.repository

import com.nijobenny.spendwise.domain.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract. The data layer provides the implementation
 * (in-memory now, SQLDelight database in the next step).
 */
interface ExpenseRepository {
    fun observeExpenses(): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Long
    suspend fun deleteExpense(id: Long)
}
