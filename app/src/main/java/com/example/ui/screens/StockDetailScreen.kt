package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PricePoint
import com.example.model.StockQuote
import com.example.model.Timeframe
import com.example.ui.components.DaysRangeSlider
import com.example.ui.components.InteractiveStockChart
import com.example.ui.components.KeyStatsGrid
import com.example.ui.components.TimeframeToggle
import com.example.ui.theme.DarkBackground
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    stock: StockQuote,
    selectedTimeframe: Timeframe,
    onTimeframeChange: (Timeframe) -> Unit,
    onToggleFavorite: () -> Unit,
    onPinToWidget: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware and gesture back to return to watchlist
    BackHandler { onBack() }

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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("stock_detail_screen"),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stock.symbol,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            if (stock.isPinnedToWidget) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StockAccent.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StockAccent.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "WIDGET",
                                        color = StockAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = stock.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Watchlist",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onPinToWidget,
                        modifier = Modifier.testTag("detail_pin_widget_button")
                    ) {
                        Icon(
                            imageVector = if (stock.isPinnedToWidget) Icons.Default.Widgets else Icons.Outlined.Widgets,
                            contentDescription = "Pin to Home Widget",
                            tint = if (stock.isPinnedToWidget) StockAccent else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("detail_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (stock.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite Stock",
                            tint = if (stock.isFavorite) Color(0xFFFFD700) else TextSecondary
                        )
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh quote",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Main Large Price Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = String.format(Locale.US, "$%.2f", displayPrice),
                        fontSize = 38.sp,
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

                // Timeframe Selector Pill (1D vs 1W)
                TimeframeToggle(
                    selectedTimeframe = selectedTimeframe,
                    onSelect = onTimeframeChange
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Full Size Interactive Chart Card with scrubbing
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    InteractiveStockChart(
                        points = points,
                        baselinePrice = baseline,
                        isPositive = isPositive,
                        timeframe = selectedTimeframe,
                        onScrubPoint = { scrubbedPoint = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Day's Range Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DaysRangeSlider(
                        currentPrice = stock.currentPrice,
                        dayLow = stock.dayLow,
                        dayHigh = stock.dayHigh
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fundamental Key Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Key Statistics",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    KeyStatsGrid(stock = stock)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Pin to Widget & Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPinToWidget,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (stock.isPinnedToWidget) StockAccent else DarkSurfaceVariant,
                        contentColor = if (stock.isPinnedToWidget) Color.White else TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (stock.isPinnedToWidget) "Pinned to Widget" else "Pin to Widget")
                }

                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (stock.isFavorite) Color(0xFFFFD700) else DarkBorder
                    )
                ) {
                    Icon(
                        imageVector = if (stock.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (stock.isFavorite) Color(0xFFFFD700) else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (stock.isFavorite) "Favorited" else "Favorite",
                        color = if (stock.isFavorite) Color(0xFFFFD700) else TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
