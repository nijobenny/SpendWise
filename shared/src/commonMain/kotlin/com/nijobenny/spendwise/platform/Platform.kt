package com.nijobenny.spendwise.platform

import androidx.compose.runtime.Composable

/** Current time in epoch milliseconds. */
expect fun currentTimeMillis(): Long

/** System back gesture/button handling (Android). No-op on iOS. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
