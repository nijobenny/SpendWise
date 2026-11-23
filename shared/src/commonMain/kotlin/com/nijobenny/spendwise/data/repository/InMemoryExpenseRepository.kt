package com.nijobenny.spendwise.data.repository

import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory implementation, used as a fake in unit tests.
 * Like Room's REPLACE strategy: an expense with an existing id is updated, id = 0 gets a new id.
 */
class InMemoryExpenseRepository : ExpenseRepository {

    private val expenses = MutableStateFlow<List<Expense>>(emptyList())
    private var nextId = 1L

    override fun observeExpenses(): Flow<List<Expense>> = expenses.asStateFlow()

    override suspend fun addExpense(expense: Expense): Long {
        val id = if (expense.id != 0L) expense.id else nextId++
        if (id >= nextId) nextId = id + 1
        val saved = expense.copy(id = id)
        expenses.update { list -> list.filterNot { it.id == id } + saved }
        return id
    }

    override suspend fun deleteExpense(id: Long) {
        expenses.update { list -> list.filterNot { it.id == id } }
    }
}
