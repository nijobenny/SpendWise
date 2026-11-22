package com.nijobenny.spendwise.data.repository

import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Temporary implementation used until the SQLDelight database is added.
 * Also handy as a fake in tests.
 */
class InMemoryExpenseRepository : ExpenseRepository {

    private val expenses = MutableStateFlow<List<Expense>>(emptyList())
    private var nextId = 1L

    override fun observeExpenses(): Flow<List<Expense>> = expenses.asStateFlow()

    override suspend fun addExpense(expense: Expense): Long {
        val id = nextId++
        expenses.update { it + expense.copy(id = id) }
        return id
    }

    override suspend fun deleteExpense(id: Long) {
        expenses.update { list -> list.filterNot { it.id == id } }
    }
}
