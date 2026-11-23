package com.nijobenny.spendwise.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

/** Holds the application context needed to locate the database file on Android. */
object AndroidContextHolder {
    lateinit var context: Context
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}

actual fun createDatabaseBuilder(): RoomDatabase.Builder<SpendWiseDatabase> {
    val appContext = AndroidContextHolder.context
    val dbFile = appContext.getDatabasePath("spendwise.db")
    return Room.databaseBuilder<SpendWiseDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
