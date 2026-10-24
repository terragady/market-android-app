package com.example.data

import android.content.Context
import com.example.data.local.StockDao
import com.example.data.local.StockDatabase
import com.example.data.local.StockEntity
import com.example.model.PricePoint
import com.example.model.StockQuote
import com.example.widget.StockWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class StockRepository(
    private val context: Context,
    private val stockDao: StockDao = StockDatabase.getInstance(context).stockDao()
) {
    private val random = Random()

    val stocksFlow: Flow<List<StockQuote>> = stockDao.getAllStocksFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun initializeIfNeeded() {
        withContext(Dispatchers.IO) {
            val count = stockDao.getStockCount()
            if (count == 0) {
                val seedList = getInitialSeedStocks()
                stockDao.insertStocks(seedList.mapIndexed { index, quote ->
                    StockEntity.fromDomain(quote, displayOrder = index)
                })
                StockWidgetProvider.updateAllWidgets(context)
            }
        }
    }

    suspend fun getStockBySymbol(symbol: String): StockQuote? {
        return withContext(Dispatchers.IO) {
            stockDao.getStockBySymbol(symbol)?.toDomain()
        }
    }

    suspend fun getWidgetStock(): StockQuote? {
        return withContext(Dispatchers.IO) {
            val pinned = stockDao.getPinnedWidgetStock()?.toDomain()
            pinned ?: stockDao.getAllStocksSync().firstOrNull()?.toDomain()
        }
    }

    suspend fun getAllStocksSync(): List<StockQuote> {
        return withContext(Dispatchers.IO) {
            stockDao.getAllStocksSync().map { it.toDomain() }
        }
    }

    suspend fun toggleFavorite(symbol: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            stockDao.updateFavorite(symbol, isFavorite)
        }
    }

    suspend fun setPinnedToWidget(symbol: String) {
        withContext(Dispatchers.IO) {
            stockDao.setPinnedWidgetStock(symbol)
            StockWidgetProvider.updateAllWidgets(context)
        }
    }

    suspend fun removeStock(symbol: String) {
        withContext(Dispatchers.IO) {
            stockDao.deleteStockBySymbol(symbol)
            StockWidgetProvider.updateAllWidgets(context)
        }
    }

    suspend fun addStock(symbol: String, name: String, startingPrice: Double = 150.0) {
        withContext(Dispatchers.IO) {
            val existing = stockDao.getStockBySymbol(symbol)
            if (existing != null) return@withContext

            val quote = generateStockQuote(
                symbol = symbol.uppercase().trim(),
                name = name.trim(),
                basePrice = startingPrice,
                peRatio = 28.5,
                marketCap = (startingPrice * 12_500_000_000L).toLong(),
                isPinned = false,
                isFav = true
            )
            stockDao.insertStock(StockEntity.fromDomain(quote, displayOrder = 100))
        }
    }

    private val apiService = com.example.data.remote.StockApiService()

    suspend fun getWidgetStocks(onlyFavorites: Boolean = false): List<StockQuote> {
        return withContext(Dispatchers.IO) {
            val all = stockDao.getAllStocksSync().map { it.toDomain() }
            if (onlyFavorites) {
                val favs = all.filter { it.isFavorite }
                if (favs.isNotEmpty()) favs else all
            } else {
                all
            }
        }
    }

    suspend fun refreshQuotes() {
        withContext(Dispatchers.IO) {
            val currentList = stockDao.getAllStocksSync()
            val updated = currentList.map { entity ->
                val domain = entity.toDomain()
                // Try live network fetch first
                val live = apiService.fetchLiveStockData(domain.symbol, domain)
                val finalQuote = live ?: simulateQuoteTick(domain)
                StockEntity.fromDomain(finalQuote, entity.displayOrder)
            }
            stockDao.insertStocks(updated)
            StockWidgetProvider.updateAllWidgets(context)
        }
    }

    suspend fun tickSingleStock(symbol: String): StockQuote? {
        return withContext(Dispatchers.IO) {
            val entity = stockDao.getStockBySymbol(symbol) ?: return@withContext null
            val domain = entity.toDomain()
            val updatedQuote = simulateQuoteTick(domain)
            stockDao.updateStock(StockEntity.fromDomain(updatedQuote, entity.displayOrder))
            StockWidgetProvider.updateAllWidgets(context)
            updatedQuote
        }
    }

    private fun simulateQuoteTick(stock: StockQuote): StockQuote {
        // Small realistic tick fluctuation between -0.4% and +0.45%
        val deltaPercent = (random.nextDouble() * 0.85 - 0.40) / 100.0
        val newPrice = max(1.0, ((stock.currentPrice * (1.0 + deltaPercent)) * 100).roundToInt() / 100.0)
        val newHigh = max(stock.dayHigh, newPrice)
        val newLow = min(stock.dayLow, newPrice)
        val newVol = stock.volume + (random.nextInt(8500) + 1500)

        // Append or update current day point
        val now = System.currentTimeMillis()
        val updatedDayPoints = stock.dayPoints.toMutableList()
        if (updatedDayPoints.isNotEmpty()) {
            val last = updatedDayPoints.last()
            if (now - last.timestamp < 60_000) {
                // Update in-place
                updatedDayPoints[updatedDayPoints.size - 1] = PricePoint(now, newPrice, last.volume + 500)
            } else {
                updatedDayPoints.add(PricePoint(now, newPrice, newVol))
            }
        } else {
            updatedDayPoints.add(PricePoint(now, newPrice, newVol))
        }

        // Update latest week point as well
        val updatedWeekPoints = stock.weekPoints.toMutableList()
        if (updatedWeekPoints.isNotEmpty()) {
            val lastW = updatedWeekPoints.last()
            updatedWeekPoints[updatedWeekPoints.size - 1] = PricePoint(now, newPrice, lastW.volume)
        }

        return stock.copy(
            currentPrice = newPrice,
            dayHigh = newHigh,
            dayLow = newLow,
            volume = newVol,
            dayPoints = updatedDayPoints,
            weekPoints = updatedWeekPoints,
            lastUpdated = now
        )
    }

    private fun getInitialSeedStocks(): List<StockQuote> {
        return listOf(
            generateStockQuote("NVDA", "NVIDIA Corporation", 138.25, 45.8, 3_410_000_000_000L, isPinned = true, isFav = true, trendBias = 0.034),
            generateStockQuote("AAPL", "Apple Inc.", 232.50, 34.2, 3_550_000_000_000L, isPinned = false, isFav = true, trendBias = 0.012),
            generateStockQuote("TSLA", "Tesla, Inc.", 218.40, 68.4, 698_000_000_000L, isPinned = false, isFav = true, trendBias = -0.018),
            generateStockQuote("MSFT", "Microsoft Corp.", 428.10, 36.1, 3_180_000_000_000L, isPinned = false, isFav = true, trendBias = 0.008),
            generateStockQuote("GOOGL", "Alphabet Inc.", 178.60, 24.3, 2_210_000_000_000L, isPinned = false, isFav = false, trendBias = 0.015),
            generateStockQuote("AMZN", "Amazon.com Inc.", 186.40, 42.1, 1_940_000_000_000L, isPinned = false, isFav = false, trendBias = 0.021),
            generateStockQuote("META", "Meta Platforms Inc.", 578.30, 27.9, 1_460_000_000_000L, isPinned = false, isFav = false, trendBias = 0.028),
            generateStockQuote("SPY", "SPDR S&P 500 ETF", 582.10, null, 590_000_000_000L, isPinned = false, isFav = false, trendBias = 0.006)
        )
    }

    private fun generateStockQuote(
        symbol: String,
        name: String,
        basePrice: Double,
        peRatio: Double?,
        marketCap: Long,
        isPinned: Boolean = false,
        isFav: Boolean = false,
        trendBias: Double = 0.01
    ): StockQuote {
        val prevClose = ((basePrice / (1.0 + trendBias)) * 100).roundToInt() / 100.0
        val currentPrice = basePrice

        val dayPoints = generateDayPoints(prevClose, currentPrice)
        val weekPoints = generateWeekPoints(basePrice)

        val allDayPrices = dayPoints.map { it.price }
        val dayHigh = max(currentPrice, allDayPrices.maxOrNull() ?: currentPrice)
        val dayLow = min(currentPrice, allDayPrices.minOrNull() ?: currentPrice)
        val dayOpen = dayPoints.firstOrNull()?.price ?: prevClose

        val week52High = ((basePrice * 1.25) * 100).roundToInt() / 100.0
        val week52Low = ((basePrice * 0.72) * 100).roundToInt() / 100.0
        val volume = 28_400_000L + random.nextInt(15_000_000)

        return StockQuote(
            symbol = symbol,
            name = name,
            currency = "USD",
            currentPrice = currentPrice,
            previousClose = prevClose,
            dayOpen = dayOpen,
            dayHigh = dayHigh,
            dayLow = dayLow,
            volume = volume,
            marketCap = marketCap,
            peRatio = peRatio,
            week52High = week52High,
            week52Low = week52Low,
            dayPoints = dayPoints,
            weekPoints = weekPoints,
            isFavorite = isFav,
            isPinnedToWidget = isPinned
        )
    }

    private fun generateDayPoints(prevClose: Double, targetEnd: Double): List<PricePoint> {
        val points = mutableListOf<PricePoint>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 30)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val totalMinutes = 390 // 9:30 AM to 4:00 PM (6.5 hours)
        val stepMinutes = 15
        val steps = totalMinutes / stepMinutes

        var current = prevClose * (1.0 + (random.nextDouble() * 0.008 - 0.004))
        for (i in 0..steps) {
            val progress = i.toDouble() / steps
            // Interpolate toward targetEnd with natural Brownian drift
            val target = prevClose + (targetEnd - prevClose) * progress
            val noise = (random.nextDouble() * 2.0 - 1.0) * (targetEnd * 0.004)
            current = (current * 0.82) + (target * 0.18) + noise
            current = ((current * 100).roundToInt() / 100.0)

            val ts = cal.timeInMillis + (i * stepMinutes * 60_000L)
            val vol = 120_000L + random.nextInt(400_000)
            points.add(PricePoint(ts, current, vol))
        }
        // Force the last point to exact current price
        if (points.isNotEmpty()) {
            points[points.size - 1] = PricePoint(System.currentTimeMillis(), targetEnd, 250_000L)
        }
        return points
    }

    private fun generateWeekPoints(currentPrice: Double): List<PricePoint> {
        val points = mutableListOf<PricePoint>()
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val daysBack = 7
        val stepsPerDay = 5
        val totalSteps = daysBack * stepsPerDay

        var price = currentPrice * (0.94 + random.nextDouble() * 0.09) // week starting baseline
        val stepMillis = (daysBack * 24 * 3600 * 1000L) / totalSteps

        for (i in 0 until totalSteps) {
            val progress = i.toDouble() / totalSteps
            val target = price + (currentPrice - price) * progress
            val noise = (random.nextDouble() * 2.0 - 1.0) * (currentPrice * 0.008)
            price = (price * 0.85) + (target * 0.15) + noise
            price = ((price * 100).roundToInt() / 100.0)

            val ts = (now - (totalSteps - i) * stepMillis)
            points.add(PricePoint(ts, price, 500_000L + random.nextInt(1_200_000)))
        }
        points.add(PricePoint(now, currentPrice, 850_000L))
        return points
    }
}
