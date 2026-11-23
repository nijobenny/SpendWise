package com.nijobenny.spendwise.ui

import com.nijobenny.spendwise.data.repository.InMemoryExpenseRepository
import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.model.ExchangeRates
import com.nijobenny.spendwise.domain.model.Money
import com.nijobenny.spendwise.domain.model.MonthKey
import com.nijobenny.spendwise.domain.repository.ExchangeRateRepository
import com.nijobenny.spendwise.domain.usecase.AddExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.DeleteExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.ExpenseValidator
import com.nijobenny.spendwise.domain.usecase.GetExchangeRatesUseCase
import com.nijobenny.spendwise.domain.usecase.ObserveExpensesUseCase
import com.nijobenny.spendwise.domain.usecase.RestoreExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.UpdateExpenseUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: InMemoryExpenseRepository
    private val now = 1_000L

    /** Every timestamp below 10_000 is "November 2026"; anything else is "October 2026". */
    private val monthOf: (Long) -> MonthKey = { if (it < 10_000) MonthKey(2026, 11) else MonthKey(2026, 10) }

    private class FakeRates(private val fail: Boolean = false) : ExchangeRateRepository {
        override suspend fun getRates(base: String, targets: List<String>): Result<ExchangeRates> =
            if (fail) Result.failure(Exception("offline"))
            else Result.success(ExchangeRates(base, mapOf("EUR" to 0.01), "2026-11-23"))
    }

    private fun createViewModel(ratesFail: Boolean = false) = ExpenseViewModel(
        observeExpenses = ObserveExpensesUseCase(repository),
        addExpense = AddExpenseUseCase(repository) { now },
        updateExpense = UpdateExpenseUseCase(repository),
        deleteExpense = DeleteExpenseUseCase(repository),
        restoreExpense = RestoreExpenseUseCase(repository),
        getExchangeRates = GetExchangeRatesUseCase(FakeRates(ratesFail)),
        currentTimeMillis = { now },
        monthOf = monthOf,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = InMemoryExpenseRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun save_withEmptyTitle_showsErrorAndTriggersShake() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onAmountChange("100")
        viewModel.save()

        assertEquals(ExpenseValidator.Error.EMPTY_TITLE, viewModel.formState.value.error)
        assertEquals(1, viewModel.formState.value.errorTrigger)
        assertTrue(repository.observeExpenses().first().isEmpty())
    }

    @Test
    fun save_withValidInput_storesExpenseEmitsEventAndResetsForm() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val events = mutableListOf<UiEvent>()
        backgroundScope.launch { viewModel.events.collect { events += it } }

        viewModel.onTitleChange("Lunch")
        viewModel.onAmountChange("250.5")
        viewModel.onCategorySelect(Category.FOOD)
        viewModel.save()

        val saved = repository.observeExpenses().first().single()
        assertEquals("Lunch", saved.title)
        assertEquals(Money(25050, "INR"), saved.amount)
        assertEquals(listOf<UiEvent>(UiEvent.ExpenseSaved(edited = false)), events)
        assertEquals("", viewModel.formState.value.title)
    }

    @Test
    fun editing_updatesExistingExpenseInsteadOfAddingNew() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTitleChange("Taxi")
        viewModel.onAmountChange("300")
        viewModel.save()
        val original = repository.observeExpenses().first().single()

        viewModel.startEditing(original)
        assertEquals("300", viewModel.formState.value.amount)
        assertTrue(viewModel.formState.value.isEditing)

        viewModel.onAmountChange("350.75")
        viewModel.save()

        val all = repository.observeExpenses().first()
        assertEquals(1, all.size)
        assertEquals(original.id, all.single().id)
        assertEquals(35075, all.single().amount.amountMinor)
    }

    @Test
    fun amountInput_rejectsMoreThanTwoDecimals() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onAmountChange("12.34")
        viewModel.onAmountChange("12.345")
        assertEquals("12.34", viewModel.formState.value.amount)
    }

    @Test
    fun homeState_showsOnlySelectedMonth_andCanNavigateBack() = runTest(dispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.homeState.collect {} }

        viewModel.onTitleChange("This month")
        viewModel.onAmountChange("100")
        viewModel.save()

        assertEquals(Money(10000, "INR"), viewModel.homeState.value.insights.summary.total)
        assertEquals(false, viewModel.homeState.value.canGoToNextMonth)

        viewModel.showPreviousMonth()
        assertEquals(MonthKey(2026, 10), viewModel.homeState.value.insights.month)
        assertEquals(0, viewModel.homeState.value.insights.summary.count)
        assertTrue(viewModel.homeState.value.canGoToNextMonth)
    }

    @Test
    fun rates_loadSuccessfully_orShowError() = runTest(dispatcher) {
        assertIs<RatesUiState.Ready>(createViewModel().ratesState.value)
        assertIs<RatesUiState.Error>(createViewModel(ratesFail = true).ratesState.value)
    }

    @Test
    fun deleteThenRestore_bringsExpenseBack() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTitleChange("Coffee")
        viewModel.onAmountChange("80")
        viewModel.save()
        val expense = repository.observeExpenses().first().single()

        viewModel.delete(expense)
        assertTrue(repository.observeExpenses().first().isEmpty())

        viewModel.restore(expense)
        assertNull(viewModel.formState.value.error)
        assertEquals(expense, repository.observeExpenses().first().single())
    }
}
