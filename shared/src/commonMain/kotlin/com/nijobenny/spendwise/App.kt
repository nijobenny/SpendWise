package com.nijobenny.spendwise

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nijobenny.spendwise.di.AppContainer
import com.nijobenny.spendwise.platform.PlatformBackHandler
import com.nijobenny.spendwise.ui.ExpenseViewModel
import com.nijobenny.spendwise.ui.UiEvent
import com.nijobenny.spendwise.ui.add.AddExpenseScreen
import com.nijobenny.spendwise.ui.home.HomeScreen
import com.nijobenny.spendwise.ui.theme.SpendColors
import com.nijobenny.spendwise.ui.theme.SpendWiseTheme
import kotlinx.coroutines.launch

private enum class Screen { Home, Add }

@Composable
fun App() {
    SpendWiseTheme {
        val viewModel: ExpenseViewModel = viewModel { AppContainer.createExpenseViewModel() }
        val homeState by viewModel.homeState.collectAsStateWithLifecycle()
        val formState by viewModel.formState.collectAsStateWithLifecycle()

        var screen by remember { mutableStateOf(Screen.Home) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        fun showMessage(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) onAction()
            }
        }

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    UiEvent.ExpenseSaved -> {
                        screen = Screen.Home
                        showMessage("Expense added ✓")
                    }
                }
            }
        }

        val closeAddScreen = {
            viewModel.resetForm()
            screen = Screen.Home
        }
        PlatformBackHandler(enabled = screen == Screen.Add, onBack = closeAddScreen)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpendColors.Background),
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (targetState == Screen.Add) {
                        (slideInVertically(tween(450, easing = FastOutSlowInEasing)) { it / 4 } + fadeIn(tween(300))) togetherWith
                            (scaleOut(targetScale = 0.94f, animationSpec = tween(350)) + fadeOut(tween(250)))
                    } else {
                        (scaleIn(initialScale = 0.94f, animationSpec = tween(400)) + fadeIn(tween(300))) togetherWith
                            (slideOutVertically(tween(350, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(250)))
                    }
                },
                label = "screen",
            ) { target ->
                when (target) {
                    Screen.Home -> HomeScreen(
                        state = homeState,
                        onAddClick = { screen = Screen.Add },
                        onDelete = { expense ->
                            viewModel.delete(expense)
                            showMessage("Deleted \"${expense.title}\"", actionLabel = "Undo") {
                                viewModel.restore(expense)
                            }
                        },
                        onLoadSample = viewModel::loadSampleData,
                    )
                    Screen.Add -> AddExpenseScreen(
                        state = formState,
                        onAmountChange = viewModel::onAmountChange,
                        onTitleChange = viewModel::onTitleChange,
                        onCategorySelect = viewModel::onCategorySelect,
                        onNoteChange = viewModel::onNoteChange,
                        onSave = viewModel::save,
                        onClose = closeAddScreen,
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 100.dp),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = SpendColors.SurfaceHigh,
                    contentColor = SpendColors.TextPrimary,
                    actionColor = SpendColors.Cyan,
                )
            }
        }
    }
}
