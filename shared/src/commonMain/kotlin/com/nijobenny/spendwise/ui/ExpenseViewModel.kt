package com.nijobenny.spendwise.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.model.ExchangeRates
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.model.Money
import com.nijobenny.spendwise.domain.model.MonthKey
import com.nijobenny.spendwise.domain.usecase.AddExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.DeleteExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.ExpenseValidator
import com.nijobenny.spendwise.domain.usecase.GetExchangeRatesUseCase
import com.nijobenny.spendwise.domain.usecase.MonthlyInsights
import com.nijobenny.spendwise.domain.usecase.ObserveExpensesUseCase
import com.nijobenny.spendwise.domain.usecase.RestoreExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.UpdateExpenseUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val insights: MonthlyInsights,
    val currentMonth: MonthKey,
    val isLoading: Boolean = true,
) {
    val canGoToNextMonth: Boolean get() = insights.month < currentMonth
}

data class AddExpenseUiState(
    val title: String = "",
    val amount: String = "",
    val category: Category = Category.FOOD,
    val note: String = "",
    val currencyCode: String = "INR",
    val error: ExpenseValidator.Error? = null,
    /** Increments on every failed save so the UI can replay the shake animation. */
    val errorTrigger: Int = 0,
    val isSaving: Boolean = false,
    /** Non-null when editing an existing expense instead of adding a new one. */
    val editing: Expense? = null,
) {
    val isEditing: Boolean get() = editing != null
}

sealed interface RatesUiState {
    data object Loading : RatesUiState
    data class Ready(val rates: ExchangeRates) : RatesUiState
    data object Error : RatesUiState
}

sealed interface UiEvent {
    data class ExpenseSaved(val edited: Boolean) : UiEvent
}

