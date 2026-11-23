package com.nijobenny.spendwise.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

@Database(entities = [ExpenseEntity::class], version = 1, exportSchema = false)
@ConstructedBy(SpendWiseDatabaseConstructor::class)
abstract class SpendWiseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}

// The Room compiler generates the `actual` implementations for each platform.
@Suppress("KotlinNoActualForExpect")
expect object SpendWiseDatabaseConstructor : RoomDatabaseConstructor<SpendWiseDatabase> {
    override fun initialize(): SpendWiseDatabase
}

/** Platform-specific: where the database file lives (Android app storage / iOS Documents). */
expect fun createDatabaseBuilder(): RoomDatabase.Builder<SpendWiseDatabase>

fun buildSpendWiseDatabase(): SpendWiseDatabase =
    createDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .build()
