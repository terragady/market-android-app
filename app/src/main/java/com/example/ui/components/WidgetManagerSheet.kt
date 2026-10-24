package com.example.ui.components

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.widget.StockWidgetProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetManagerSheet(
    stocks: List<StockQuote>,
    currentPinnedStock: StockQuote?,
    onSelectPinnedStock: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(StockWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE) }

    var showGraphs by remember { mutableStateOf(prefs.getBoolean(StockWidgetProvider.PREF_SHOW_GRAPHS, true)) }
    var onlyFavorites by remember { mutableStateOf(prefs.getBoolean(StockWidgetProvider.PREF_FILTER_FAVORITES, false)) }
    var selectedTimeframe by remember {
        val tf = prefs.getString(StockWidgetProvider.PREF_TIMEFRAME, Timeframe.DAY.name) ?: Timeframe.DAY.name
        mutableStateOf(if (tf == Timeframe.WEEK.name) Timeframe.WEEK else Timeframe.DAY)
    }
    var showApiExplanation by remember { mutableStateOf(false) }

    fun updatePrefsAndWidget() {
        prefs.edit()
            .putBoolean(StockWidgetProvider.PREF_SHOW_GRAPHS, showGraphs)
            .putBoolean(StockWidgetProvider.PREF_FILTER_FAVORITES, onlyFavorites)
            .putString(StockWidgetProvider.PREF_TIMEFRAME, selectedTimeframe.name)
            .apply()
        StockWidgetProvider.updateAllWidgets(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .testTag("widget_manager_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = StockAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Multi-Stock Widget",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "StockPulse's home screen widget displays multiple stocks in a compact, scrollable collection. You can resize it on your home screen to 2x2, 4x2, 4x3, or larger grid sizes.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Multi-Stock Live Preview Card
            Text(
                text = "LIVE MULTI-STOCK WIDGET PREVIEW",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val displayStocks = if (onlyFavorites) {
                val favs = stocks.filter { it.isFavorite }
                if (favs.isNotEmpty()) favs else stocks
            } else {
                stocks
            }

            MultiStockWidgetPreview(
                stocks = displayStocks.take(4),
                showGraphs = showGraphs,
                timeframe = selectedTimeframe,
                onlyFavorites = onlyFavorites
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Widget Customization Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Option 1: Toggle Sparkline Graphs ON/OFF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Mini Sparkline Graphs", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                            Text(
                                text = if (showGraphs) "Visual price curves enabled" else "Ultra-compact price-only mode",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = showGraphs,
                            onCheckedChange = {
                                showGraphs = it
                                updatePrefsAndWidget()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StockAccent,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: Filter Favorites Only
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Filter: Starred Favorites Only", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                            Text(
                                text = if (onlyFavorites) "Showing only your starred stocks" else "Showing all watchlist stocks",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = onlyFavorites,
                            onCheckedChange = {
                                onlyFavorites = it
                                updatePrefsAndWidget()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFFD700),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 3: Timeframe 1D vs 1W
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Graph Timeframe", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                            Text("1D Intraday vs 1W Past 7 Days", color = TextMuted, fontSize = 11.sp)
                        }
                        Row {
                            Timeframe.values().forEach { tf ->
                                val isSel = tf == selectedTimeframe
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) StockGain else Color.Transparent)
                                        .clickable {
                                            selectedTimeframe = tf
                                            updatePrefsAndWidget()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tf.label,
                                        color = if (isSel) Color(0xFF0A0D14) else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Expandable "How We Fetch Market Prices" Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showApiExplanation = !showApiExplanation },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = StockGain,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How Market Prices Are Fetched",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Icon(
                            imageVector = if (showApiExplanation) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }

                    AnimatedVisibility(visible = showApiExplanation) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "1. Live Market API Integration: StockPulse queries public financial market endpoints (Yahoo Finance v8 chart API) for real-time equity quotes, previous close, volume, high/low, and 15-minute intraday tick points.\n\n" +
                                       "2. Resilient Fallback Engine: When offline, on network throttling, or during weekends/market holidays, the app uses an on-device Room database cache with Brownian drift simulation so your charts and widgets never show blank or broken errors.\n\n" +
                                       "3. Live Ticker Updates: When 'LIVE' is enabled in the top app bar, active stocks receive continuous simulated tick streaming every 4 seconds.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // One-tap pin widget to home screen (Android 8.0+)
                Button(
                    onClick = {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, StockWidgetProvider::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                            appWidgetManager.requestPinAppWidget(provider, null, null)
                            Toast.makeText(context, "Adding widget to home screen...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Long-press your home screen and choose 'StockPulse' to add widget",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pin_widget_to_home_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StockAccent, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pin to Home Screen", fontWeight = FontWeight.Bold)
                }

                // Force Sync Widget
                OutlinedButton(
                    onClick = {
                        StockWidgetProvider.updateAllWidgets(context)
                        Toast.makeText(context, "Widget refreshed", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Tip: You can customize the widget anytime right here or long-press the widget on your home screen to reconfigure!",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun MultiStockWidgetPreview(
    stocks: List<StockQuote>,
    showGraphs: Boolean,
    timeframe: Timeframe,
    onlyFavorites: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263147))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Clean multi-stock list preview without headers
            stocks.forEach { stock ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    if (showGraphs) {
                        MiniSparkline(
                            points = if (timeframe == Timeframe.DAY) stock.dayPoints else stock.weekPoints,
                            baselinePrice = if (timeframe == Timeframe.DAY) stock.previousClose else stock.weekStartPrice,
                            isPositive = if (timeframe == Timeframe.DAY) stock.isPositive else stock.weekChange >= 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stock.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(stock.name, color = Color(0xFF9EABB8), fontSize = 9.sp, maxLines = 1)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(stock.formattedPrice(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(1.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (stock.isPositive) Color(0xFF133827) else Color(0xFF3D181D),
                                        RoundedCornerShape(3.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (timeframe == Timeframe.DAY) stock.formattedChangePercent() else stock.formattedWeekChangePercent(),
                                    color = if (stock.isPositive) Color(0xFF00E676) else Color(0xFFFF5252),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Updated 10:45", color = Color(0xFF5A6677), fontSize = 9.sp)
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF5A6677),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
