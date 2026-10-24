package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StockGain
import com.example.ui.theme.StockGainBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

data class CatalogItem(
    val symbol: String,
    val name: String,
    val basePrice: Double,
    val category: String
)

val POPULAR_CATALOG = listOf(
    CatalogItem("AMD", "Advanced Micro Devices", 156.40, "Tech"),
    CatalogItem("PLTR", "Palantir Technologies", 43.80, "AI"),
    CatalogItem("INTC", "Intel Corporation", 22.90, "Tech"),
    CatalogItem("AVGO", "Broadcom Inc.", 174.50, "Tech"),
    CatalogItem("ARM", "Arm Holdings plc", 142.10, "Tech"),
    CatalogItem("NFLX", "Netflix Inc.", 712.30, "Media"),
    CatalogItem("DIS", "The Walt Disney Company", 96.40, "Media"),
    CatalogItem("COIN", "Coinbase Global, Inc.", 215.80, "Crypto"),
    CatalogItem("JPM", "JPMorgan Chase & Co.", 222.10, "Finance"),
    CatalogItem("V", "Visa Inc.", 284.50, "Finance"),
    CatalogItem("QQQ", "Invesco QQQ Trust ETF", 496.20, "ETF"),
    CatalogItem("DIA", "SPDR Dow Jones ETF", 428.90, "ETF"),
    CatalogItem("LLY", "Eli Lilly and Company", 894.20, "Healthcare")
)

@Composable
fun AddStockDialog(
    onDismiss: () -> Unit,
    onAddStock: (symbol: String, name: String, startingPrice: Double) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Popular, 1: Custom
    var catalogSearch by remember { mutableStateOf("") }

    var customSymbol by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customPrice by remember { mutableStateOf("150.00") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_stock_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Add Stock to Watchlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Popular Market Leaders vs Custom Ticker
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurfaceVariant,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = StockGain
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Popular Leaders", color = if (selectedTab == 0) StockGain else TextSecondary) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Custom Ticker", color = if (selectedTab == 1) StockGain else TextSecondary) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // Popular catalog with search
                    OutlinedTextField(
                        value = catalogSearch,
                        onValueChange = { catalogSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search catalog...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StockGain,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredCatalog = POPULAR_CATALOG.filter {
                        catalogSearch.isBlank() ||
                        it.symbol.contains(catalogSearch, ignoreCase = true) ||
                        it.name.contains(catalogSearch, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCatalog) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddStock(item.symbol, item.name, item.basePrice)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(item.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(item.name, color = TextMuted, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = String.format(Locale.US, "$%.2f", item.basePrice),
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = StockGain
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Custom ticker form
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customSymbol,
                            onValueChange = {
                                customSymbol = it.uppercase().trim()
                                errorMessage = null
                            },
                            label = { Text("Stock Symbol (e.g. AMD)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_symbol_input"),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StockGain,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = customName,
                            onValueChange = {
                                customName = it
                                errorMessage = null
                            },
                            label = { Text("Company Name (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StockGain,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = customPrice,
                            onValueChange = {
                                customPrice = it
                                errorMessage = null
                            },
                            label = { Text("Base Price USD ($)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StockGain,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        errorMessage?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val sym = customSymbol.trim()
                                if (sym.isEmpty()) {
                                    errorMessage = "Please enter a valid stock symbol"
                                    return@Button
                                }
                                val price = customPrice.toDoubleOrNull() ?: 100.0
                                val name = if (customName.isNotBlank()) customName.trim() else "$sym Corporation"
                                onAddStock(sym, name, price)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_add_stock_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StockGain, contentColor = Color(0xFF0A0D14))
                        ) {
                            Text("Add to Watchlist", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
