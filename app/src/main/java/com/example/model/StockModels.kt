package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Timeframe(val label: String, val subtitle: String) {
    DAY("1D", "Today's Intraday"),
    WEEK("1W", "Past 7 Days")
}

data class PricePoint(
    val timestamp: Long,
    val price: Double,
    val volume: Long = 0L
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formattedDateTime(): String {
        val sdf = SimpleDateFormat("EEE, MMM d HH:mm", Locale.US)
        return sdf.format(Date(timestamp))
    }
}

data class StockQuote(
    val symbol: String,
    val name: String,
    val currency: String = "USD",
    val currentPrice: Double,
    val previousClose: Double,
    val dayOpen: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val volume: Long,
    val marketCap: Long,
    val peRatio: Double?,
    val week52High: Double,
    val week52Low: Double,
    val dayPoints: List<PricePoint>,
    val weekPoints: List<PricePoint>,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isPinnedToWidget: Boolean = false
) {
    val change: Double
        get() = currentPrice - previousClose

    val changePercent: Double
        get() = if (previousClose > 0.0) ((currentPrice - previousClose) / previousClose) * 100.0 else 0.0

    val isPositive: Boolean
        get() = change >= 0.0

    val weekStartPrice: Double
        get() = weekPoints.firstOrNull()?.price ?: previousClose

    val weekChange: Double
        get() = currentPrice - weekStartPrice

    val weekChangePercent: Double
        get() = if (weekStartPrice > 0.0) ((currentPrice - weekStartPrice) / weekStartPrice) * 100.0 else 0.0

    fun formattedPrice(): String = String.format(Locale.US, "$%.2f", currentPrice)

    fun formattedChange(): String {
        val sign = if (change >= 0) "+" else ""
        return String.format(Locale.US, "%s$%.2f", sign, change)
    }

    fun formattedChangePercent(): String {
        val sign = if (changePercent >= 0) "+" else ""
        return String.format(Locale.US, "%s%.2f%%", sign, changePercent)
    }

    fun formattedWeekChange(): String {
        val sign = if (weekChange >= 0) "+" else ""
        return String.format(Locale.US, "%s$%.2f", sign, weekChange)
    }

    fun formattedWeekChangePercent(): String {
        val sign = if (weekChangePercent >= 0) "+" else ""
        return String.format(Locale.US, "%s%.2f%%", sign, weekChangePercent)
    }

    fun formattedMarketCap(): String {
        return when {
            marketCap >= 1_000_000_000_000L -> String.format(Locale.US, "$%.2fT", marketCap / 1_000_000_000_000.0)
            marketCap >= 1_000_000_000L -> String.format(Locale.US, "$%.2fB", marketCap / 1_000_000_000.0)
            marketCap >= 1_000_000L -> String.format(Locale.US, "$%.2fM", marketCap / 1_000_000.0)
            else -> String.format(Locale.US, "$%,d", marketCap)
        }
    }

    fun formattedVolume(): String {
        return when {
            volume >= 1_000_000L -> String.format(Locale.US, "%.2fM", volume / 1_000_000.0)
            volume >= 1_000L -> String.format(Locale.US, "%.1fK", volume / 1_000.0)
            else -> "$volume"
        }
    }
}
