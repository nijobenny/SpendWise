package com.nijobenny.spendwise.ui.splash

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.spendwise.ui.theme.SpendColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated intro shown once per launch, right after the system splash.
 * It starts with the logo exactly where the system splash drew it, so the hand-over is seamless:
 * the logo lifts up, a light sweep glides across it, and the wordmark rises in.
 */
@Composable
fun SplashIntro(onFinished: () -> Unit) {
    val logoSize = remember { Animatable(128f) }   // same size as the system splash icon
    val logoLift = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val shimmer = remember { Animatable(-0.6f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleRise = remember { Animatable(24f) }
    val taglineAlpha = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            delay(120)
            launch { logoSize.animateTo(96f, spring(dampingRatio = 0.6f, stiffness = 180f)) }
            launch { logoLift.animateTo(-60f, spring(dampingRatio = 0.7f, stiffness = 160f)) }
            glow.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            delay(380)
            launch { titleAlpha.animateTo(1f, tween(450)) }
            titleRise.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
        }
        launch {
            delay(560)
            taglineAlpha.animateTo(1f, tween(450))
        }
        launch {
            delay(650)
            shimmer.animateTo(1.6f, tween(900, easing = LinearEasing))
        }
        launch {
            delay(700)
            footerAlpha.animateTo(1f, tween(500))
        }
        delay(1_750)
        onFinished()
    }

    val breathing = rememberInfiniteTransition(label = "breathing")
    val glowScale by breathing.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        // Soft violet/cyan aura behind the logo
        Canvas(
            modifier = Modifier
                .size(320.dp)
                .offset(y = logoLift.value.dp)
                .graphicsLayer {
                    alpha = glow.value
                    scaleX = glowScale
                    scaleY = glowScale
                },
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(SpendColors.Violet.copy(alpha = 0.45f), SpendColors.Cyan.copy(alpha = 0.12f), Color.Transparent),
                ),
            )
        }

        SpendWiseLogo(
            shimmerProgress = shimmer.value,
            modifier = Modifier
                .offset(y = logoLift.value.dp)
                .size(logoSize.value.dp),
        )

        Column(
            modifier = Modifier.offset(y = 62.dp + titleRise.value.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "SpendWise",
                color = SpendColors.TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Track smarter. Spend wiser.",
                style = MaterialTheme.typography.bodyLarge,
                color = SpendColors.TextMuted,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value },
            )
        }

        Text(
            "Built with Kotlin Multiplatform",
            style = MaterialTheme.typography.labelMedium,
            color = SpendColors.TextMuted.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = footerAlpha.value },
        )
    }
}

/**
 * The app icon drawn in code (same geometry as the launcher icon), so it can be animated.
 * Coordinates use the icon's 72-unit visible area. [shimmerProgress] moves a light band across it.
 */
@Composable
fun SpendWiseLogo(shimmerProgress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val k = size.width / 72f
        fun p(x: Float, y: Float) = Offset((x - 18f) * k, (y - 18f) * k)

        val corner = CornerRadius(20f * k, 20f * k)
        val shape = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, corner))
        }

        clipPath(shape) {
            drawRect(
                brush = Brush.linearGradient(
                    listOf(SpendColors.Violet, SpendColors.Indigo, SpendColors.Cyan),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                    center = p(30f, 22f),
                    radius = 60f * k,
                ),
            )

            // Segmented donut
            val ringRadius = 20f * k
            val stroke = Stroke(width = 7.5f * k, cap = StrokeCap.Round)
            val topLeft = p(54f - 20f, 54f - 20f)
            val ringSize = Size(ringRadius * 2, ringRadius * 2)
            var start = -75f
            listOf(125f to 1f, 85f to 0.72f, 60f to 0.45f).forEach { (sweep, alpha) ->
                drawArc(
                    color = Color.White.copy(alpha = alpha),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = ringSize,
                    style = stroke,
                )
                start += sweep + 30f
            }

            // Rising trend line + end dot
            val trend = Path().apply {
                val a = p(43f, 61f)
                moveTo(a.x, a.y)
                listOf(p(49.5f, 53.5f), p(55f, 58f), p(62.5f, 48.5f)).forEach { lineTo(it.x, it.y) }
            }
            drawPath(trend, Color.White, style = Stroke(width = 4.5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(Color.White, radius = 4.2f * k, center = p(63.2f, 47.6f))

            // Diagonal light sweep
            val x = shimmerProgress * size.width
            drawRect(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                    start = Offset(x - size.width * 0.35f, 0f),
                    end = Offset(x, size.height),
                ),
            )
        }
    }
}
