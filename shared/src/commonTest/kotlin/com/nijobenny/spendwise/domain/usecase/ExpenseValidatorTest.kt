package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.usecase.ExpenseValidator.Error
import com.nijobenny.spendwise.domain.usecase.ExpenseValidator.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpenseValidatorTest {

    @Test
    fun validInput_isTrimmedAndConvertedToMinorUnits() {
        assertEquals(Result.Valid("Lunch", 12050), ExpenseValidator.validate("  Lunch ", "120.5"))
    }

    @Test
    fun emptyTitle_isRejected() {
        assertEquals(Result.Invalid(Error.EMPTY_TITLE), ExpenseValidator.validate("   ", "100"))
    }

    @Test
    fun tooLongTitle_isRejected() {
        val title = "a".repeat(ExpenseValidator.MAX_TITLE_LENGTH + 1)
        assertEquals(Result.Invalid(Error.TITLE_TOO_LONG), ExpenseValidator.validate(title, "100"))
    }

    @Test
    fun nonNumericAmount_isRejected() {
        assertEquals(Result.Invalid(Error.INVALID_AMOUNT), ExpenseValidator.validate("Taxi", "abc"))
    }

    @Test
    fun zeroOrNegativeAmount_isRejected() {
        assertEquals(Result.Invalid(Error.AMOUNT_NOT_POSITIVE), ExpenseValidator.validate("Taxi", "0"))
        assertEquals(Result.Invalid(Error.AMOUNT_NOT_POSITIVE), ExpenseValidator.validate("Taxi", "-5"))
    }

    @Test
    fun parseToMinor_handlesDecimals() {
        assertEquals(100, ExpenseValidator.parseToMinor("1"))
        assertEquals(150, ExpenseValidator.parseToMinor("1.5"))
        assertEquals(105, ExpenseValidator.parseToMinor("1.05"))
        assertNull(ExpenseValidator.parseToMinor("1.555"))
        assertNull(ExpenseValidator.parseToMinor(""))
    }
}
