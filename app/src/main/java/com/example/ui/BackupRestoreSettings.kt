package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.backup.BackupCryptoException
import com.example.data.backup.TinyBackup
import com.example.security.AppLock
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepRose
import com.example.ui.theme.SageGreen
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.backup_explainer), fontSize = 12.sp, color = DarkSlate.copy(alpha = 0.7f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { step = BackupStep.CHOOSE_EXPORT_PASSWORD },
                enabled = step != BackupStep.WORKING,
                modifier = Modifier.weight(1f).testTag("backup_export_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.backup_export), fontSize = 12.5.sp) }
            Button(
                onClick = { step = BackupStep.CONFIRM_RESTORE },
                enabled = step != BackupStep.WORKING,
                modifier = Modifier.weight(1f).testTag("backup_restore_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSlate.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.backup_restore), fontSize = 12.5.sp) }
        }
        if (step == BackupStep.WORKING) Text(stringResource(R.string.backup_working), fontSize = 12.sp, color = DeepRose)
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
            Text(stringResource(R.string.backup_restore_confirm_title), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkSlate)
            Text(stringResource(R.string.backup_restore_confirm_body), fontSize = 13.sp, color = DarkSlate.copy(alpha = 0.75f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.action_cancel), color = DarkSlate.copy(alpha = 0.6f), modifier = Modifier.clickable { step = BackupStep.NONE }.padding(10.dp))
                Text(
                    stringResource(R.string.backup_choose_file), color = DeepRose, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        AppLock.expectExternalActivity()
                        openDoc.launch(arrayOf("*/*"))
                    }.padding(10.dp).testTag("backup_choose_file")
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
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkSlate)
        if (hint != null) Text(hint, fontSize = 12.sp, color = DarkSlate.copy(alpha = 0.7f))
        OutlinedTextField(
            value = first, onValueChange = { first = it }, singleLine = true,
            label = { Text(stringResource(R.string.backup_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag("backup_password_field")
        )
        if (confirm) {
            OutlinedTextField(
                value = second, onValueChange = { second = it }, singleLine = true,
                label = { Text(stringResource(R.string.backup_password_again)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = second.isNotEmpty() && !matches,
                modifier = Modifier.fillMaxWidth().testTag("backup_password_confirm_field")
            )
        }
        if (error != null) Text(error, fontSize = 12.sp, color = DeepRose)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(stringResource(R.string.action_cancel), color = DarkSlate.copy(alpha = 0.6f), modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp))
            Text(
                stringResource(R.string.action_continue),
                color = if (longEnough && matches) DeepRose else Color.Gray,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(enabled = longEnough && matches) { onDone(first.toCharArray()) }.padding(10.dp).testTag("backup_password_continue")
            )
        }
    }
}
