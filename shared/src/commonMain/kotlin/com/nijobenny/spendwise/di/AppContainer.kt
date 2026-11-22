package com.nijobenny.spendwise.di

import com.nijobenny.spendwise.data.repository.InMemoryExpenseRepository
import com.nijobenny.spendwise.domain.repository.ExpenseRepository
import com.nijobenny.spendwise.domain.usecase.AddExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.DeleteExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.GetExpenseSummaryUseCase
import com.nijobenny.spendwise.domain.usecase.ObserveExpensesUseCase
import com.nijobenny.spendwise.domain.usecase.RestoreExpenseUseCase
import com.nijobenny.spendwise.platform.currentTimeMillis
import com.nijobenny.spendwise.ui.ExpenseViewModel

/** Simple manual dependency injection. Will be swapped for Koin in a later step. */
object AppContainer {

    private val repository: ExpenseRepository by lazy { InMemoryExpenseRepository() }

    fun createExpenseViewModel(): ExpenseViewModel = ExpenseViewModel(
        observeExpenses = ObserveExpensesUseCase(repository),
        getSummary = GetExpenseSummaryUseCase(repository),
        addExpense = AddExpenseUseCase(repository, ::currentTimeMillis),
        deleteExpense = DeleteExpenseUseCase(repository),
        restoreExpense = RestoreExpenseUseCase(repository),
        currentTimeMillis = ::currentTimeMillis,
    )
}
