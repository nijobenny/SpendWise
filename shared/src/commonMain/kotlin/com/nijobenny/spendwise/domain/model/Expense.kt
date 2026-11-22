package com.nijobenny.spendwise.domain.model

/** A single expense entry. Pure Kotlin — no Android or database types. */
data class Expense(
    val id: Long = 0,
    val title: String,
    val amount: Money,
    val category: Category,
    val createdAtMillis: Long,
    val note: String? = null,
)
