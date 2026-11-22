package com.nijobenny.spendwise.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.spendwise.domain.model.Money
import com.nijobenny.spendwise.ui.formatMoney
import com.nijobenny.spendwise.ui.theme.SpendColors
import kotlin.math.roundToLong

/** Springy "press-in" effect used on every tappable element for a tactile, premium feel. */
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "bounce",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/** Money that counts up from 0 to its value whenever it changes. */
@Composable
fun AnimatedAmount(
    money: Money,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val target = money.amountMinor.toFloat()
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animatable.animateTo(target, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }
    val shown = if (animatable.value == target) money.amountMinor else animatable.value.roundToLong()
    Text(text = formatMoney(shown, money.currencyCode), style = style, color = color, modifier = modifier)
}

/** Animated donut chart. Each slice sweeps in on first display and when data changes. */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit = {},
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1200, easing = FastOutSlowInEasing))
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.14f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            drawArc(
                color = SpendColors.SurfaceHigh,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            val total = slices.sumOf { it.second.toDouble() }.toFloat()
            if (total <= 0f) return@Canvas
            val gap = if (slices.size > 1) 3f else 0f
            var start = -90f
            slices.forEach { (color, value) ->
                val full = 360f * (value / total) * progress.value
                val sweep = (full - gap).coerceAtLeast(0f)
                if (sweep > 0f) {
                    drawArc(
                        color = color,
                        startAngle = start + gap / 2,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )
                }
                start += full
            }
        }
        centerContent()
    }
}

/** Full-width gradient call-to-action with a glow and loading state. */
@Composable
fun GradientButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(elevation = 18.dp, shape = shape, ambientColor = SpendColors.Violet, spotColor = SpendColors.Violet)
            .clip(shape)
            .background(SpendColors.ButtonGradient)
            .bounceClick(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "buttonContent",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
            } else {
                Text(text = text, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}

/** Floating gradient "+" button. Shows a pulsing ring to invite the first tap. */
@Composable
fun GradientFab(onClick: () -> Unit, pulse: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fab")
    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label = "ringScale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label = "ringAlpha",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (pulse) {
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .background(SpendColors.Violet, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(elevation = 20.dp, shape = CircleShape, ambientColor = SpendColors.Violet, spotColor = SpendColors.Violet)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(SpendColors.Violet, SpendColors.Cyan)))
                .bounceClick(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
        }
    }
}
