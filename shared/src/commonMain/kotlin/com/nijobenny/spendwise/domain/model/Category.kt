package com.nijobenny.spendwise.domain.model

/** Spending categories shown in the app. */
enum class Category(val displayName: String, val emoji: String) {
    FOOD("Food", "🍔"),
    TRANSPORT("Transport", "🚕"),
    SHOPPING("Shopping", "🛍️"),
    BILLS("Bills", "🧾"),
    HEALTH("Health", "💊"),
    ENTERTAINMENT("Entertainment", "🎬"),
    OTHER("Other", "📦"),
}
