package com.hisabkitab.khata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.hisabkitab.khata.R
import com.hisabkitab.khata.data.Formats
import com.hisabkitab.khata.data.TxType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, onManageShops: () -> Unit, onCustomers: () -> Unit) {
    val shops by vm.shops.collectAsState()
    val selected by vm.selectedShopId.collectAsState()
    val d by vm.dashboard.collectAsState()
    var menu by remember { mutableStateOf(false) }
    val shopList = shops.orEmpty()
    val label = shopList.firstOrNull { it.id == selected }?.name ?: stringResource(R.string.all_shops)

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.weight(1f))
                        Text("▾")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.all_shops)) },
                            onClick = { vm.selectShop(0L); menu = false }
                        )
                        shopList.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { vm.selectShop(s.id); menu = false }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.manage_shops)) },
                            onClick = { menu = false; onManageShops() }
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(stringResource(R.string.today_credit), moneyText(d.todayCredit), Modifier.weight(1f), OweRed)
                    StatCard(stringResource(R.string.today_collections), moneyText(d.todayCollections), Modifier.weight(1f), PayGreen)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(stringResource(R.string.receivables), moneyText(d.receivables), Modifier.weight(1f), OweRed)
                    StatCard(stringResource(R.string.payables), moneyText(d.payables), Modifier.weight(1f), PayGreen)
                }
            }
            item {
                StatCard(stringResource(R.string.total_shops), shopList.size.toString(), Modifier.fillMaxWidth())
            }
            item {
                Text(stringResource(R.string.quick_actions), style = MaterialTheme.typography.titleSmall)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onCustomers, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.customers))
                    }
                    OutlinedButton(onClick = onManageShops, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.shops))
                    }
                }
            }
            item {
                Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleSmall)
            }
            items(d.recent, key = { it.id }) { t ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(t.customerName, fontWeight = FontWeight.Medium)
                        Text(
                            Formats.dateTime(t.timestamp) + if (t.description.isNotBlank()) " • " + t.description else "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val isCredit = t.type == TxType.GIVE_CREDIT
                    Text(
                        (if (isCredit) "+" else "−") + moneyText(t.amount),
                        color = if (isCredit) OweRed else PayGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
