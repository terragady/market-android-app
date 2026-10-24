package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.PricePoint
import com.example.model.StockQuote

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String,
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
    val dayPointsEncoded: String, // serialized compact string: "ts,price,vol;ts,price,vol"
    val weekPointsEncoded: String,
    val lastUpdated: Long,
    val isFavorite: Boolean = false,
    val isPinnedToWidget: Boolean = false,
    val displayOrder: Int = 0
) {
    fun toDomain(): StockQuote {
        return StockQuote(
            symbol = symbol,
            name = name,
            currency = currency,
            currentPrice = currentPrice,
            previousClose = previousClose,
            dayOpen = dayOpen,
            dayHigh = dayHigh,
            dayLow = dayLow,
            volume = volume,
            marketCap = marketCap,
            peRatio = peRatio,
            week52High = week52High,
            week52Low = week52Low,
            dayPoints = decodePoints(dayPointsEncoded),
            weekPoints = decodePoints(weekPointsEncoded),
            lastUpdated = lastUpdated,
            isFavorite = isFavorite,
            isPinnedToWidget = isPinnedToWidget
        )
    }

    companion object {
        fun fromDomain(quote: StockQuote, displayOrder: Int = 0): StockEntity {
            return StockEntity(
                symbol = quote.symbol,
                name = quote.name,
                currency = quote.currency,
                currentPrice = quote.currentPrice,
                previousClose = quote.previousClose,
                dayOpen = quote.dayOpen,
                dayHigh = quote.dayHigh,
                dayLow = quote.dayLow,
                volume = quote.volume,
                marketCap = quote.marketCap,
                peRatio = quote.peRatio,
                week52High = quote.week52High,
                week52Low = quote.week52Low,
                dayPointsEncoded = encodePoints(quote.dayPoints),
                weekPointsEncoded = encodePoints(quote.weekPoints),
                lastUpdated = quote.lastUpdated,
                isFavorite = quote.isFavorite,
                isPinnedToWidget = quote.isPinnedToWidget,
                displayOrder = displayOrder
            )
        }

        fun encodePoints(points: List<PricePoint>): String {
            return points.joinToString(";") { "${it.timestamp},${it.price},${it.volume}" }
        }

        fun decodePoints(encoded: String): List<PricePoint> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(";").mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size >= 2) {
                    val ts = parts[0].toLongOrNull() ?: 0L
                    val price = parts[1].toDoubleOrNull() ?: 0.0
                    val vol = if (parts.size >= 3) parts[2].toLongOrNull() ?: 0L else 0L
                    PricePoint(ts, price, vol)
                } else null
            }
        }
    }
}
