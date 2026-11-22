package com.nijobenny.spendwise.ui.theme

import androidx.compose.ui.graphics.Color
import com.nijobenny.spendwise.domain.model.Category

/** UI-only colour for each category (kept out of the domain layer). */
val Category.color: Color
    get() = when (this) {
        Category.FOOD -> Color(0xFFF59E0B)
        Category.TRANSPORT -> SpendColors.Cyan
        Category.SHOPPING -> SpendColors.Pink
        Category.BILLS -> SpendColors.Violet
        Category.HEALTH -> SpendColors.Emerald
        Category.ENTERTAINMENT -> SpendColors.Rose
        Category.OTHER -> Color(0xFF94A3B8)
    }
