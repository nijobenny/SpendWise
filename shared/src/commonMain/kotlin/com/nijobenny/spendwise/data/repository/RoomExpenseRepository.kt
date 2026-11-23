package com.nijobenny.spendwise.data.repository

import com.nijobenny.spendwise.data.local.ExpenseDao
import com.nijobenny.spendwise.data.local.toDomain
import com.nijobenny.spendwise.data.local.toEntity
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persistent repository backed by Room. Data survives app restarts. */
class RoomExpenseRepository(private val dao: ExpenseDao) : ExpenseRepository {

    override fun observeExpenses(): Flow<List<Expense>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun addExpense(expense: Expense): Long = dao.insert(expense.toEntity())

    override suspend fun deleteExpense(id: Long) = dao.deleteById(id)
}
