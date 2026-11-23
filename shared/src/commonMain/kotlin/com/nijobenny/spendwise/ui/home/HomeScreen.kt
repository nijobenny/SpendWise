package com.nijobenny.spendwise.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxHeight
import com.nijobenny.spendwise.domain.model.MonthKey
import com.nijobenny.spendwise.domain.usecase.MonthTotal
import com.nijobenny.spendwise.domain.usecase.MonthlyInsights
import com.nijobenny.spendwise.ui.formatCompact
import com.nijobenny.spendwise.ui.monthName
import com.nijobenny.spendwise.ui.shortMonthName
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
import com.nijobenny.spendwise.ui.RatesUiState
import com.nijobenny.spendwise.domain.model.Money
import com.nijobenny.spendwise.domain.usecase.GetExchangeRatesUseCase
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
    ratesState: RatesUiState,
    onRetryRates: () -> Unit,
    onAddClick: () -> Unit,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
    onLoadSample: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (MonthKey) -> Unit,
) {
    val insights = state.insights
    val expenses = insights.expenses
    val now = remember(expenses) { currentTimeMillis() }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AmbientGlow()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset + 16.dp, bottom = 170.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") { Header() }
            item(key = "month") {
                MonthSwitcher(
                    month = insights.month,
                    canGoNext = state.canGoToNextMonth,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                )
            }
            item(key = "balance") { BalanceCard(insights) }
            if (insights.hasAnyExpenses) {
                item(key = "trend") {
                    TrendCard(insights.trend, selected = insights.month, onSelect = onSelectMonth, modifier = Modifier.animateItem())
                }
            }
            if (insights.summary.count > 0) {
                item(key = "currencies") {
                    CurrencyCard(insights.summary.total, insights.month, ratesState, onRetryRates, Modifier.animateItem())
                }
            }
            if (insights.summary.count > 0) {
                item(key = "breakdown") { BreakdownCard(insights.summary, Modifier.animateItem()) }
            }
            item(key = "recentTitle") {
                SectionTitle(
                    title = "Transactions",
                    trailing = if (expenses.isNotEmpty()) "Tap to edit · swipe to delete" else null,
                )
            }
            if (!state.isLoading && !insights.hasAnyExpenses) {
                item(key = "empty") { EmptyState(onLoadSample, Modifier.animateItem()) }
            } else if (!state.isLoading && expenses.isEmpty()) {
                item(key = "emptyMonth") {
                    Text(
                        "No expenses in ${monthName(insights.month.month)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpendColors.TextMuted,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).animateItem(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(expenses, key = { it.id }) { expense ->
                SwipeToDeleteRow(
                    expense = expense,
                    nowMillis = now,
                    onDelete = { onDelete(expense) },
                    onClick = { onEdit(expense) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        // Soft fade so list content never collides with the floating button.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, SpendColors.Background.copy(alpha = 0.92f), SpendColors.Background)
                    )
                ),
        )

        GradientFab(
            onClick = onAddClick,
            pulse = !insights.hasAnyExpenses && !state.isLoading,
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
private fun BalanceCard(insights: MonthlyInsights) {
    val summary = insights.summary
    val transition = rememberInfiniteTransition(label = "card")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    val topCategory = summary.byCategory.keys.firstOrNull()
    val change = insights.changeVsPreviousPercent

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
            Text(
                "Spent in ${monthName(insights.month.month)}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(6.dp))
            AnimatedAmount(
                money = summary.total,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("${summary.count} ${if (summary.count == 1) "expense" else "expenses"}")
                if (change != null) {
                    val arrow = if (change > 0) "▲" else if (change < 0) "▼" else "•"
                    GlassPill("$arrow ${kotlin.math.abs(change)}% vs ${shortMonthName(insights.month.previous().month)}")
                } else if (topCategory != null) {
                    GlassPill("Top: ${topCategory.emoji} ${topCategory.displayName}")
                }
            }
        }
    }
}

/** "‹  November 2026  ›" with a directional slide when the month changes. */
@Composable
private fun MonthSwitcher(
    month: MonthKey,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton("‹", enabled = true, onClick = onPrevious)
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(300)) { if (forward) it / 2 else -it / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { if (forward) -it / 2 else it / 2 } + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
            label = "month",
        ) { shown ->
            Text(
                "${monthName(shown.month)} ${shown.year}",
                style = MaterialTheme.typography.titleMedium,
                color = SpendColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        RoundIconButton("›", enabled = canGoNext, onClick = onNext)
    }
}

@Composable
private fun RoundIconButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.3f }
            .clip(CircleShape)
            .background(SpendColors.SurfaceHigh)
            .bounceClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 22.sp, color = SpendColors.TextPrimary)
    }
}

