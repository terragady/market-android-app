package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PricePoint
import com.example.model.StockQuote
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
fun WatchlistSection(
    stocks: List<StockQuote>,
    selectedSymbol: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectStock: (StockQuote) -> Unit,
    onToggleFavorite: (StockQuote) -> Unit,
    onPinToWidget: (StockQuote) -> Unit,
    onDeleteStock: (StockQuote) -> Unit,
    onOpenAddDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Market Indices ticker tape
        MarketIndexTape(stocks = stocks)

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stock_search_input"),
                placeholder = { Text("Search watchlist...", color = TextMuted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StockGain,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(10.dp))

            FilledTonalButton(
                onClick = onOpenAddDialog,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = StockGainBg,
                    contentColor = StockGain
                ),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("add_stock_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add stock",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section Title & Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Watchlist (${stocks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Tap to view chart",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stock list
        val filtered = if (searchQuery.isBlank()) stocks else {
            stocks.filter {
                it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching stocks found",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.forEach { stock ->
                    StockListItem(
                        stock = stock,
                        isSelected = stock.symbol == selectedSymbol,
                        onClick = { onSelectStock(stock) },
                        onToggleFavorite = { onToggleFavorite(stock) },
                        onPinToWidget = { onPinToWidget(stock) },
                        onDelete = { onDeleteStock(stock) },
                        canDelete = stocks.size > 1
                    )
                }
            }
        }
    }
}

@Composable
fun StockListItem(
    stock: StockQuote,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPinToWidget: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) StockGain.copy(alpha = 0.8f) else DarkBorder
    val bgColor = if (isSelected) DarkSurfaceVariant else DarkSurface
    val isPositive = stock.isPositive

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("stock_item_${stock.symbol}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ticker & Name
            Column(modifier = Modifier.width(88.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (stock.isPinnedToWidget) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Active widget",
                            tint = StockAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    fontSize = 11.sp
                )
            }

            // Mini Sparkline Graph
            MiniSparkline(
                points = stock.dayPoints,
                baselinePrice = stock.previousClose,
                isPositive = isPositive,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .padding(horizontal = 8.dp)
            )

            // Price & Change Badge
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = stock.formattedPrice(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isPositive) StockGainBg else StockLossBg,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stock.formattedChangePercent(),
                        color = if (isPositive) StockGain else StockLoss,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Pin to Widget Icon Button
            IconButton(
                onClick = onPinToWidget,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (stock.isPinnedToWidget) Icons.Default.Widgets else Icons.Outlined.Widgets,
                    contentDescription = "Pin to Home Widget",
                    tint = if (stock.isPinnedToWidget) StockAccent else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Favorite star
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (stock.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (stock.isFavorite) Color(0xFFFFD700) else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MiniSparkline(
    points: List<PricePoint>,
    baselinePrice: Double,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isPositive) StockGain else StockLoss
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val width = size.width
        val height = size.height

        val prices = points.map { it.price }
        val minPrice = min(baselinePrice, prices.minOrNull() ?: baselinePrice)
        val maxPrice = max(baselinePrice, prices.maxOrNull() ?: baselinePrice)
        val range = max(0.01, maxPrice - minPrice)

        fun getX(i: Int): Float = (i.toFloat() / (points.size - 1)) * width
        fun getY(price: Double): Float {
            val norm = (price - minPrice) / range
            return (height - (norm * height)).toFloat().coerceIn(2f, height - 2f)
        }

        val path = Path()
        path.moveTo(getX(0), getY(points[0].price))
        for (i in 1 until points.size) {
            val prevX = getX(i - 1)
            val prevY = getY(points[i - 1].price)
            val curX = getX(i)
            val curY = getY(points[i].price)
            val midX = (prevX + curX) / 2f
            path.cubicTo(midX, prevY, midX, curY, curX, curY)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MarketIndexTape(stocks: List<StockQuote>, modifier: Modifier = Modifier) {
    val indices = stocks.filter { it.symbol in listOf("SPY", "BTC-USD", "NVDA", "AAPL") }
    if (indices.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(indices) { stock ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stock.symbol,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stock.formattedPrice(),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stock.formattedChangePercent(),
                        color = if (stock.isPositive) StockGain else StockLoss,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
