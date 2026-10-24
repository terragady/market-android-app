package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY isPinnedToWidget DESC, isFavorite DESC, displayOrder ASC")
    fun getAllStocksFlow(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks ORDER BY isPinnedToWidget DESC, isFavorite DESC, displayOrder ASC")
    suspend fun getAllStocksSync(): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE symbol = :symbol LIMIT 1")
    suspend fun getStockBySymbol(symbol: String): StockEntity?

    @Query("SELECT * FROM stocks WHERE isPinnedToWidget = 1 LIMIT 1")
    suspend fun getPinnedWidgetStock(): StockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)

    @Update
    suspend fun updateStock(stock: StockEntity)

    @Query("UPDATE stocks SET isFavorite = :isFavorite WHERE symbol = :symbol")
    suspend fun updateFavorite(symbol: String, isFavorite: Boolean)

    @Query("UPDATE stocks SET isPinnedToWidget = CASE WHEN symbol = :symbol THEN 1 ELSE 0 END")
    suspend fun setPinnedWidgetStock(symbol: String)

    @Query("DELETE FROM stocks WHERE symbol = :symbol")
    suspend fun deleteStockBySymbol(symbol: String)

    @Query("SELECT COUNT(*) FROM stocks")
    suspend fun getStockCount(): Int
}
