package com.nijobenny.spendwise.di

import com.nijobenny.spendwise.data.local.SpendWiseDatabase
import com.nijobenny.spendwise.data.local.buildSpendWiseDatabase
import com.nijobenny.spendwise.data.remote.FrankfurterApi
import com.nijobenny.spendwise.data.remote.createHttpClient
import com.nijobenny.spendwise.data.repository.FrankfurterExchangeRateRepository
import com.nijobenny.spendwise.data.repository.RoomExpenseRepository
import com.nijobenny.spendwise.domain.model.MonthKey
import com.nijobenny.spendwise.domain.repository.ExchangeRateRepository
import com.nijobenny.spendwise.domain.repository.ExpenseRepository
import com.nijobenny.spendwise.domain.usecase.AddExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.DeleteExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.GetExchangeRatesUseCase
import com.nijobenny.spendwise.domain.usecase.ObserveExpensesUseCase
import com.nijobenny.spendwise.domain.usecase.RestoreExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.UpdateExpenseUseCase
import com.nijobenny.spendwise.platform.currentTimeMillis
import com.nijobenny.spendwise.ui.ExpenseViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single<SpendWiseDatabase> { buildSpendWiseDatabase() }
    single { get<SpendWiseDatabase>().expenseDao() }
    single<ExpenseRepository> { RoomExpenseRepository(get()) }

    single { createHttpClient() }
    single { FrankfurterApi(get()) }
    single<ExchangeRateRepository> { FrankfurterExchangeRateRepository(get(), ::currentTimeMillis) }
}

val domainModule = module {
    factory { ObserveExpensesUseCase(get()) }
    factory { AddExpenseUseCase(get(), ::currentTimeMillis) }
    factory { UpdateExpenseUseCase(get()) }
    factory { DeleteExpenseUseCase(get()) }
    factory { RestoreExpenseUseCase(get()) }
    factory { GetExchangeRatesUseCase(get()) }
}

val uiModule = module {
    viewModel {
        ExpenseViewModel(
            observeExpenses = get(),
            addExpense = get(),
            updateExpense = get(),
            deleteExpense = get(),
            restoreExpense = get(),
            getExchangeRates = get(),
            currentTimeMillis = ::currentTimeMillis,
            monthOf = { millis -> MonthKey.fromEpochMillis(millis) },
        )
    }
}

private var koinStarted = false

/** Starts Koin once per process. Called from Android's MainActivity and iOS's MainViewController. */
fun initKoin() {
    if (koinStarted) return
    startKoin { modules(dataModule, domainModule, uiModule) }
    koinStarted = true
}
