package com.nijobenny.spendwise.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.spendwise.domain.model.Expense
import com.nijobenny.spendwise.domain.usecase.ExpenseSummary
import com.nijobenny.spendwise.platform.currentTimeMillis
import com.nijobenny.spendwise.ui.HomeUiState
import com.nijobenny.spendwise.ui.components.AnimatedAmount
import com.nijobenny.spendwise.ui.components.DonutChart
import com.nijobenny.spendwise.ui.components.GradientFab
import com.nijobenny.spendwise.ui.components.bounceClick
import com.nijobenny.spendwise.ui.formatMoney
import com.nijobenny.spendwise.ui.relativeTime
import com.nijobenny.spendwise.ui.theme.SpendColors
import com.nijobenny.spendwise.ui.theme.color
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAddClick: () -> Unit,
    onDelete: (Expense) -> Unit,
    onLoadSample: () -> Unit,
) {
    val now = remember(state.expenses) { currentTimeMillis() }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AmbientGlow()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset + 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") { Header() }
            item(key = "balance") { BalanceCard(state.summary) }
            if (state.summary.count > 0) {
                item(key = "breakdown") { BreakdownCard(state.summary, Modifier.animateItem()) }
            }
            item(key = "recentTitle") {
                SectionTitle(
                    title = "Recent activity",
                    trailing = if (state.expenses.isNotEmpty()) "Swipe left to delete" else null,
                )
            }
            if (state.expenses.isEmpty() && !state.isLoading) {
                item(key = "empty") { EmptyState(onLoadSample, Modifier.animateItem()) }
            }
            items(state.expenses, key = { it.id }) { expense ->
                SwipeToDeleteRow(
                    expense = expense,
                    nowMillis = now,
                    onDelete = { onDelete(expense) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        GradientFab(
            onClick = onAddClick,
            pulse = state.expenses.isEmpty() && !state.isLoading,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp),
        )
    }
}

/** Soft, slowly drifting colour glows behind the content. */
@Composable
private fun AmbientGlow() {
    val transition = rememberInfiniteTransition(label = "glow")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c1 = Offset(w * (0.15f + 0.2f * drift), h * 0.08f)
        drawCircle(
            brush = Brush.radialGradient(listOf(SpendColors.Violet.copy(alpha = 0.28f), Color.Transparent), center = c1, radius = w * 0.75f),
            radius = w * 0.75f,
            center = c1,
        )
        val c2 = Offset(w * (0.95f - 0.2f * drift), h * 0.35f)
        drawCircle(
            brush = Brush.radialGradient(listOf(SpendColors.Cyan.copy(alpha = 0.14f), Color.Transparent), center = c2, radius = w * 0.6f),
            radius = w * 0.6f,
            center = c2,
        )
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Welcome back 👋", style = MaterialTheme.typography.labelLarge, color = SpendColors.TextMuted)
            Spacer(Modifier.height(2.dp))
            Text("SpendWise", style = MaterialTheme.typography.headlineMedium, color = SpendColors.TextPrimary)
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(SpendColors.PrimaryGradient)
                .padding(2.dp)
                .clip(CircleShape)
                .background(SpendColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Text("NB", style = MaterialTheme.typography.labelLarge, color = SpendColors.TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BalanceCard(summary: ExpenseSummary) {
    val transition = rememberInfiniteTransition(label = "card")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    val topCategory = summary.byCategory.keys.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(SpendColors.Violet, SpendColors.Indigo, SpendColors.Cyan),
                        start = Offset(-w * 0.3f + w * 0.3f * shift, 0f),
                        end = Offset(w * 1.3f - w * 0.3f * shift, h),
                    )
                )
                drawCircle(Color.White.copy(alpha = 0.10f), radius = h * 0.85f, center = Offset(w * (0.9f + 0.05f * shift), -h * 0.15f))
                drawCircle(Color.White.copy(alpha = 0.07f), radius = h * 0.55f, center = Offset(w * (0.05f + 0.1f * shift), h * 1.05f))
            }
            .padding(24.dp),
    ) {
        Column {
            Text("Total spent", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(6.dp))
            AnimatedAmount(
                money = summary.total,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("${summary.count} ${if (summary.count == 1) "expense" else "expenses"}")
                if (topCategory != null) GlassPill("Top: ${topCategory.emoji} ${topCategory.displayName}")
            }
        }
    }
}

