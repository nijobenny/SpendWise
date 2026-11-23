package com.nijobenny.spendwise

import android.content.Context
import com.nijobenny.spendwise.data.local.AndroidContextHolder
import com.nijobenny.spendwise.di.initKoin

/** Call once from the Android app before showing [App]. */
fun initSpendWise(context: Context) {
    AndroidContextHolder.init(context)
    initKoin()
}
