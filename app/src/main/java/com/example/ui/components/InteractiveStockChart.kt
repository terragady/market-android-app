package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PricePoint
import com.example.model.Timeframe
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.StockGain
import com.example.ui.theme.StockLoss
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun InteractiveStockChart(
    points: List<PricePoint>,
    baselinePrice: Double,
    isPositive: Boolean,
    timeframe: Timeframe,
    modifier: Modifier = Modifier,
    onScrubPoint: ((PricePoint?) -> Unit)? = null
) {
    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading market points...", color = TextSecondary)
        }
        return
    }

    var scrubIndex by remember(points) { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableStateOf(0f) }

    val strokeColor = if (isPositive) StockGain else StockLoss
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            strokeColor.copy(alpha = 0.35f),
            strokeColor.copy(alpha = 0.08f),
            Color.Transparent
        )
    )

    val prices = remember(points) { points.map { it.price } }
    val minPrice = remember(prices, baselinePrice) { min(baselinePrice, prices.minOrNull() ?: baselinePrice) }
    val maxPrice = remember(prices, baselinePrice) { max(baselinePrice, prices.maxOrNull() ?: baselinePrice) }
    val priceRange = remember(minPrice, maxPrice) { max(0.01, maxPrice - minPrice) }

    val maxVolume = remember(points) { points.maxOfOrNull { it.volume } ?: 1L }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .testTag("interactive_stock_chart")
            .pointerInput(points) {
                detectTapGestures(
                    onPress = { offset ->
                        val index = ((offset.x / size.width) * (points.size - 1))
                            .roundToInt()
                            .coerceIn(0, points.size - 1)
                        scrubIndex = index
                        touchX = offset.x
                        onScrubPoint?.invoke(points[index])
                        tryAwaitRelease()
                        scrubIndex = null
                        onScrubPoint?.invoke(null)
                    }
                )
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = ((offset.x / size.width) * (points.size - 1))
                            .roundToInt()
                            .coerceIn(0, points.size - 1)
                        scrubIndex = index
                        touchX = offset.x
                        onScrubPoint?.invoke(points[index])
                    },
                    onDrag = { change, _ ->
                        touchX = change.position.x
                        val index = ((touchX / size.width) * (points.size - 1))
                            .roundToInt()
                            .coerceIn(0, points.size - 1)
                        scrubIndex = index
                        onScrubPoint?.invoke(points[index])
                    },
                    onDragEnd = {
                        scrubIndex = null
                        onScrubPoint?.invoke(null)
                    },
                    onDragCancel = {
                        scrubIndex = null
                        onScrubPoint?.invoke(null)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val chartHeight = height * 0.78f
            val volumeHeight = height * 0.18f
            val volumeTop = height - volumeHeight

            val paddingRatio = 0.08
            val effectiveMin = minPrice - priceRange * paddingRatio
            val effectiveMax = maxPrice + priceRange * paddingRatio
            val effectiveRange = effectiveMax - effectiveMin

            fun getX(index: Int): Float = (index.toFloat() / (points.size - 1)) * width
            fun getY(price: Double): Float {
                val norm = (price - effectiveMin) / effectiveRange
                return (chartHeight - (norm * chartHeight)).toFloat().coerceIn(4f, chartHeight)
            }

            // Draw baseline dashed line
            val baselineY = getY(baselinePrice)
            drawLine(
                color = DarkBorder,
                start = Offset(0f, baselineY),
                end = Offset(width, baselineY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )

            // Draw volume bars at the bottom
            val barWidth = (width / points.size) * 0.75f
            points.forEachIndexed { i, point ->
                val x = getX(i)
                val volRatio = if (maxVolume > 0) point.volume.toFloat() / maxVolume else 0f
                val barH = (volRatio * volumeHeight).coerceAtLeast(2f)
                val isPointGain = point.price >= baselinePrice
                val barColor = if (isPointGain) StockGain.copy(alpha = 0.25f) else StockLoss.copy(alpha = 0.25f)

                drawRect(
                    color = barColor,
                    topLeft = Offset(x - barWidth / 2f, height - barH),
                    size = androidx.compose.ui.geometry.Size(barWidth, barH)
                )
            }

            // Build smooth Bezier path
            val linePath = Path()
            val fillPath = Path()

            linePath.moveTo(getX(0), getY(points[0].price))
            fillPath.moveTo(getX(0), chartHeight)
            fillPath.lineTo(getX(0), getY(points[0].price))

            for (i in 1 until points.size) {
                val prevX = getX(i - 1)
                val prevY = getY(points[i - 1].price)
                val curX = getX(i)
                val curY = getY(points[i].price)

                val midX = (prevX + curX) / 2f
                linePath.cubicTo(midX, prevY, midX, curY, curX, curY)
                fillPath.cubicTo(midX, prevY, midX, curY, curX, curY)
            }

            fillPath.lineTo(getX(points.size - 1), chartHeight)
            fillPath.close()

            // Draw area gradient
            drawPath(path = fillPath, brush = gradientBrush)

            // Draw line stroke
            drawPath(
                path = linePath,
                color = strokeColor,
                style = Stroke(
                    width = 2.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw current active price dot if not scrubbing
            if (scrubIndex == null) {
                val lastX = getX(points.size - 1)
                val lastY = getY(points.last().price)
                drawCircle(color = strokeColor.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = Offset(lastX, lastY))
                drawCircle(color = strokeColor, radius = 4.5.dp.toPx(), center = Offset(lastX, lastY))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(lastX, lastY))
            }

            // Draw scrubber cursor
            scrubIndex?.let { index ->
                val x = getX(index)
                val y = getY(points[index].price)

                // Vertical cursor line
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Dot on cursor intersection
                drawCircle(color = strokeColor.copy(alpha = 0.45f), radius = 10.dp.toPx(), center = Offset(x, y))
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(color = strokeColor, radius = 3.dp.toPx(), center = Offset(x, y))
            }
        }

        // Floating scrubber tooltip pill
        scrubIndex?.let { index ->
            val point = points[index]
            val isScrubGain = point.price >= baselinePrice
            val pillColor = if (isScrubGain) StockGain else StockLoss

            val clampedX = touchX.coerceIn(80f, 800f) // approximate bounds
            Box(
                modifier = Modifier
                    .offset { IntOffset(touchX.toInt() - 90, 8) }
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "$%.2f", point.price),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (timeframe == Timeframe.DAY) point.formattedTime() else point.formattedDateTime(),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Subtle range labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.US, "Min: $%.2f", minPrice),
                color = TextMuted,
                fontSize = 10.sp
            )
            Text(
                text = if (timeframe == Timeframe.DAY) "Intraday Range" else "7-Day Range",
                color = TextMuted,
                fontSize = 10.sp
            )
            Text(
                text = String.format(Locale.US, "Max: $%.2f", maxPrice),
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
