package com.hisabkitab.khata.ui

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hisabkitab.khata.R
import com.hisabkitab.khata.data.Formats
import com.hisabkitab.khata.data.LedgerTransaction
import com.hisabkitab.khata.data.TxType
import com.hisabkitab.khata.pdf.StatementPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class StatementRow(val tx: LedgerTransaction, val before: Long, val after: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(vm: MainViewModel, customerId: Long, onBack: () -> Unit) {
    val customerFlow = remember(customerId) { vm.customer(customerId) }
    val txFlow = remember(customerId) { vm.transactions(customerId) }
    val customer by customerFlow.collectAsState(initial = null)
    val txs by txFlow.collectAsState(initial = emptyList())
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var dialogType by remember { mutableStateOf<TxType?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var showRemove by remember { mutableStateOf(false) }
    var voiding by remember { mutableStateOf<LedgerTransaction?>(null) }

    val c = customer
    if (c == null) {
        Scaffold(topBar = {
            TopAppBar(title = {}, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
            })
        }) { Column(Modifier.padding(it)) {} }
        return
    }

    // Running balance is recomputed from the opening balance on every display; nothing is stored,
    // so a balance can never drift out of sync with the history.
    var running = c.openingBalance
    val rows = txs.map { t ->
        val before = running
        if (!t.voided) running += if (t.type == TxType.GIVE_CREDIT) t.amount else -t.amount
        StatementRow(t, before, running)
    }
    val totalCredit = txs.filter { !it.voided && it.type == TxType.GIVE_CREDIT }.sumOf { it.amount }
    val totalPaid = txs.filter { !it.voided && it.type == TxType.RECEIVE_PAYMENT }.sumOf { it.amount }
    val statusText = if (c.balance > 0 && totalPaid > 0) stringResource(R.string.partially_paid) else balanceLabel(c.balance)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(c.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) { StatementPdf.create(ctx, c, txs) }
                                StatementPdf.share(ctx, file)
                            } catch (e: Exception) {
                                Toast.makeText(ctx, e.message ?: "PDF error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_statement)) }
                    IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = { showRemove = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (c.phone.isNotBlank()) Text(c.phone)
                        if (c.address.isNotBlank()) Text(c.address, style = MaterialTheme.typography.bodySmall)
                        Text(
                            moneyText(kotlin.math.abs(c.balance)),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor(c.balance, MaterialTheme.colorScheme.onSurface)
                        )
                        Text(statusText, style = MaterialTheme.typography.titleSmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.total_credit) + ": " + moneyText(totalCredit), style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.total_payments) + ": " + moneyText(totalPaid), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { dialogType = TxType.GIVE_CREDIT }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.give_credit))
                    }
                    OutlinedButton(onClick = { dialogType = TxType.RECEIVE_PAYMENT }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.receive_payment))
                    }
                }
            }
            item { Text(stringResource(R.string.statement), style = MaterialTheme.typography.titleSmall) }
            if (rows.isEmpty()) {
                item { Text(stringResource(R.string.no_transactions)) }
            }
            items(rows.reversed(), key = { it.tx.id }) { r ->
                val t = r.tx
                val isCredit = t.type == TxType.GIVE_CREDIT
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(if (isCredit) R.string.give_credit else R.string.receive_payment),
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (t.voided) TextDecoration.LineThrough else null
                        )
                        Text(Formats.dateTime(t.timestamp), style = MaterialTheme.typography.bodySmall)
                        val note = listOf(t.description, t.reference).filter { it.isNotBlank() }.joinToString(" • ")
                        if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.bodySmall)
                        if (t.voided) {
                            Text(stringResource(R.string.cancelled), style = MaterialTheme.typography.labelSmall, color = OweRed)
                        } else {
                            Text(
                                stringResource(R.string.previous_balance) + " " + moneyText(r.before) + "  →  " +
                                    stringResource(R.string.new_balance) + " " + moneyText(r.after),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        (if (isCredit) "+" else "−") + moneyText(t.amount),
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCredit) OweRed else PayGreen,
                        textDecoration = if (t.voided) TextDecoration.LineThrough else null
                    )
                    if (!t.voided) {
                        IconButton(onClick = { voiding = t }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_entry))
                        }
                    }
                }
                HorizontalDivider()
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.opening_balance))
                    Text(moneyText(c.openingBalance))
                }
            }
        }
    }

    dialogType?.let { type ->
        TxDialog(type = type, onDismiss = { dialogType = null }) { amount, desc, ref ->
            vm.addTransaction(c, type, amount, desc, ref)
            dialogType = null
        }
    }
    if (showEdit) {
        CustomerDialog(customer = c, onDismiss = { showEdit = false }) { name, phone, address, _ ->
            vm.updateCustomer(c.id, name, phone, address)
            showEdit = false
        }
    }
    if (showRemove) {
        ConfirmDialog(
            title = stringResource(R.string.remove_customer_title),
            message = stringResource(R.string.remove_customer_msg),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { showRemove = false; vm.archiveCustomer(c.id); onBack() },
            onDismiss = { showRemove = false }
        )
    }
    voiding?.let { t ->
        ConfirmDialog(
            title = stringResource(R.string.cancel_entry),
            message = stringResource(R.string.cancel_entry_msg),
            confirmLabel = stringResource(R.string.ok),
            onConfirm = { vm.voidTransaction(t.id); voiding = null },
            onDismiss = { voiding = null }
        )
    }
}
