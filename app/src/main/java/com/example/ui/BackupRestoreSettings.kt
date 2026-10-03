package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.R
import com.example.data.backup.BackupCryptoException
import com.example.data.backup.TinyBackup
import com.example.security.AppLock
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private enum class BackupStep { NONE, CHOOSE_EXPORT_PASSWORD, CONFIRM_RESTORE, ENTER_RESTORE_PASSWORD, WORKING }

/** Settings card: save everything to a password-protected file, or restore from one. */
@Composable
fun BackupRestoreSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(BackupStep.NONE) }
    var exportPassword by remember { mutableStateOf<CharArray?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }

    val savedMsg = stringResource(R.string.backup_saved)
    val failedMsg = stringResource(R.string.backup_failed)
    val restoredMsg = stringResource(R.string.backup_restored)
    val wrongMsg = stringResource(R.string.backup_wrong_password)

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pw = exportPassword
        exportPassword = null
        if (uri == null || pw == null) { step = BackupStep.NONE; return@rememberLauncherForActivityResult }
        step = BackupStep.WORKING
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { TinyBackup.export(context, pw, it) }
                }.also { pw.fill(' ') }
            }
            step = BackupStep.NONE
            Toast.makeText(
                context,
                result.fold({ String.format(savedMsg, it.photos) }, { failedMsg }),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { step = BackupStep.NONE; return@rememberLauncherForActivityResult }
        restoreUri = uri
        restoreError = null
        step = BackupStep.ENTER_RESTORE_PASSWORD
    }

    Column(verticalArrangement = Arrangement.spacedBy(TinySpace.md)) {
        Text(stringResource(R.string.backup_explainer), style = TinyType.Caption)
        Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
            TinyButton(
                text = stringResource(R.string.backup_export),
                onClick = { step = BackupStep.CHOOSE_EXPORT_PASSWORD },
                modifier = Modifier.weight(1f),
                style = TinyButtonStyle.Secondary,
                icon = Icons.Rounded.Download,
                enabled = step != BackupStep.WORKING,
                testTag = "backup_export_button"
            )
            TinyButton(
                text = stringResource(R.string.backup_restore),
                onClick = { step = BackupStep.CONFIRM_RESTORE },
                modifier = Modifier.weight(1f),
                style = TinyButtonStyle.Outline,
                icon = Icons.Rounded.Restore,
                enabled = step != BackupStep.WORKING,
                testTag = "backup_restore_button"
            )
        }
        if (step == BackupStep.WORKING) Text(stringResource(R.string.backup_working), style = TinyType.Caption.copy(color = TinyColors.Rose))
    }

    when (step) {
        BackupStep.CHOOSE_EXPORT_PASSWORD -> PasswordDialog(
            title = stringResource(R.string.backup_password_title),
            hint = stringResource(R.string.backup_password_hint),
            confirm = true,
            error = null,
            onDismiss = { step = BackupStep.NONE }
        ) { pw ->
            exportPassword = pw
            AppLock.expectExternalActivity()
            createDoc.launch("TinyUs-${LocalDate.now()}.tinyus")
            step = BackupStep.NONE
        }
        BackupStep.CONFIRM_RESTORE -> LockDialogCard(onDismiss = { step = BackupStep.NONE }) {
            TinyDialogHeader(title = stringResource(R.string.backup_restore_confirm_title), icon = Icons.Rounded.Restore)
            Text(stringResource(R.string.backup_restore_confirm_body), style = TinyType.Body.copy(color = TinyColors.InkMuted))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TinySpace.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TinyButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { step = BackupStep.NONE },
                    style = TinyButtonStyle.Ghost
                )
                TinyButton(
                    text = stringResource(R.string.backup_choose_file),
                    onClick = {
                        AppLock.expectExternalActivity()
                        openDoc.launch(arrayOf("*/*"))
                    },
                    style = TinyButtonStyle.Primary,
                    testTag = "backup_choose_file"
                )
            }
        }
        BackupStep.ENTER_RESTORE_PASSWORD -> PasswordDialog(
            title = stringResource(R.string.backup_enter_password),
            hint = null,
            confirm = false,
            error = restoreError,
            onDismiss = { step = BackupStep.NONE; restoreUri = null }
        ) { pw ->
            val uri = restoreUri ?: return@PasswordDialog
            step = BackupStep.WORKING
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)!!.use { TinyBackup.restore(context, pw, it) }
                    }.also { pw.fill(' ') }
                }
                result.onSuccess {
                    Toast.makeText(context, restoredMsg, Toast.LENGTH_LONG).show()
                    // Rebuild every screen so nothing shows pre-restore data from memory.
                    context.findFragmentActivity()?.recreate()
                    step = BackupStep.NONE
                }.onFailure { e ->
                    restoreError = if (e is BackupCryptoException) e.message ?: wrongMsg else failedMsg
                    step = BackupStep.ENTER_RESTORE_PASSWORD
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    hint: String?,
    confirm: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onDone: (CharArray) -> Unit
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val longEnough = first.length >= TinyBackup.MIN_PASSWORD_LENGTH
    val matches = !confirm || first == second
    LockDialogCard(onDismiss) {
        TinyDialogHeader(title = title, subtitle = hint)
        OutlinedTextField(
            value = first, onValueChange = { first = it }, singleLine = true,
            label = { Text(stringResource(R.string.backup_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = TinyFieldShape,
            colors = tinyTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("backup_password_field")
        )
        if (confirm) {
            OutlinedTextField(
                value = second, onValueChange = { second = it }, singleLine = true,
                label = { Text(stringResource(R.string.backup_password_again)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = second.isNotEmpty() && !matches,
                shape = TinyFieldShape,
                colors = tinyTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("backup_password_confirm_field")
            )
        }
        if (error != null) Text(error, style = TinyType.Caption.copy(color = TinyColors.Rose))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TinySpace.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TinyButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                style = TinyButtonStyle.Ghost
            )
            TinyButton(
                text = stringResource(R.string.action_continue),
                onClick = { onDone(first.toCharArray()) },
                style = TinyButtonStyle.Primary,
                enabled = longEnough && matches,
                testTag = "backup_password_continue"
            )
        }
    }
}
