package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.data.backup.BackupCryptoException
import com.example.data.backup.BackupError
import com.example.data.backup.TinyBackup
import com.example.security.AppLock
import com.example.security.findFragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Backup & Restore on Android: the system file picker and the streaming [TinyBackup], as before. */
@Composable
fun rememberAndroidBackupFiles(): BackupFiles {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = remember(context) { AndroidBackupFiles(context, scope) }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { files.onExportTarget(it) }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { files.onRestoreFile(it) }
    files.launchCreate = { name -> createDoc.launch(name) }
    files.launchOpen = { openDoc.launch(arrayOf("*/*")) }
    return files
}

private class AndroidBackupFiles(private val context: Context, private val scope: CoroutineScope) : BackupFiles {
    var launchCreate: (String) -> Unit = {}
    var launchOpen: () -> Unit = {}
    private var pendingExport: Pair<CharArray, (Result<Int>?) -> Unit>? = null
    private var pendingChoice: ((Boolean) -> Unit)? = null
    private var restoreUri: Uri? = null

    override fun export(password: CharArray, fileName: String, onDone: (Result<Int>?) -> Unit) {
        pendingExport = password.copyOf() to onDone
        AppLock.expectExternalActivity()
        launchCreate(fileName)
    }

    fun onExportTarget(uri: Uri?) {
        val (pw, onDone) = pendingExport ?: return
        pendingExport = null
        if (uri == null) {
            pw.fill(' ')
            onDone(null)
            return
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { TinyBackup.export(context, pw, it).photos }
                }.also { pw.fill(' ') }
            }
            onDone(result)
        }
    }

    override fun chooseFile(onChosen: (Boolean) -> Unit) {
        pendingChoice = onChosen
        AppLock.expectExternalActivity()
        launchOpen()
    }

    fun onRestoreFile(uri: Uri?) {
        restoreUri = uri
        pendingChoice?.invoke(uri != null)
        pendingChoice = null
    }

    override suspend fun restore(password: CharArray): Result<Unit> {
        val uri = restoreUri ?: return Result.failure(BackupError("Not a Tiny Us backup"))
        return withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)!!.use { TinyBackup.restore(context, password, it) }
                Unit
            }.recoverCatching { e -> throw if (e is BackupCryptoException) BackupError(e.message ?: "") else e }
        }
    }

    override fun showMessage(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    /** Rebuild every screen so nothing shows pre-restore data from memory. */
    override fun reload() {
        context.findFragmentActivity()?.recreate()
    }
}
