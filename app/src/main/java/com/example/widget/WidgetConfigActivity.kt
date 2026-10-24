package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.MainViewModel
import com.example.ui.components.WidgetManagerSheet
import com.example.ui.theme.MyApplicationTheme

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set result to CANCELED in case user cancels or navigates back
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                WidgetManagerSheet(
                    stocks = uiState.stocks,
                    currentPinnedStock = uiState.stocks.find { it.isPinnedToWidget } ?: uiState.selectedStock,
                    onSelectPinnedStock = { symbol ->
                        val target = uiState.stocks.find { it.symbol == symbol }
                        if (target != null) {
                            viewModel.pinToWidget(target)
                        }
                    },
                    onDismiss = {
                        StockWidgetProvider.updateAllWidgets(applicationContext)
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}
