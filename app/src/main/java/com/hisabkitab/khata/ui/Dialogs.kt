package com.hisabkitab.khata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisabkitab.khata.R
import com.hisabkitab.khata.data.CustomerBalance
import com.hisabkitab.khata.data.Money
import com.hisabkitab.khata.data.Shop
import com.hisabkitab.khata.data.TxType

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun ShopDialog(shop: Shop?, onDismiss: () -> Unit, onSave: (Shop) -> Unit) {
    var name by remember { mutableStateOf(shop?.name ?: "") }
    var owner by remember { mutableStateOf(shop?.ownerName ?: "") }
    var phone by remember { mutableStateOf(shop?.phone ?: "") }
    var address by remember { mutableStateOf(shop?.address ?: "") }
    var notes by remember { mutableStateOf(shop?.notes ?: "") }
    var opening by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val openingVal = Money.parseSigned(opening)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (shop == null) R.string.add_shop else R.string.edit_shop)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(name, { name = it }, stringResource(R.string.shop_name),
                    isError = tried && name.isBlank(),
                    supporting = if (tried && name.isBlank()) stringResource(R.string.name_required) else null)
                Field(owner, { owner = it }, stringResource(R.string.owner_name))
                Field(phone, { phone = it }, stringResource(R.string.phone), KeyboardType.Phone)
                Field(address, { address = it }, stringResource(R.string.address))
                if (shop == null) {
                    Field(opening, { opening = it }, stringResource(R.string.opening_balance), KeyboardType.Decimal,
                        isError = openingVal == null,
                        supporting = if (openingVal == null) stringResource(R.string.invalid_amount) else null)
                }
                Field(notes, { notes = it }, stringResource(R.string.notes), singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (name.isNotBlank() && (shop != null || openingVal != null)) {
                    val base = shop ?: Shop(name = "")
                    onSave(
                        base.copy(
                            name = name.trim(), ownerName = owner.trim(), phone = phone.trim(),
                            address = address.trim(), notes = notes.trim(),
                            openingBalance = if (shop == null) (openingVal ?: 0L) else shop.openingBalance
                        )
                    )
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun CustomerDialog(
    customer: CustomerBalance?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, opening: Long) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var opening by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val openingVal = Money.parseSigned(opening)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (customer == null) R.string.add_customer else R.string.edit_customer)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(name, { name = it }, stringResource(R.string.customer_name),
                    isError = tried && name.isBlank(),
                    supporting = if (tried && name.isBlank()) stringResource(R.string.name_required) else null)
                Field(phone, { phone = it }, stringResource(R.string.phone), KeyboardType.Phone)
                Field(address, { address = it }, stringResource(R.string.address))
                if (customer == null) {
                    Field(opening, { opening = it }, stringResource(R.string.opening_balance), KeyboardType.Decimal,
                        isError = openingVal == null,
                        supporting = if (openingVal == null) stringResource(R.string.invalid_amount)
                        else stringResource(R.string.opening_balance_hint))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (name.isNotBlank() && (customer != null || openingVal != null)) {
                    onSave(name.trim(), phone.trim(), address.trim(), openingVal ?: 0L)
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun TxDialog(type: TxType, onDismiss: () -> Unit, onSave: (amount: Long, description: String, reference: String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val parsed = Money.parsePositive(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (type == TxType.GIVE_CREDIT) R.string.give_credit else R.string.receive_payment))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(amount, { amount = it }, stringResource(R.string.amount), KeyboardType.Decimal,
                    isError = tried && parsed == null,
                    supporting = if (tried && parsed == null) stringResource(R.string.invalid_amount) else null)
                Field(description, { description = it }, stringResource(R.string.description))
                Field(reference, { reference = it }, stringResource(R.string.reference))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (parsed != null) onSave(parsed, description, reference)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
