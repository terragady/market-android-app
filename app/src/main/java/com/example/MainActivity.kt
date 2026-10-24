package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StockQuote
import com.example.ui.MainViewModel
import com.example.ui.components.AddStockDialog
import com.example.ui.components.StockDetailHero
import com.example.ui.components.WatchlistSection
import com.example.ui.components.WidgetManagerSheet
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StockAccent
import com.example.ui.theme.StockGain
import com.example.ui.theme.StockGainBg
import com.example.ui.theme.StockLoss
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.widget.StockWidgetProvider

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleWidgetIntent(intent)

        setContent {
            MyApplicationTheme {
                StockPulseApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        val symbol = intent?.getStringExtra(StockWidgetProvider.EXTRA_SYMBOL)
        if (!symbol.isNullOrBlank()) {
            viewModel.selectStockBySymbol(symbol)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockPulseApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            StockTopAppBar(
                isLive = uiState.isLiveStreaming,
                isRefreshing = uiState.isRefreshing,
                onToggleLive = { viewModel.toggleLiveStreaming() },
                onRefresh = { viewModel.refreshQuotes() },
                onOpenWidgetSheet = { viewModel.setShowWidgetSheet(true) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddStockDialog(true) },
                containerColor = StockGain,
                contentColor = Color(0xFF0A0D14),
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_stock")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add stock to watchlist")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Spotlight: Currently selected stock with Day/Week chart
            item {
                val current = uiState.selectedStock
                if (current != null) {
                    StockDetailHero(
                        stock = current,
                        selectedTimeframe = uiState.selectedTimeframe,
                        onTimeframeChange = { viewModel.setTimeframe(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(current) },
                        onPinToWidget = { viewModel.pinToWidget(current) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StockGain)
                    }
                }
            }

            // Watchlist Section with Search, Sparklines, and Ticker tape
            item {
                WatchlistSection(
                    stocks = uiState.stocks,
                    selectedSymbol = uiState.selectedStock?.symbol ?: "",
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onSelectStock = { viewModel.selectStock(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPinToWidget = { viewModel.pinToWidget(it) },
                    onDeleteStock = { viewModel.deleteStock(it) },
                    onOpenAddDialog = { viewModel.setShowAddStockDialog(true) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Add Stock Modal Dialog
    if (uiState.showAddStockDialog) {
        AddStockDialog(
            onDismiss = { viewModel.setShowAddStockDialog(false) },
            onAddStock = { symbol, name, price ->
                viewModel.addStock(symbol, name, price)
            }
        )
    }

    // Home Screen Widget Manager Bottom Sheet
    if (uiState.showWidgetSheet) {
        WidgetManagerSheet(
            stocks = uiState.stocks,
            currentPinnedStock = uiState.stocks.find { it.isPinnedToWidget } ?: uiState.selectedStock,
            onSelectPinnedStock = { symbol ->
                val target = uiState.stocks.find { it.symbol == symbol }
                if (target != null) {
                    viewModel.pinToWidget(target)
                }
            },
            onDismiss = { viewModel.setShowWidgetSheet(false) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTopAppBar(
    isLive: Boolean,
    isRefreshing: Boolean,
    onToggleLive: () -> Unit,
    onRefresh: () -> Unit,
    onOpenWidgetSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    TopAppBar(
        modifier = modifier.testTag("stock_top_app_bar"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StockGainBg,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "StockPulse",
                            tint = StockGain,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "StockPulse",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Live Market & Widget",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        },
        actions = {
            // Live Tick Indicator / Switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isLive) StockGainBg else DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isLive) StockGain.copy(alpha = 0.6f) else DarkBorder
                ),
                modifier = Modifier
                    .clickable(onClick = onToggleLive)
                    .testTag("toggle_live_stream_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(if (isLive) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(if (isLive) StockGain else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isLive) "LIVE" else "PAUSED",
                        color = if (isLive) StockGain else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Widget Config Action
            IconButton(
                onClick = onOpenWidgetSheet,
                modifier = Modifier.testTag("open_widget_manager_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = "Configure Home Widget",
                    tint = StockAccent
                )
            }

            // Refresh action
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("manual_refresh_button")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = StockGain,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh market quotes",
                        tint = TextSecondary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground,
            titleContentColor = TextPrimary
        )
    )
}
