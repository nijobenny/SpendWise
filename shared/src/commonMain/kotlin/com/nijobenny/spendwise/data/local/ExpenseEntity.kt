package com.nijobenny.spendwise.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.model.Money

/** Database row. Kept separate from the domain [Expense] so storage details never leak into business logic. */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val category: String,
    val createdAtMillis: Long,
    val note: String?,
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    title = title,
    amount = Money(amountMinor, currencyCode),
    category = Category.entries.firstOrNull { it.name == category } ?: Category.OTHER,
    createdAtMillis = createdAtMillis,
    note = note,
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    title = title,
    amountMinor = amount.amountMinor,
    currencyCode = amount.currencyCode,
    category = category.name,
    createdAtMillis = createdAtMillis,
    note = note,
)
