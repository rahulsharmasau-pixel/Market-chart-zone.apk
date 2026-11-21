package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalMarketExtendedColors
import java.util.Locale

data class PairSpec(
    val symbol: String,
    val pipSize: Double,
    val usdPerPipPerLot: Double
)

private val SUPPORTED_PAIRS = listOf(
    PairSpec("EUR/USD", 0.0001, 10.0),
    PairSpec("GBP/USD", 0.0001, 10.0),
    PairSpec("AUD/USD", 0.0001, 10.0),
    PairSpec("NZD/USD", 0.0001, 10.0),
    PairSpec("USD/JPY", 0.01, 6.67),
    PairSpec("GBP/JPY", 0.01, 6.67),
    PairSpec("USD/CAD", 0.0001, 7.15),
    PairSpec("USD/CHF", 0.0001, 11.20),
    PairSpec("XAU/USD", 0.10, 10.0),
    PairSpec("BTC/USD", 1.0, 1.0)
)

@Composable
fun ToolsScreen(
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    val ext = LocalMarketExtendedColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Trading Calculators",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Precision risk management before every execution",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex.coerceIn(0, 1),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { onSelectTab(0) },
                    text = { Text("Position size") },
                    modifier = Modifier.testTag("tab_position_size")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { onSelectTab(1) },
                    text = { Text("Profit") },
                    modifier = Modifier.testTag("tab_profit")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            if (selectedTabIndex == 0) {
                PositionSizeCalculatorCard()
            } else {
                ProfitCalculatorCard()
            }
        }
    }
}

@Composable
private fun PositionSizeCalculatorCard() {
    val ext = LocalMarketExtendedColors.current
    var balanceText by remember { mutableStateOf("10000") }
    var riskPctText by remember { mutableStateOf("1.0") }
    var stopLossPipsText by remember { mutableStateOf("25") }
    var selectedPair by remember { mutableStateOf(SUPPORTED_PAIRS.first()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var lotResult by remember { mutableStateOf<Double?>(null) }
    var riskAmountResult by remember { mutableStateOf<Double?>(null) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PairDropdown(
                selected = selectedPair,
                onSelect = { selectedPair = it }
            )

            OutlinedTextField(
                value = balanceText,
                onValueChange = { balanceText = it },
                label = { Text("Account balance (USD)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_balance_input")
            )

            OutlinedTextField(
                value = riskPctText,
                onValueChange = { riskPctText = it },
                label = { Text("Risk (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_risk_input")
            )

            OutlinedTextField(
                value = stopLossPipsText,
                onValueChange = { stopLossPipsText = it },
                label = { Text("Stop loss (pips)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_sl_input")
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        balanceText = ""
                        riskPctText = ""
                        stopLossPipsText = ""
                        lotResult = null
                        riskAmountResult = null
                        errorMsg = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("pos_reset_button")
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        val bal = balanceText.toDoubleOrNull()
                        val riskPct = riskPctText.toDoubleOrNull()
                        val sl = stopLossPipsText.toDoubleOrNull()
                        if (bal == null || bal <= 0 || riskPct == null || riskPct <= 0 || sl == null || sl <= 0) {
                            errorMsg = "Enter valid positive numbers for balance, risk %, and stop loss."
                            return@Button
                        }
                        errorMsg = null
                        val riskMoney = bal * (riskPct / 100.0)
                        val lots = riskMoney / (sl * selectedPair.usdPerPipPerLot)
                        riskAmountResult = riskMoney
                        lotResult = lots
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("pos_calculate_button")
                ) {
                    Text("Calculate", fontWeight = FontWeight.Bold)
                }
            }

            if (lotResult != null && riskAmountResult != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recommended Position Size",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f Standard Lots", lotResult),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.US, "Risk: $%.2f", riskAmountResult),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "Mini: %.1f • Micro: %.0f", lotResult!! * 10, lotResult!! * 100),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfitCalculatorCard() {
    val ext = LocalMarketExtendedColors.current
    var selectedPair by remember { mutableStateOf(SUPPORTED_PAIRS.first()) }
    var volumeText by remember { mutableStateOf("1.00") }
    var isLong by remember { mutableStateOf(true) }
    var openPriceText by remember { mutableStateOf("1.0840") }
    var closePriceText by remember { mutableStateOf("1.0910") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var profitResult by remember { mutableStateOf<Double?>(null) }
    var pipsResult by remember { mutableStateOf<Double?>(null) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PairDropdown(
                selected = selectedPair,
                onSelect = { selectedPair = it }
            )

            OutlinedTextField(
                value = volumeText,
                onValueChange = { volumeText = it },
                label = { Text("Volume (lots)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profit_volume_input")
            )

            Column {
                Text(
                    text = "Trade type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = isLong,
                        onClick = { isLong = true },
                        label = { Text("Long (Buy)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ext.bullish.copy(alpha = 0.2f),
                            selectedLabelColor = ext.bullish
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trade_type_long")
                    )
                    FilterChip(
                        selected = !isLong,
                        onClick = { isLong = false },
                        label = { Text("Short (Sell)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ext.bearish.copy(alpha = 0.2f),
                            selectedLabelColor = ext.bearish
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trade_type_short")
                    )
                }
            }

            OutlinedTextField(
                value = openPriceText,
                onValueChange = { openPriceText = it },
                label = { Text("Open price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profit_open_input")
            )

            OutlinedTextField(
                value = closePriceText,
                onValueChange = { closePriceText = it },
                label = { Text("Close price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profit_close_input")
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        volumeText = ""
                        openPriceText = ""
                        closePriceText = ""
                        profitResult = null
                        pipsResult = null
                        errorMsg = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("profit_reset_button")
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        val lots = volumeText.toDoubleOrNull()
                        val open = openPriceText.toDoubleOrNull()
                        val close = closePriceText.toDoubleOrNull()
                        if (lots == null || lots <= 0 || open == null || open <= 0 || close == null || close <= 0) {
                            errorMsg = "Enter valid positive numbers for lots, open price, and close price."
                            return@Button
                        }
                        errorMsg = null
                        val priceDiff = if (isLong) (close - open) else (open - close)
                        val pips = priceDiff / selectedPair.pipSize
                        val pnl = pips * selectedPair.usdPerPipPerLot * lots
                        pipsResult = pips
                        profitResult = pnl
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("profit_calculate_button")
                ) {
                    Text("Calculate", fontWeight = FontWeight.Bold)
                }
            }

            if (profitResult != null && pipsResult != null) {
                val isGain = profitResult!! >= 0.0
                val accent = if (isGain) ext.bullish else ext.bearish
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isGain) "Estimated Net Profit" else "Estimated Net Loss",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent
                        )
                        val sign = if (profitResult!! >= 0) "+" else "-"
                        Text(
                            text = String.format(Locale.US, "%s$%.2f", sign, kotlin.math.abs(profitResult!!)),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(Locale.US, "%+.1f pips (%s)", pipsResult, selectedPair.symbol),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairDropdown(
    selected: PairSpec,
    onSelect: (PairSpec) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.symbol,
            onValueChange = {},
            readOnly = true,
            label = { Text("Currency pair") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("pair_dropdown")
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SUPPORTED_PAIRS.forEach { pair ->
                DropdownMenuItem(
                    text = { Text(pair.symbol) },
                    onClick = {
                        onSelect(pair)
                        expanded = false
                    }
                )
            }
        }
    }
}
