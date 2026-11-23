package com.nijobenny.spendwise.ui

fun currencySymbol(code: String): String = when (code) {
    "INR" -> "₹"
    "EUR" -> "€"
    "USD" -> "$"
    "GBP" -> "£"
    else -> "$code "
}

/** 12550000 paise in INR -> "₹1,25,500.00" (Indian grouping); other currencies use 1,000s grouping. */
fun formatMoney(amountMinor: Long, currencyCode: String, withDecimals: Boolean = true): String {
    val negative = amountMinor < 0
    val abs = if (negative) -amountMinor else amountMinor
    val digits = (abs / 100).toString()
    val cents = (abs % 100).toString().padStart(2, '0')
    val grouped = if (currencyCode == "INR") groupIndian(digits) else groupThousands(digits)
    val body = if (withDecimals) "$grouped.$cents" else grouped
    return (if (negative) "-" else "") + currencySymbol(currencyCode) + body
}

private fun groupThousands(digits: String): String =
    digits.reversed().chunked(3).joinToString(",").reversed()

private fun groupIndian(digits: String): String {
    if (digits.length <= 3) return digits
    val rest = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
    return "$rest,${digits.takeLast(3)}"
}

fun relativeTime(thenMillis: Long, nowMillis: Long): String {
    val minutes = (nowMillis - thenMillis).coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60} h ago"
        minutes < 60 * 48 -> "Yesterday"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)} days ago"
        else -> "${minutes / (60 * 24 * 7)} w ago"
    }
}

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

fun monthName(month: Int): String = MONTH_NAMES[month - 1]

fun shortMonthName(month: Int): String = MONTH_NAMES[month - 1].take(3)

/** Compact amounts for charts: ₹850, ₹9.2k, ₹1.4L (INR) / €1.4M. */
fun formatCompact(amountMinor: Long, currencyCode: String): String {
    val major = amountMinor / 100
    val symbol = currencySymbol(currencyCode)
    fun oneDecimal(value: Long, unit: Long): String {
        val tenths = value * 10 / unit
        return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    }
    return when {
        major < 1_000 -> "$symbol$major"
        currencyCode == "INR" && major >= 1_00_000 -> "$symbol${oneDecimal(major, 1_00_000)}L"
        major >= 1_000_000 -> "$symbol${oneDecimal(major, 1_000_000)}M"
        else -> "$symbol${oneDecimal(major, 1_000)}k"
    }
}
