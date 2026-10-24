package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PricePoint
import com.example.model.StockQuote
import com.example.model.Timeframe
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StockAccent
import com.example.ui.theme.StockGain
import com.example.ui.theme.StockGainBg
import com.example.ui.theme.StockLoss
import com.example.ui.theme.StockLossBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun StockDetailHero(
    stock: StockQuote,
    selectedTimeframe: Timeframe,
    onTimeframeChange: (Timeframe) -> Unit,
    onToggleFavorite: () -> Unit,
    onPinToWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scrubbedPoint by remember { mutableStateOf<PricePoint?>(null) }

    val isDay = selectedTimeframe == Timeframe.DAY
    val points = if (isDay) stock.dayPoints else stock.weekPoints
    val baseline = if (isDay) stock.previousClose else stock.weekStartPrice

    val displayPrice = scrubbedPoint?.price ?: stock.currentPrice
    val changeFromBaseline = displayPrice - baseline
    val percentFromBaseline = if (baseline > 0) (changeFromBaseline / baseline) * 100.0 else 0.0
    val isPositive = changeFromBaseline >= 0

    val accentColor by animateColorAsState(if (isPositive) StockGain else StockLoss, label = "accentColor")
    val badgeBgColor by animateColorAsState(if (isPositive) StockGainBg else StockLossBg, label = "badgeBg")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stock_detail_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Symbol, Name, Widget Pin Badge, Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.symbol,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (stock.isPinnedToWidget) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StockAccent.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StockAccent.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = "Active in home widget",
                                        tint = StockAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "WIDGET PINNED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockAccent
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = stock.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Actions: Pin to Widget & Star
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPinToWidget,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("pin_widget_button")
                    ) {
                        Icon(
                            imageVector = if (stock.isPinnedToWidget) Icons.Default.Widgets else Icons.Outlined.Widgets,
                            contentDescription = "Pin to Home Widget",
                            tint = if (stock.isPinnedToWidget) StockAccent else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("favorite_button")
                    ) {
                        Icon(
                            imageVector = if (stock.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite Stock",
                            tint = if (stock.isFavorite) Color(0xFFFFD700) else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Price and Change Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = String.format(Locale.US, "$%.2f", displayPrice),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(badgeBgColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val sign = if (changeFromBaseline >= 0) "+" else ""
                            Text(
                                text = String.format(Locale.US, "%s%.2f%% (%s$%.2f)", sign, percentFromBaseline, sign, changeFromBaseline),
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (scrubbedPoint != null) "at ${scrubbedPoint!!.formattedTime()}" else if (isDay) "Today" else "Past 7D",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                // Timeframe Selector: 1D | 1W
                TimeframeToggle(
                    selectedTimeframe = selectedTimeframe,
                    onSelect = onTimeframeChange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Chart
            InteractiveStockChart(
                points = points,
                baselinePrice = baseline,
                isPositive = isPositive,
                timeframe = selectedTimeframe,
                onScrubPoint = { point -> scrubbedPoint = point }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day's Range Slider Bar
            DaysRangeSlider(
                currentPrice = stock.currentPrice,
                dayLow = stock.dayLow,
                dayHigh = stock.dayHigh
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fundamental Key Stats Grid
            KeyStatsGrid(stock = stock)
        }
    }
}

@Composable
fun TimeframeToggle(
    selectedTimeframe: Timeframe,
    onSelect: (Timeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("timeframe_toggle"),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            Timeframe.values().forEach { tf ->
                val isSelected = tf == selectedTimeframe
                val bg = if (isSelected) StockGain else Color.Transparent
                val textColor = if (isSelected) Color(0xFF0A0D14) else TextSecondary

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(bg)
                        .clickable { onSelect(tf) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tf.label,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DaysRangeSlider(
    currentPrice: Double,
    dayLow: Double,
    dayHigh: Double,
    modifier: Modifier = Modifier
) {
    val totalRange = max(0.01, dayHigh - dayLow)
    val fraction = ((currentPrice - dayLow) / totalRange).toFloat().coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Day's Range", color = TextSecondary, fontSize = 11.sp)
            Text(
                text = String.format(Locale.US, "$%.2f - $%.2f", dayLow, dayHigh),
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DarkSurfaceVariant)
        ) {
            // Highlighted progress
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(StockGain.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
fun KeyStatsGrid(stock: StockQuote, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(label = "Open", value = String.format(Locale.US, "$%.2f", stock.dayOpen), modifier = Modifier.weight(1f))
            StatItem(label = "Volume", value = stock.formattedVolume(), modifier = Modifier.weight(1f))
            StatItem(label = "Market Cap", value = stock.formattedMarketCap(), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(label = "52W High", value = String.format(Locale.US, "$%.2f", stock.week52High), modifier = Modifier.weight(1f))
            StatItem(label = "52W Low", value = String.format(Locale.US, "$%.2f", stock.week52Low), modifier = Modifier.weight(1f))
            StatItem(label = "P/E Ratio", value = stock.peRatio?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
