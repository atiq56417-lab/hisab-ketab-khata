package com.hisabkitab.khata.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.hisabkitab.khata.R
import com.hisabkitab.khata.data.Shop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopsScreen(vm: MainViewModel, onBack: (() -> Unit)?, onSelected: () -> Unit) {
    val shops by vm.shops.collectAsState()
    val list = shops.orEmpty()
    var editing by remember { mutableStateOf<Shop?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Shop?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shops)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_shop))
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (list.isEmpty()) {
                Text(
                    stringResource(R.string.no_shops),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { shop ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            vm.selectShop(shop.id)
                            onSelected()
                        }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(shop.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (shop.ownerName.isNotBlank()) Text(shop.ownerName)
                                if (shop.phone.isNotBlank()) Text(shop.phone, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { editing = shop }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                            IconButton(onClick = { removing = shop }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
            if (onBack == null && list.isNotEmpty()) {
                Button(
                    onClick = onSelected,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text(stringResource(R.string.continue_btn)) }
            }
        }
    }

    if (showAdd) {
        ShopDialog(shop = null, onDismiss = { showAdd = false }) {
            vm.addShop(it)
            showAdd = false
        }
    }
    editing?.let { s ->
        ShopDialog(shop = s, onDismiss = { editing = null }) {
            vm.updateShop(it)
            editing = null
        }
    }
    removing?.let { s ->
        ConfirmDialog(
            title = stringResource(R.string.remove_shop_title),
            message = stringResource(R.string.remove_shop_msg),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { vm.archiveShop(s.id); removing = null },
            onDismiss = { removing = null }
        )
    }
}
