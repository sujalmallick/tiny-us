package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.data.CoupleDates
import com.example.data.backup.BackupContent
import com.example.data.backup.BackupError
import com.example.resources.*
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The platform's side of Backup & Restore: its file picker and its saved data. Android writes
 * through the system file picker with its streaming TinyBackup; iOS through the document picker
 * with the shared backup format. Both write the same file, so a backup moves between phones.
 */
interface BackupFiles {
    /** True on iPhone and iPad (the wording says "device" rather than "phone"). */
    val isApple: Boolean get() = false

    /**
     * Asks where to save, then writes the backup protected by [password]. [onDone] gets the number
     * of photos saved, a failure, or null when the couple cancelled.
     */
    fun export(password: CharArray, fileName: String, onDone: (Result<Int>?) -> Unit)

    /** Asks for a backup file to restore; [onChosen] gets false when cancelled. */
    fun chooseFile(onChosen: (Boolean) -> Unit)

    /** Restores the chosen file with [password]; a [BackupError] says what's wrong with it. */
    suspend fun restore(password: CharArray): Result<Unit>

    fun showMessage(text: String)

    /** Rebuilds every screen, so nothing shows the data from before the restore. */
    fun reload()
}

private enum class BackupStep { NONE, CHOOSE_EXPORT_PASSWORD, CONFIRM_RESTORE, ENTER_RESTORE_PASSWORD, WORKING }

/** Settings card: save everything to a password-protected file, or restore from one. */
@Composable
fun BackupRestoreSettings(files: BackupFiles) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(BackupStep.NONE) }
    var restoreError by remember { mutableStateOf<String?>(null) }

    val failedMsg = stringResource(Res.string.backup_failed)
    val restoredMsg = stringResource(Res.string.backup_restored)
    val wrongMsg = stringResource(Res.string.backup_wrong_password)
    val savedTemplate = stringResource(Res.string.backup_saved)

    Column(verticalArrangement = Arrangement.spacedBy(TinySpace.md)) {
        Text(stringResource(Res.string.backup_explainer), style = TinyType.Caption)
        Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
            TinyButton(
                text = stringResource(Res.string.backup_export),
                onClick = { step = BackupStep.CHOOSE_EXPORT_PASSWORD },
                modifier = Modifier.weight(1f),
                style = TinyButtonStyle.Secondary,
                icon = PixelIcons.Download,
                enabled = step != BackupStep.WORKING,
                testTag = "backup_export_button"
            )
            TinyButton(
                text = stringResource(Res.string.backup_restore),
                onClick = { step = BackupStep.CONFIRM_RESTORE },
                modifier = Modifier.weight(1f),
                style = TinyButtonStyle.Outline,
                icon = PixelIcons.Restore,
                enabled = step != BackupStep.WORKING,
                testTag = "backup_restore_button"
            )
        }
        if (step == BackupStep.WORKING) Text(stringResource(Res.string.backup_working), style = TinyType.Caption.copy(color = TinyColors.Rose))
    }

    when (step) {
        BackupStep.CHOOSE_EXPORT_PASSWORD -> PasswordDialog(
            title = stringResource(Res.string.backup_password_title),
            hint = stringResource(Res.string.backup_password_hint),
            confirm = true,
            error = null,
            onDismiss = { step = BackupStep.NONE }
        ) { pw ->
            step = BackupStep.WORKING
            files.export(pw, "TinyUs-${CoupleDates.today()}.tinyus") { result ->
                pw.fill(' ')
                step = BackupStep.NONE
                if (result != null) {
                    files.showMessage(result.fold({ savedTemplate.replace("%1\$d", it.toString()) }, { failedMsg }))
                }
            }
        }
        BackupStep.CONFIRM_RESTORE -> LockDialogCard(onDismiss = { step = BackupStep.NONE }) {
            TinyDialogHeader(title = stringResource(Res.string.backup_restore_confirm_title), icon = PixelIcons.Restore)
            Text(
                stringResource(if (files.isApple) Res.string.backup_restore_confirm_body_ios else Res.string.backup_restore_confirm_body),
                style = TinyType.Body.copy(color = TinyColors.InkMuted)
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TinySpace.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TinyButton(
                    text = stringResource(Res.string.action_cancel),
                    onClick = { step = BackupStep.NONE },
                    style = TinyButtonStyle.Ghost
                )
                TinyButton(
                    text = stringResource(Res.string.backup_choose_file),
                    onClick = {
                        files.chooseFile { chosen ->
                            restoreError = null
                            step = if (chosen) BackupStep.ENTER_RESTORE_PASSWORD else BackupStep.NONE
                        }
                    },
                    style = TinyButtonStyle.Primary,
                    testTag = "backup_choose_file"
                )
            }
        }
        BackupStep.ENTER_RESTORE_PASSWORD -> PasswordDialog(
            title = stringResource(Res.string.backup_enter_password),
            hint = null,
            confirm = false,
            error = restoreError,
            onDismiss = { step = BackupStep.NONE }
        ) { pw ->
            step = BackupStep.WORKING
            scope.launch {
                val result = files.restore(pw).also { pw.fill(' ') }
                result.onSuccess {
                    files.showMessage(restoredMsg)
                    step = BackupStep.NONE
                    files.reload()
                }.onFailure { e ->
                    restoreError = if (e is BackupError) e.message ?: wrongMsg else failedMsg
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
    val longEnough = first.length >= BackupContent.MIN_PASSWORD_LENGTH
    val matches = !confirm || first == second
    LockDialogCard(onDismiss) {
        TinyDialogHeader(title = title, subtitle = hint)
        OutlinedTextField(
            value = first, onValueChange = { first = it }, singleLine = true,
            label = { Text(stringResource(Res.string.backup_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = TinyFieldShape,
            colors = tinyTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("backup_password_field")
        )
        if (confirm) {
            OutlinedTextField(
                value = second, onValueChange = { second = it }, singleLine = true,
                label = { Text(stringResource(Res.string.backup_password_again)) },
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
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
                style = TinyButtonStyle.Ghost
            )
            TinyButton(
                text = stringResource(Res.string.action_continue),
                onClick = { onDone(first.toCharArray()) },
                style = TinyButtonStyle.Primary,
                enabled = longEnough && matches,
                testTag = "backup_password_continue"
            )
        }
    }
}
