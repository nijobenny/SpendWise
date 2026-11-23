package com.nijobenny.spendwise

import androidx.compose.ui.window.ComposeUIViewController
import com.nijobenny.spendwise.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
