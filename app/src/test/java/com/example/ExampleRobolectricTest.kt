package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.StockEntity
import com.example.model.PricePoint
import com.example.model.StockQuote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StockPulse", appName)
  }

  @Test
  fun `stock price and change calculation`() {
    val quote = StockQuote(
      symbol = "NVDA",
      name = "NVIDIA Corporation",
      currentPrice = 140.0,
      previousClose = 130.0,
      dayOpen = 132.0,
      dayHigh = 142.0,
      dayLow = 131.0,
      volume = 1000000L,
      marketCap = 3000000000000L,
      peRatio = 45.0,
      week52High = 150.0,
      week52Low = 70.0,
      dayPoints = listOf(PricePoint(1000L, 132.0), PricePoint(2000L, 140.0)),
      weekPoints = listOf(PricePoint(500L, 125.0), PricePoint(2000L, 140.0))
    )

    assertEquals(10.0, quote.change, 0.001)
    assertTrue(quote.isPositive)
    assertEquals("+7.69%", quote.formattedChangePercent())
  }

  @Test
  fun `stock entity point encoding and decoding`() {
    val points = listOf(
      PricePoint(1000L, 105.5, 1200L),
      PricePoint(2000L, 108.25, 3400L)
    )
    val encoded = StockEntity.encodePoints(points)
    val decoded = StockEntity.decodePoints(encoded)

    assertEquals(2, decoded.size)
    assertEquals(105.5, decoded[0].price, 0.001)
    assertEquals(1200L, decoded[0].volume)
    assertEquals(108.25, decoded[1].price, 0.001)
  }
}
