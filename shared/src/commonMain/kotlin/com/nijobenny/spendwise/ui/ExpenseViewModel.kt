package com.nijobenny.spendwise.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.model.Money
import com.nijobenny.spendwise.domain.usecase.AddExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.DeleteExpenseUseCase
import com.nijobenny.spendwise.domain.usecase.ExpenseSummary
import com.nijobenny.spendwise.domain.usecase.ExpenseValidator
import com.nijobenny.spendwise.domain.usecase.GetExpenseSummaryUseCase
import com.nijobenny.spendwise.domain.usecase.ObserveExpensesUseCase
import com.nijobenny.spendwise.domain.usecase.RestoreExpenseUseCase
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
    val expenses: List<Expense> = emptyList(),
    val summary: ExpenseSummary = ExpenseSummary(Money.zero("INR"), emptyMap(), 0),
    val isLoading: Boolean = true,
)

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
)

sealed interface UiEvent {
    data object ExpenseSaved : UiEvent
}

class ExpenseViewModel(
    observeExpenses: ObserveExpensesUseCase,
    getSummary: GetExpenseSummaryUseCase,
    private val addExpense: AddExpenseUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    private val restoreExpense: RestoreExpenseUseCase,
    private val currentTimeMillis: () -> Long,
    private val currencyCode: String = "INR",
) : ViewModel() {

    val homeState: StateFlow<HomeUiState> =
        combine(observeExpenses(), getSummary(currencyCode)) { expenses, summary ->
            HomeUiState(expenses = expenses, summary = summary, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

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

    fun save() {
        val form = _form.value
        if (form.isSaving) return
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            val normalizedAmount = form.amount.trimEnd('.').let { if (it.startsWith(".")) "0$it" else it }
            when (val result = addExpense(form.title, normalizedAmount, form.category, currencyCode, form.note)) {
                is ExpenseValidator.Result.Valid -> {
                    resetForm()
                    _events.send(UiEvent.ExpenseSaved)
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
            listOf(
                Sample("Dinner at Paragon", 124000, Category.FOOD, 2 * hour),
                Sample("Uber to Infopark", 38500, Category.TRANSPORT, 5 * hour),
                Sample("Electricity bill", 214000, Category.BILLS, 26 * hour),
                Sample("Movie night", 64000, Category.ENTERTAINMENT, 50 * hour),
                Sample("New headphones", 349900, Category.SHOPPING, 74 * hour),
                Sample("Pharmacy", 42050, Category.HEALTH, 98 * hour),
                Sample("Coffee & snacks", 18000, Category.FOOD, 120 * hour),
                Sample("Metro card top-up", 50000, Category.TRANSPORT, 150 * hour),
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

    private data class Sample(val title: String, val amountMinor: Long, val category: Category, val ageMillis: Long)
}
