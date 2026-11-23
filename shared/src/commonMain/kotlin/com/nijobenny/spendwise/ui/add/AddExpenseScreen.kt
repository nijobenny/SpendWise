package com.nijobenny.spendwise.ui.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.spendwise.domain.model.Category
import com.nijobenny.spendwise.domain.usecase.ExpenseValidator
import com.nijobenny.spendwise.ui.AddExpenseUiState
import com.nijobenny.spendwise.ui.components.GradientButton
import com.nijobenny.spendwise.ui.components.bounceClick
import com.nijobenny.spendwise.ui.currencySymbol
import com.nijobenny.spendwise.ui.theme.SpendColors
import com.nijobenny.spendwise.ui.theme.color
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    state: AddExpenseUiState,
    onAmountChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onCategorySelect: (Category) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    // Shake the form whenever a save attempt fails validation.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.errorTrigger) {
        if (state.errorTrigger > 0) {
            for (x in listOf(-20f, 18f, -14f, 10f, -6f, 0f)) shake.animateTo(x, tween(45))
        }
    }

    // Keep the last message visible while the error banner animates out.
    var lastError by remember { mutableStateOf(state.error) }
    LaunchedEffect(state.error) { if (state.error != null) lastError = state.error }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        TopBar(title = if (state.isEditing) "Edit expense" else "New expense", onClose = {
            focusManager.clearFocus()
            onClose()
        })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            AmountInput(
                value = state.amount,
                currencyCode = state.currencyCode,
                onValueChange = onAmountChange,
                hasError = state.error == ExpenseValidator.Error.INVALID_AMOUNT ||
                    state.error == ExpenseValidator.Error.AMOUNT_NOT_POSITIVE,
                modifier = Modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
            )

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ErrorBanner(errorMessage(state.error ?: lastError))
            }

            SectionLabel("What was it for?")
            PremiumTextField(
                value = state.title,
                onValueChange = onTitleChange,
                placeholder = "e.g. Dinner with friends",
                isError = state.error == ExpenseValidator.Error.EMPTY_TITLE ||
                    state.error == ExpenseValidator.Error.TITLE_TOO_LONG,
                singleLine = true,
            )

            SectionLabel("Category")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Category.entries.forEach { category ->
                    CategoryChip(
                        category = category,
                        selected = category == state.category,
                        onClick = { onCategorySelect(category) },
                    )
                }
            }

            SectionLabel("Note (optional)")
            PremiumTextField(
                value = state.note,
                onValueChange = onNoteChange,
                placeholder = "Add a short note",
                isError = false,
                singleLine = false,
            )
            Spacer(Modifier.height(24.dp))
        }

        GradientButton(
            text = if (state.isEditing) "Save changes" else "Save expense",
            loading = state.isSaving,
            onClick = {
                focusManager.clearFocus()
                onSave()
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

private fun errorMessage(error: ExpenseValidator.Error?): String = when (error) {
    ExpenseValidator.Error.EMPTY_TITLE -> "Give this expense a name"
    ExpenseValidator.Error.TITLE_TOO_LONG -> "Keep the name under ${ExpenseValidator.MAX_TITLE_LENGTH} characters"
    ExpenseValidator.Error.INVALID_AMOUNT -> "Enter a valid amount"
    ExpenseValidator.Error.AMOUNT_NOT_POSITIVE -> "Amount must be more than zero"
    null -> ""
}

@Composable
private fun TopBar(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SpendColors.SurfaceHigh)
                .bounceClick(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = SpendColors.TextPrimary, fontSize = 16.sp)
        }
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = SpendColors.TextPrimary)
    }
}

@Composable
private fun AmountInput(
    value: String,
    currencyCode: String,
    onValueChange: (String) -> Unit,
    hasError: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(400) // wait for the screen's enter animation
        focusRequester.requestFocus()
    }
    val accent by animateColorAsState(
        targetValue = if (hasError) SpendColors.Rose else SpendColors.Violet,
        animationSpec = tween(250),
        label = "amountAccent",
    )
    val amountStyle = TextStyle(fontSize = 54.sp, fontWeight = FontWeight.Bold, color = SpendColors.TextPrimary)

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Amount", style = MaterialTheme.typography.labelLarge, color = SpendColors.TextMuted)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(currencySymbol(currencyCode), fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = accent)
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = amountStyle,
                cursorBrush = SolidColor(SpendColors.Cyan),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .widthIn(min = 40.dp, max = 280.dp)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text("0", style = amountStyle.copy(color = SpendColors.TextMuted.copy(alpha = 0.5f)))
                        }
                        innerTextField()
                    }
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .width(120.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.7f))
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SpendColors.Rose.copy(alpha = 0.12f))
            .border(1.dp, SpendColors.Rose.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⚠️", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(message, style = MaterialTheme.typography.labelLarge, color = SpendColors.Rose)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = SpendColors.TextMuted,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    singleLine: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SpendColors.TextPrimary,
            unfocusedTextColor = SpendColors.TextPrimary,
            focusedContainerColor = SpendColors.Surface,
            unfocusedContainerColor = SpendColors.Surface,
            errorContainerColor = SpendColors.Surface,
            focusedBorderColor = SpendColors.Violet,
            unfocusedBorderColor = SpendColors.Outline,
            errorBorderColor = SpendColors.Rose,
            cursorColor = SpendColors.Cyan,
            focusedPlaceholderColor = SpendColors.TextMuted,
            unfocusedPlaceholderColor = SpendColors.TextMuted,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val background by animateColorAsState(
        targetValue = if (selected) category.color.copy(alpha = 0.22f) else SpendColors.SurfaceHigh,
        animationSpec = tween(250),
        label = "chipBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) category.color else Color.Transparent,
        animationSpec = tween(250),
        label = "chipBorder",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) SpendColors.TextPrimary else SpendColors.TextMuted,
        animationSpec = tween(250),
        label = "chipText",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "chipScale",
    )
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(background)
            .border(1.5.dp, border, shape)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(category.emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(category.displayName, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}
