package com.nijobenny.spendwise.domain.usecase

/** Validates raw user input before an expense is created. */
object ExpenseValidator {

    sealed interface Result {
        data class Valid(val title: String, val amountMinor: Long) : Result
        data class Invalid(val error: Error) : Result
    }

    enum class Error { EMPTY_TITLE, TITLE_TOO_LONG, INVALID_AMOUNT, AMOUNT_NOT_POSITIVE }

    const val MAX_TITLE_LENGTH = 50

    /** [amountInput] is what the user typed, e.g. "120", "99.5", "1250.75". */
    fun validate(titleInput: String, amountInput: String): Result {
        val title = titleInput.trim()
        if (title.isEmpty()) return Result.Invalid(Error.EMPTY_TITLE)
        if (title.length > MAX_TITLE_LENGTH) return Result.Invalid(Error.TITLE_TOO_LONG)

        val amountMinor = parseToMinor(amountInput.trim())
            ?: return Result.Invalid(Error.INVALID_AMOUNT)
        if (amountMinor <= 0) return Result.Invalid(Error.AMOUNT_NOT_POSITIVE)

        return Result.Valid(title, amountMinor)
    }

    /** "99.5" -> 9950. Returns null for invalid input or more than 2 decimals. */
    internal fun parseToMinor(input: String): Long? {
        if (!Regex("""^-?\d{1,9}(\.\d{1,2})?$""").matches(input)) return null
        val negative = input.startsWith("-")
        val parts = input.removePrefix("-").split(".")
        val major = parts[0].toLong()
        val minor = parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0L
        val total = major * 100 + minor
        return if (negative) -total else total
    }
}