@Composable
private fun GlassPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun BreakdownCard(summary: ExpenseSummary, modifier: Modifier = Modifier) {
    val entries = summary.byCategory.entries.toList()
    val total = summary.total.amountMinor.coerceAtLeast(1)
    val slices = remember(entries) { entries.map { it.key.color to it.value.amountMinor.toFloat() } }
    val top = entries.firstOrNull()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SpendColors.Surface)
            .border(1.dp, SpendColors.Outline, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DonutChart(slices = slices, modifier = Modifier.size(124.dp)) {
            if (top != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(top.value.amountMinor * 100 / total)}%", style = MaterialTheme.typography.titleLarge, color = SpendColors.TextPrimary)
                    Text(top.key.displayName, style = MaterialTheme.typography.labelMedium, color = SpendColors.TextMuted)
                }
            }
        }
        Spacer(Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            entries.take(4).forEach { (category, money) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(category.color))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        category.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = SpendColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${money.amountMinor * 100 / total}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = SpendColors.TextMuted,
                    )
                }
            }
            if (entries.size > 4) {
                Text("+${entries.size - 4} more", style = MaterialTheme.typography.labelMedium, color = SpendColors.TextMuted)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = SpendColors.TextPrimary, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = SpendColors.TextMuted)
        }
    }
}

@Composable
private fun EmptyState(onLoadSample: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "float")
    val dy by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy",
    )
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = dy.dp.toPx() }
                .size(96.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(SpendColors.Violet.copy(alpha = 0.35f), SpendColors.Cyan.copy(alpha = 0.2f))))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("💸", fontSize = 42.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("No expenses yet", style = MaterialTheme.typography.titleLarge, color = SpendColors.TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap + to add your first expense,\nor load sample data to explore.",
            style = MaterialTheme.typography.bodyMedium,
            color = SpendColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Load sample data",
            style = MaterialTheme.typography.labelLarge,
            color = SpendColors.TextPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SpendColors.SurfaceHigh)
                .border(1.dp, SpendColors.Violet.copy(alpha = 0.6f), RoundedCornerShape(50))
                .bounceClick(onClick = onLoadSample)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

/** Expense row with a custom swipe-left-to-delete gesture and spring snap-back. */
@Composable
private fun SwipeToDeleteRow(
    expense: Expense,
    nowMillis: Long,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 110.dp.toPx() }
    val maxDragPx = with(density) { 160.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(20.dp)
    val revealProgress = (-offsetX.value / thresholdPx).coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxWidth()) {
        // Red "delete" layer revealed behind the card
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(SpendColors.Rose.copy(alpha = 0.15f + 0.6f * revealProgress)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                "Delete",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .padding(end = 24.dp)
                    .graphicsLayer {
                        alpha = revealProgress
                        scaleX = 0.8f + 0.2f * revealProgress
                        scaleY = 0.8f + 0.2f * revealProgress
                    },
            )
        }

        ExpenseRow(
            expense = expense,
            nowMillis = nowMillis,
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-maxDragPx, 0f)) }
                    },
                    onDragStopped = {
                        if (offsetX.value < -thresholdPx) {
                            offsetX.animateTo(-maxDragPx * 3, tween(220))
                            onDelete()
                        } else {
                            offsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f))
                        }
                    },
                ),
        )
    }
}

@Composable
private fun ExpenseRow(expense: Expense, nowMillis: Long, modifier: Modifier = Modifier) {
    val categoryColor = expense.category.color
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SpendColors.Surface)
            .border(1.dp, SpendColors.Outline, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(categoryColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(expense.category.emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                expense.title,
                style = MaterialTheme.typography.titleMedium,
                color = SpendColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${expense.category.displayName} · ${relativeTime(expense.createdAtMillis, nowMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = SpendColors.TextMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "-" + formatMoney(expense.amount.amountMinor, expense.amount.currencyCode),
            style = MaterialTheme.typography.titleMedium,
            color = SpendColors.TextPrimary,
        )
    }
}
