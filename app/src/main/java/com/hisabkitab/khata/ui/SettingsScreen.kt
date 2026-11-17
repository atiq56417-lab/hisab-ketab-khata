package com.hisabkitab.khata.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.hisabkitab.khata.KhataApp
import com.hisabkitab.khata.R
import com.hisabkitab.khata.data.Formats
import kotlinx.coroutines.launch
import android.net.Uri
import kotlin.system.exitProcess

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as KhataApp
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val theme by vm.theme.collectAsState()
    val lang = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "ur"

    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var restoreDone by remember { mutableStateOf(false) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = app.backup.export(uri).isSuccess
            snackbar.showSnackbar(ctx.getString(if (ok) R.string.backup_ok else R.string.backup_fail))
        }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    val version = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
            RadioRow(stringResource(R.string.lang_urdu), lang == "ur") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ur"))
            }
            RadioRow(stringResource(R.string.lang_english), lang == "en") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
            RadioRow(stringResource(R.string.theme_system), theme == "system") { vm.setTheme("system") }
            RadioRow(stringResource(R.string.theme_light), theme == "light") { vm.setTheme("light") }
            RadioRow(stringResource(R.string.theme_dark), theme == "dark") { vm.setTheme("dark") }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.settings_backup), style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = { createLauncher.launch("hisab_kitab_backup_${Formats.fileStamp()}.db") },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.backup_create)) }
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.backup_restore)) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.app_name))
            Text(stringResource(R.string.app_version) + ": " + version)
        }
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.restore_confirm_title),
            message = stringResource(R.string.restore_confirm_msg),
            confirmLabel = stringResource(R.string.ok),
            onConfirm = {
                pendingRestore = null
                scope.launch {
                    val result = app.backup.restore(uri)
                    if (result.isSuccess) restoreDone = true
                    else snackbar.showSnackbar(ctx.getString(R.string.restore_invalid))
                }
            },
            onDismiss = { pendingRestore = null }
        )
    }
    if (restoreDone) {
        AlertDialog(
            onDismissRequest = {},
            text = { Text(stringResource(R.string.restore_done)) },
            confirmButton = { TextButton(onClick = { exitProcess(0) }) { Text(stringResource(R.string.ok)) } }
        )
    }
}