class ExpenseViewModel(
    observeExpenses: ObserveExpensesUseCase,
    private val addExpense: AddExpenseUseCase,
    private val updateExpense: UpdateExpenseUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    private val restoreExpense: RestoreExpenseUseCase,
    private val getExchangeRates: GetExchangeRatesUseCase,
    private val currentTimeMillis: () -> Long,
    private val monthOf: (Long) -> MonthKey,
    private val currencyCode: String = "INR",
) : ViewModel() {

    private fun currentMonth(): MonthKey = monthOf(currentTimeMillis())

    private val selectedMonth = MutableStateFlow(currentMonth())

    val homeState: StateFlow<HomeUiState> =
        combine(observeExpenses(), selectedMonth) { expenses, month ->
            HomeUiState(
                insights = MonthlyInsights.compute(expenses, month, currencyCode, monthOf),
                currentMonth = currentMonth(),
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                insights = MonthlyInsights.empty(selectedMonth.value, currencyCode),
                currentMonth = selectedMonth.value,
            ),
        )

    fun showPreviousMonth() = selectedMonth.update { it.previous() }

    fun showNextMonth() = selectedMonth.update { if (it < currentMonth()) it.next() else it }

    fun selectMonth(month: MonthKey) {
        if (month <= currentMonth()) selectedMonth.value = month
    }

    private val _rates = MutableStateFlow<RatesUiState>(RatesUiState.Loading)
    val ratesState: StateFlow<RatesUiState> = _rates.asStateFlow()

    init {
        loadRates()
    }

    fun loadRates() {
        viewModelScope.launch {
            _rates.value = RatesUiState.Loading
            _rates.value = getExchangeRates(base = currencyCode).fold(
                onSuccess = { RatesUiState.Ready(it) },
                onFailure = { RatesUiState.Error },
            )
        }
    }

    private val _form = MutableStateFlow(AddExpenseUiState(currencyCode = currencyCode))
    val formState: StateFlow<AddExpenseUiState> = _form.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    fun onTitleChange(value: String) {
        _form.update { it.copy(title = value.take(ExpenseValidator.MAX_TITLE_LENGTH), error = null) }
    }

    /** Only allows digits with at most one decimal point and 2 decimals. */
    fun onAmountChange(input: String) {
        val cleaned = input.replace(',', '.').filter { it.isDigit() || it == '.' }
        if (cleaned.count { it == '.' } > 1) return
        val parts = cleaned.split('.')
        if (parts[0].length > 7) return
        if (parts.size == 2 && parts[1].length > 2) return
        _form.update { it.copy(amount = cleaned, error = null) }
    }

    fun onCategorySelect(category: Category) = _form.update { it.copy(category = category) }

    fun onNoteChange(value: String) = _form.update { it.copy(note = value.take(120)) }

    fun resetForm() {
        _form.value = AddExpenseUiState(currencyCode = currencyCode)
    }

    /** Pre-fills the form with [expense] so it can be edited. */
    fun startEditing(expense: Expense) {
        _form.value = AddExpenseUiState(
            title = expense.title,
            amount = minorToInput(expense.amount.amountMinor),
            category = expense.category,
            note = expense.note.orEmpty(),
            currencyCode = expense.amount.currencyCode,
            editing = expense,
        )
    }

    fun save() {
        val form = _form.value
        if (form.isSaving) return
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            val normalizedAmount = form.amount.trimEnd('.').let { if (it.startsWith(".")) "0$it" else it }
            val existing = form.editing
            val result = if (existing != null) {
                updateExpense(existing, form.title, normalizedAmount, form.category, form.note)
            } else {
                addExpense(form.title, normalizedAmount, form.category, currencyCode, form.note)
            }
            when (result) {
                is ExpenseValidator.Result.Valid -> {
                    resetForm()
                    if (existing == null) selectedMonth.value = currentMonth() // show where the new expense landed
                    _events.send(UiEvent.ExpenseSaved(edited = existing != null))
                }
                is ExpenseValidator.Result.Invalid -> _form.update {
                    it.copy(error = result.error, errorTrigger = it.errorTrigger + 1, isSaving = false)
                }
            }
        }
    }

    fun delete(expense: Expense) {
        viewModelScope.launch { deleteExpense(expense.id) }
    }

    fun restore(expense: Expense) {
        viewModelScope.launch { restoreExpense(expense) }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            val now = currentTimeMillis()
            val hour = 3_600_000L
            val day = 24 * hour
            listOf(
                Sample("Dinner at Paragon", 124000, Category.FOOD, 2 * hour),
                Sample("Uber to Infopark", 38500, Category.TRANSPORT, 5 * hour),
                Sample("Electricity bill", 214000, Category.BILLS, 1 * day),
                Sample("Movie night", 64000, Category.ENTERTAINMENT, 2 * day),
                Sample("New headphones", 349900, Category.SHOPPING, 3 * day),
                Sample("Pharmacy", 42050, Category.HEALTH, 4 * day),
                Sample("Coffee & snacks", 18000, Category.FOOD, 5 * day),
                Sample("Metro card top-up", 50000, Category.TRANSPORT, 6 * day),
                Sample("Rent share", 900000, Category.BILLS, 32 * day),
                Sample("Groceries", 265000, Category.FOOD, 36 * day),
                Sample("Concert tickets", 180000, Category.ENTERTAINMENT, 40 * day),
                Sample("Rent share", 900000, Category.BILLS, 62 * day),
                Sample("Running shoes", 459900, Category.SHOPPING, 66 * day),
                Sample("Doctor visit", 120000, Category.HEALTH, 70 * day),
                Sample("Rent share", 900000, Category.BILLS, 93 * day),
                Sample("Weekend trip", 640000, Category.TRANSPORT, 98 * day),
                Sample("Rent share", 900000, Category.BILLS, 124 * day),
                Sample("Birthday gift", 250000, Category.SHOPPING, 128 * day),
                Sample("Rent share", 900000, Category.BILLS, 155 * day),
                Sample("Team lunch", 85000, Category.FOOD, 158 * day),
            ).forEach { sample ->
                restoreExpense(
                    Expense(
                        title = sample.title,
                        amount = Money(sample.amountMinor, currencyCode),
                        category = sample.category,
                        createdAtMillis = now - sample.ageMillis,
                    )
                )
            }
        }
    }

    /** 25050 -> "250.5", 25000 -> "250", 25005 -> "250.05" (what a user would type). */
    private fun minorToInput(amountMinor: Long): String {
        val major = amountMinor / 100
        val cents = amountMinor % 100
        if (cents == 0L) return major.toString()
        return "$major.${cents.toString().padStart(2, '0').trimEnd('0')}"
    }

    private data class Sample(val title: String, val amountMinor: Long, val category: Category, val ageMillis: Long)
}