/** Animated bar chart of the last 6 months. Tap a bar to jump to that month. */
@Composable
private fun TrendCard(
    trend: List<MonthTotal>,
    selected: MonthKey,
    onSelect: (MonthKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val max = trend.maxOfOrNull { it.total.amountMinor }?.coerceAtLeast(1) ?: 1
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SpendColors.Surface)
            .border(1.dp, SpendColors.Outline, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text("Last 6 months", style = MaterialTheme.typography.titleMedium, color = SpendColors.TextPrimary)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            trend.forEach { item ->
                val isSelected = item.month == selected
                val fraction by animateFloatAsState(
                    targetValue = item.total.amountMinor.toFloat() / max,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 120f),
                    label = "bar",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bounceClick { onSelect(item.month) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    // Label slot is always reserved so every bar shares the same baseline height.
                    Text(
                        formatCompact(item.total.amountMinor, item.total.currencyCode),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected && item.total.amountMinor > 0) SpendColors.TextPrimary else Color.Transparent,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .fillMaxHeight((fraction * 0.82f).coerceAtLeast(0.03f))
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(listOf(SpendColors.Cyan, SpendColors.Violet))
                                } else {
                                    Brush.verticalGradient(listOf(SpendColors.Violet.copy(alpha = 0.45f), SpendColors.Violet.copy(alpha = 0.15f)))
                                }
                            ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        shortMonthName(item.month.month),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) SpendColors.TextPrimary else SpendColors.TextMuted,
                    )
                }
            }
        }
    }
}

/** Month total converted live into the currencies of the countries I'm moving to. */
@Composable
private fun CurrencyCard(
    total: Money,
    month: MonthKey,
    ratesState: RatesUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SpendColors.Surface)
            .border(1.dp, SpendColors.Outline, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${monthName(month.month)} in other currencies",
                style = MaterialTheme.typography.titleMedium,
                color = SpendColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (ratesState is RatesUiState.Ready) {
                Text(
                    "ECB · ${shortDate(ratesState.rates.date)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SpendColors.TextMuted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        AnimatedContent(
            targetState = ratesState,
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "rates",
        ) { state ->
            when (state) {
                is RatesUiState.Loading -> CurrencyGrid { code -> CurrencyTile(code, value = null) }
                is RatesUiState.Ready -> CurrencyGrid { code -> CurrencyTile(code, value = state.rates.convert(total, code)) }
                is RatesUiState.Error -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SpendColors.SurfaceHigh)
                        .bounceClick(onClick = onRetry)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📡", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Couldn't load exchange rates. Tap to retry.",
                        style = MaterialTheme.typography.labelLarge,
                        color = SpendColors.TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyGrid(tile: @Composable (String) -> Unit) {
    val codes = GetExchangeRatesUseCase.DEFAULT_TARGETS
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        codes.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { code ->
                    Box(modifier = Modifier.weight(1f)) { tile(code) }
                }
            }
        }
    }
}

private fun flagFor(code: String): String = when (code) {
    "EUR" -> "🇪🇺"
    "CHF" -> "🇨🇭"
    "GBP" -> "🇬🇧"
    "USD" -> "🇺🇸"
    else -> "🏳️"
}

/** A currency tile. Shows a pulsing placeholder while [value] is loading. */
@Composable
private fun CurrencyTile(code: String, value: Money?) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpendColors.SurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(flagFor(code), fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(code, style = MaterialTheme.typography.labelMedium, color = SpendColors.TextMuted)
        }
        Spacer(Modifier.height(6.dp))
        if (value != null) {
            AnimatedAmount(
                money = value,
                style = MaterialTheme.typography.titleMedium,
                color = SpendColors.TextPrimary,
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(18.dp)
                    .graphicsLayer { alpha = shimmer }
                    .clip(RoundedCornerShape(6.dp))
                    .background(SpendColors.Outline)
            )
        }
    }
}

/** "2026-11-23" -> "23 Nov". */
private fun shortDate(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val month = parts[1].toIntOrNull() ?: return isoDate
    if (month !in 1..12) return isoDate
    return "${parts[2].trimStart('0')} ${shortMonthName(month)}"
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
    onClick: () -> Unit,
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
                .bounceClick(onClick = onClick)
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
