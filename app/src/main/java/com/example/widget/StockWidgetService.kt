package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.StockRepository
import com.example.data.local.StockDatabase
import com.example.model.PricePoint
import com.example.model.StockQuote
import com.example.model.Timeframe
import kotlinx.coroutines.runBlocking
import kotlin.math.max
import kotlin.math.min

class StockWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StockRemoteViewsFactory(applicationContext)
    }
}

class StockRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var stocks: List<StockQuote> = emptyList()
    private var showGraphs: Boolean = true
    private var timeframe: Timeframe = Timeframe.DAY
    private var onlyFavorites: Boolean = false

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        val prefs = context.getSharedPreferences(StockWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        showGraphs = prefs.getBoolean(StockWidgetProvider.PREF_SHOW_GRAPHS, true)
        val tfStr = prefs.getString(StockWidgetProvider.PREF_TIMEFRAME, Timeframe.DAY.name) ?: Timeframe.DAY.name
        timeframe = if (tfStr == Timeframe.WEEK.name) Timeframe.WEEK else Timeframe.DAY
        onlyFavorites = prefs.getBoolean(StockWidgetProvider.PREF_FILTER_FAVORITES, false)

        try {
            runBlocking {
                val db = StockDatabase.getInstance(context)
                var all = db.stockDao().getAllStocksSync().map { it.toDomain() }
                if (all.isEmpty()) {
                    val repo = StockRepository(context)
                    repo.initializeIfNeeded()
                    all = db.stockDao().getAllStocksSync().map { it.toDomain() }
                }
                stocks = if (onlyFavorites) {
                    val favs = all.filter { it.isFavorite }
                    if (favs.isNotEmpty()) favs else all
                } else {
                    all
                }
            }
        } catch (e: Exception) {
            stocks = emptyList()
        }
    }

    override fun onDestroy() {
        stocks = emptyList()
    }

    override fun getCount(): Int = stocks.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= stocks.size) return null
        val stock = stocks[position]

        val views = RemoteViews(context.packageName, R.layout.widget_stock_item)

        // Textual info
        views.setTextViewText(R.id.item_symbol, stock.symbol)
        views.setTextViewText(R.id.item_name, stock.name)
        views.setTextViewText(R.id.item_price, stock.formattedPrice())

        val isDay = timeframe == Timeframe.DAY
        val isPositive = if (isDay) stock.isPositive else stock.weekChange >= 0
        val changeText = if (isDay) stock.formattedChangePercent() else stock.formattedWeekChangePercent()

        views.setTextViewText(R.id.item_change, changeText)
        views.setInt(
            R.id.item_change,
            "setBackgroundResource",
            if (isPositive) R.drawable.widget_badge_gain else R.drawable.widget_badge_loss
        )
        views.setTextColor(
            R.id.item_change,
            if (isPositive) Color.parseColor("#00E676") else Color.parseColor("#FF5252")
        )

        // Small Sparkline Graph (Optional based on user toggle)
        if (showGraphs) {
            views.setViewVisibility(R.id.item_chart_image, View.VISIBLE)
            val points = if (isDay) stock.dayPoints else stock.weekPoints
            val baseline = if (isDay) stock.previousClose else stock.weekStartPrice
            val sparkline = renderItemSparkline(points, baseline, isPositive)
            views.setImageViewBitmap(R.id.item_chart_image, sparkline)
        } else {
            // Ultra-compact mode without graphs
            views.setViewVisibility(R.id.item_chart_image, View.GONE)
        }

        // Deep linking to open specific stock in app
        val fillInIntent = Intent().apply {
            putExtra(StockWidgetProvider.EXTRA_SYMBOL, stock.symbol)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    private fun renderItemSparkline(
        points: List<PricePoint>,
        baselinePrice: Double,
        isPositive: Boolean
    ): Bitmap {
        val width = 280
        val height = 70
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (points.size < 2) return bitmap

        val strokeColor = if (isPositive) Color.argb(175, 0, 230, 118) else Color.argb(175, 255, 82, 82)
        val gradStart = if (isPositive) Color.argb(70, 0, 230, 118) else Color.argb(70, 255, 82, 82)
        val gradEnd = Color.argb(8, 0, 0, 0)

        val prices = points.map { it.price }
        val minPrice = min(baselinePrice, prices.minOrNull() ?: baselinePrice)
        val maxPrice = max(baselinePrice, prices.maxOrNull() ?: baselinePrice)
        val range = max(0.01, maxPrice - minPrice)

        fun getX(i: Int): Float = (i.toFloat() / (points.size - 1)) * (width - 12f) + 6f
        fun getY(price: Double): Float {
            val norm = (price - minPrice) / range
            return (height - 8f - (norm * (height - 18f))).toFloat().coerceIn(6f, height - 6f)
        }

        // Draw faint baseline
        val baselineY = getY(baselinePrice)
        val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 255, 255, 255)
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(6f, baselineY, width - 6f, baselineY, baselinePaint)

        val linePath = Path()
        val fillPath = Path()

        linePath.moveTo(getX(0), getY(points[0].price))
        fillPath.moveTo(getX(0), height.toFloat())
        fillPath.lineTo(getX(0), getY(points[0].price))

        for (i in 1 until points.size) {
            val prevX = getX(i - 1)
            val prevY = getY(points[i - 1].price)
            val curX = getX(i)
            val curY = getY(points[i].price)
            val midX = (prevX + curX) / 2f
            linePath.cubicTo(midX, prevY, midX, curY, curX, curY)
            fillPath.cubicTo(midX, prevY, midX, curY, curX, curY)
        }

        fillPath.lineTo(getX(points.size - 1), height.toFloat())
        fillPath.close()

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), gradStart, gradEnd, Shader.TileMode.CLAMP)
        }
        canvas.drawPath(fillPath, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            strokeWidth = 2.4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(linePath, strokePaint)

        // Draw glowing point at the last price
        val lastX = getX(points.size - 1)
        val lastY = getY(points.last().price)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPositive) Color.argb(90, 0, 230, 118) else Color.argb(90, 255, 82, 82)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(lastX, lastY, 6f, glowPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPositive) Color.parseColor("#00E676") else Color.parseColor("#FF5252")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(lastX, lastY, 3.5f, dotPaint)

        return bitmap
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
