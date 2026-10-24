package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StockRepository
import com.example.model.StockQuote
import com.example.model.Timeframe
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MainUiState(
    val stocks: List<StockQuote> = emptyList(),
    val selectedStock: StockQuote? = null,
    val selectedTimeframe: Timeframe = Timeframe.DAY,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val isLiveStreaming: Boolean = true,
    val isDetailScreenOpen: Boolean = false,
    val showAddStockDialog: Boolean = false,
    val showWidgetSheet: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var pendingSymbolToSelect: String? = null
    private var liveStreamJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeIfNeeded()
            repository.stocksFlow.collectLatest { list ->
                _uiState.update { current ->
                    val target = pendingSymbolToSelect
                    val selected = if (target != null) {
                        list.find { it.symbol.equals(target, ignoreCase = true) }
                            ?: current.selectedStock
                            ?: list.firstOrNull()
                    } else {
                        list.find { it.symbol == current.selectedStock?.symbol }
                            ?: list.find { it.isPinnedToWidget }
                            ?: list.firstOrNull()
                    }

                    val openDetail = if (target != null) true else current.isDetailScreenOpen
                    if (target != null && selected != null) {
                        pendingSymbolToSelect = null
                    }

                    current.copy(
                        stocks = list,
                        selectedStock = selected,
                        isDetailScreenOpen = openDetail
                    )
                }
            }
        }
        startLiveStreaming()
    }

    fun openStockDetail(stock: StockQuote) {
        _uiState.update { it.copy(selectedStock = stock, isDetailScreenOpen = true) }
    }

    fun closeStockDetail() {
        _uiState.update { it.copy(isDetailScreenOpen = false) }
    }

    fun selectStock(stock: StockQuote) {
        _uiState.update { it.copy(selectedStock = stock) }
    }

    fun selectStockBySymbol(symbol: String, openDetail: Boolean = true) {
        pendingSymbolToSelect = symbol
        val found = _uiState.value.stocks.find { it.symbol.equals(symbol, ignoreCase = true) }
        if (found != null) {
            pendingSymbolToSelect = null
            _uiState.update { it.copy(selectedStock = found, isDetailScreenOpen = openDetail) }
        }
    }

    fun setTimeframe(timeframe: Timeframe) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavorite(stock: StockQuote) {
        viewModelScope.launch {
            repository.toggleFavorite(stock.symbol, !stock.isFavorite)
        }
    }

    fun pinToWidget(stock: StockQuote) {
        viewModelScope.launch {
            repository.setPinnedToWidget(stock.symbol)
        }
    }

    fun addStock(symbol: String, name: String, startingPrice: Double) {
        viewModelScope.launch {
            repository.addStock(symbol, name, startingPrice)
        }
    }

    fun deleteStock(stock: StockQuote) {
        viewModelScope.launch {
            repository.removeStock(stock.symbol)
        }
    }

    fun refreshQuotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository.refreshQuotes()
            delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleLiveStreaming() {
        val current = _uiState.value.isLiveStreaming
        if (current) {
            stopLiveStreaming()
            _uiState.update { it.copy(isLiveStreaming = false) }
        } else {
            startLiveStreaming()
            _uiState.update { it.copy(isLiveStreaming = true) }
        }
    }

    fun setShowAddStockDialog(show: Boolean) {
        _uiState.update { it.copy(showAddStockDialog = show) }
    }

    fun setShowWidgetSheet(show: Boolean) {
        _uiState.update { it.copy(showWidgetSheet = show) }
    }

    private fun startLiveStreaming() {
        liveStreamJob?.cancel()
        liveStreamJob = viewModelScope.launch {
            while (isActive) {
                delay(4000)
                val current = _uiState.value.selectedStock?.symbol
                if (current != null) {
                    repository.tickSingleStock(current)
                }
            }
        }
    }

    private fun stopLiveStreaming() {
        liveStreamJob?.cancel()
        liveStreamJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveStreaming()
    }
}
