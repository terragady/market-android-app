package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.StockRepository
import com.example.model.Timeframe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_REFRESH -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = StockRepository(context)
                        repository.initializeIfNeeded()
                        repository.refreshQuotes()
                        updateAllWidgetsInternal(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_WIDGET_TOGGLE_TIMEFRAME -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val current = prefs.getString(PREF_TIMEFRAME, Timeframe.DAY.name) ?: Timeframe.DAY.name
                val next = if (current == Timeframe.DAY.name) Timeframe.WEEK.name else Timeframe.DAY.name
                prefs.edit().putString(PREF_TIMEFRAME, next).apply()

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        updateAllWidgetsInternal(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_WIDGET_TOGGLE_GRAPHS -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val current = prefs.getBoolean(PREF_SHOW_GRAPHS, true)
                prefs.edit().putBoolean(PREF_SHOW_GRAPHS, !current).apply()

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        updateAllWidgetsInternal(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_WIDGET_FILTER_FAVS -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val current = prefs.getBoolean(PREF_FILTER_FAVORITES, false)
                prefs.edit().putBoolean(PREF_FILTER_FAVORITES, !current).apply()

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        updateAllWidgetsInternal(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.example.stockpulse.ACTION_WIDGET_REFRESH"
        const val ACTION_WIDGET_TOGGLE_TIMEFRAME = "com.example.stockpulse.ACTION_WIDGET_TOGGLE_TIMEFRAME"
        const val ACTION_WIDGET_TOGGLE_GRAPHS = "com.example.stockpulse.ACTION_WIDGET_TOGGLE_GRAPHS"
        const val ACTION_WIDGET_FILTER_FAVS = "com.example.stockpulse.ACTION_WIDGET_FILTER_FAVS"

        const val PREFS_NAME = "stock_widget_preferences"
        const val PREF_TIMEFRAME = "pref_widget_timeframe"
        const val PREF_SHOW_GRAPHS = "pref_widget_show_graphs"
        const val PREF_FILTER_FAVORITES = "pref_widget_filter_favorites"

        const val EXTRA_SYMBOL = "extra_stock_symbol"
        const val EXTRA_OPEN_WIDGET_SETTINGS = "extra_open_widget_settings"

        fun updateAllWidgets(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                updateAllWidgetsInternal(context)
            }
        }

        private suspend fun updateAllWidgetsInternal(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, StockWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
            }
        }

        private suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val repository = StockRepository(context)
            repository.initializeIfNeeded()

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val showGraphs = prefs.getBoolean(PREF_SHOW_GRAPHS, true)
            val timeframeStr = prefs.getString(PREF_TIMEFRAME, Timeframe.DAY.name) ?: Timeframe.DAY.name
            val timeframe = if (timeframeStr == Timeframe.WEEK.name) Timeframe.WEEK else Timeframe.DAY
            val onlyFavorites = prefs.getBoolean(PREF_FILTER_FAVORITES, false)

            val flagImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val flagMutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

            val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
            val updatedText = "Updated " + timeFormat.format(Date())

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.stock_widget_layout)

                views.setTextViewText(R.id.widget_update_time, updatedText)

                // Set up collection adapter for ListView with unique content Uri
                val serviceIntent = Intent(context, StockWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = Uri.parse("content://com.example.stockpulse.widget/id/$widgetId")
                }
                views.setRemoteAdapter(R.id.widget_stock_list, serviceIntent)
                views.setEmptyView(R.id.widget_stock_list, R.id.widget_empty_view)

                // Click template for individual list items -> opens MainActivity with stock extra
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val itemPendingIntent = PendingIntent.getActivity(
                    context,
                    100 + widgetId,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or flagMutable
                )
                views.setPendingIntentTemplate(R.id.widget_stock_list, itemPendingIntent)


                // Refresh Button
                val refreshIntent = Intent(context, StockWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_REFRESH
                }
                val refreshPending = PendingIntent.getBroadcast(
                    context,
                    204 + widgetId,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable
                )
                views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPending)

                // Notify ListView to refresh items
                appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_stock_list)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
