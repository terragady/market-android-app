package com.example.data.remote

import android.util.Log
import com.example.model.PricePoint
import com.example.model.StockQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class StockApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "StockApiService"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    suspend fun fetchLiveStockData(symbol: String, fallback: StockQuote): StockQuote? {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch 1-day intraday data
                val dayUrl = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=15m&range=1d"
                val dayRequest = Request.Builder()
                    .url(dayUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()

                val dayResponse = client.newCall(dayRequest).execute()
                if (!dayResponse.isSuccessful) {
                    dayResponse.close()
                    return@withContext null
                }

                val dayBody = dayResponse.body?.string() ?: return@withContext null
                dayResponse.close()

                val dayJson = JSONObject(dayBody)
                val chart = dayJson.optJSONObject("chart") ?: return@withContext null
                val results = chart.optJSONArray("result") ?: return@withContext null
                if (results.length() == 0) return@withContext null

                val item = results.getJSONObject(0)
                val meta = item.getJSONObject("meta")

                val currentPrice = meta.optDouble("regularMarketPrice", fallback.currentPrice)
                val prevClose = meta.optDouble("previousClose", meta.optDouble("chartPreviousClose", fallback.previousClose))
                val dayHigh = meta.optDouble("regularMarketDayHigh", fallback.dayHigh)
                val dayLow = meta.optDouble("regularMarketDayLow", fallback.dayLow)
                val dayVolume = meta.optLong("regularMarketVolume", fallback.volume)
                val currency = meta.optString("currency", "USD")

                // Parse day points
                val timestamps = item.optJSONArray("timestamp")
                val indicators = item.optJSONObject("indicators")
                val quoteArray = indicators?.optJSONArray("quote")
                val quoteObj = quoteArray?.optJSONObject(0)
                val closes = quoteObj?.optJSONArray("close")
                val volumes = quoteObj?.optJSONArray("volume")

                val dayPoints = mutableListOf<PricePoint>()
                if (timestamps != null && closes != null) {
                    for (i in 0 until timestamps.length()) {
                        val ts = timestamps.optLong(i) * 1000L
                        val closeVal = closes.optDouble(i, Double.NaN)
                        val vol = volumes?.optLong(i, 0L) ?: 0L
                        if (!closeVal.isNaN() && closeVal > 0.0) {
                            dayPoints.add(PricePoint(ts, closeVal, vol))
                        }
                    }
                }

                val finalDayPoints = if (dayPoints.size >= 2) dayPoints else fallback.dayPoints

                // Fetch 5-day / 1-week data
                val weekPoints = fetchWeekPoints(symbol) ?: fallback.weekPoints

                fallback.copy(
                    currentPrice = currentPrice,
                    previousClose = prevClose,
                    dayHigh = max(dayHigh, currentPrice),
                    dayLow = min(dayLow, currentPrice),
                    volume = dayVolume,
                    currency = currency,
                    dayPoints = finalDayPoints,
                    weekPoints = weekPoints,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Network fetch failed for $symbol: ${e.message}. Using cache/simulation.")
                null
            }
        }
    }

    private fun fetchWeekPoints(symbol: String): List<PricePoint>? {
        return try {
            val weekUrl = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1h&range=5d"
            val request = Request.Builder()
                .url(weekUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }

            val body = response.body?.string() ?: return null
            response.close()

            val json = JSONObject(body)
            val chart = json.optJSONObject("chart") ?: return null
            val results = chart.optJSONArray("result") ?: return null
            if (results.length() == 0) return null

            val item = results.getJSONObject(0)
            val timestamps = item.optJSONArray("timestamp")
            val quoteObj = item.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0)
            val closes = quoteObj?.optJSONArray("close")
            val volumes = quoteObj?.optJSONArray("volume")

            val points = mutableListOf<PricePoint>()
            if (timestamps != null && closes != null) {
                for (i in 0 until timestamps.length()) {
                    val ts = timestamps.optLong(i) * 1000L
                    val closeVal = closes.optDouble(i, Double.NaN)
                    val vol = volumes?.optLong(i, 0L) ?: 0L
                    if (!closeVal.isNaN() && closeVal > 0.0) {
                        points.add(PricePoint(ts, closeVal, vol))
                    }
                }
            }
            if (points.size >= 2) points else null
        } catch (e: Exception) {
            null
        }
    }
}
