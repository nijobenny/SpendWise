package com.nijobenny.spendwise.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
