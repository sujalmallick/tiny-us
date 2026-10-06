@file:OptIn(ExperimentalForeignApi::class)

package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.IosUserDefaultsStorage
import com.example.data.backup.BackupCipher
import com.example.data.backup.BackupContent
import com.example.data.backup.BackupError
import com.example.data.backup.toByteArray
import com.example.data.backup.toNSData
import kotlin.time.Clock
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.* // NSData and NSFileManager category methods

/**
 * The document picker, which only Swift presents: the Swift app sets [presenter] at launch, and
 * calls [fileSaved] / [filePicked] when the couple is done.
 */
interface FilePickerPresenter {
    /** Offers the file at [path] to be saved wherever the couple likes (Files, iCloud Drive, AirDrop...). */
    fun presentSave(path: String)

    /** Lets the couple pick a file to open. */
    fun presentOpen()
}

object IosFilePickers {
    var presenter: FilePickerPresenter? = null
    internal var onSaved: ((Boolean) -> Unit)? = null
    internal var onPicked: ((NSData?) -> Unit)? = null

    fun fileSaved(ok: Boolean) {
        onSaved?.invoke(ok)
        onSaved = null
    }

    fun filePicked(data: NSData?) {
        onPicked?.invoke(data)
        onPicked = null
    }
}

/** A short note over the app (iOS has no toast), and the app's rebuild after a restore. */
object IosAppShell {
    var message by mutableStateOf<String?>(null)

    /** Bumped to rebuild every screen with freshly read data. */
    var generation by mutableIntStateOf(0)
}

/**
 * Backup & Restore on iOS: the same backup file as Android (see [BackupContent], [BackupCipher]),
 * made from the App Group defaults and the Polaroids in Documents/polaroids.
 */
class IosBackupFiles(private val storage: IosUserDefaultsStorage) : BackupFiles {
    override val isApple: Boolean get() = true

    private var chosen: ByteArray? = null

    private val photoDir: String by lazy {
        val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        "$docs/${BackupContent.PHOTO_DIR}".also {
            NSFileManager.defaultManager.createDirectoryAtPath(it, withIntermediateDirectories = true, attributes = null, error = null)
        }
    }

    override fun export(password: CharArray, fileName: String, onDone: (Result<Int>?) -> Unit) {
        val presenter = IosFilePickers.presenter ?: return onDone(Result.failure(BackupError("Backup isn't available")))
        val made = runCatching {
            val photos = photos()
            val contents = BackupContent.Contents(splitStores(storage.snapshot(LONG_KEYS)), photos)
            val file = BackupCipher.seal(BackupContent.build(contents, Clock.System.now().toEpochMilliseconds()), password)
            val path = "${NSTemporaryDirectory()}$fileName"
            if (!file.toNSData().writeToFile(path, atomically = true)) throw BackupError("Backup couldn't be written")
            path to photos.size
        }
        val (path, photoCount) = made.getOrElse { return onDone(Result.failure(it)) }
        IosFilePickers.onSaved = { ok ->
            NSFileManager.defaultManager.removeItemAtPath(path, error = null)
            onDone(if (ok) Result.success(photoCount) else null)
        }
        presenter.presentSave(path)
    }

    override fun chooseFile(onChosen: (Boolean) -> Unit) {
        val presenter = IosFilePickers.presenter ?: return onChosen(false)
        IosFilePickers.onPicked = { data ->
            chosen = data?.toByteArray()
            onChosen(chosen != null)
        }
        presenter.presentOpen()
    }

    override suspend fun restore(password: CharArray): Result<Unit> {
        val file = chosen ?: return Result.failure(BackupError("Not a Tiny Us backup"))
        return withContext(Dispatchers.Default) {
            runCatching {
                // Everything is read and checked first; the data is only replaced once it all opened.
                val contents = BackupContent.parse(BackupCipher.open(file, password))
                withContext(Dispatchers.Main) {
                    val manager = NSFileManager.defaultManager
                    (manager.contentsOfDirectoryAtPath(photoDir, error = null) ?: emptyList<Any?>()).forEach { name ->
                        manager.removeItemAtPath("$photoDir/$name", error = null)
                    }
                    contents.photos.forEach { (name, bytes) -> bytes.toNSData().writeToFile("$photoDir/$name", atomically = true) }
                    val merged = LinkedHashMap<String, Any>()
                    contents.prefs["tiny_us_prefs"]?.let { merged.putAll(it) }
                    contents.prefs["tiny_us_polaroids"]?.let { merged.putAll(BackupContent.remapPhotoPaths(it, photoDir)) }
                    storage.replaceAll(merged)
                }
                chosen = null
            }
        }
    }

    override fun showMessage(text: String) {
        IosAppShell.message = text
    }

    override fun reload() {
        IosAppShell.generation++
    }

    private fun photos(): Map<String, ByteArray> {
        val names = NSFileManager.defaultManager.contentsOfDirectoryAtPath(photoDir, error = null).orEmpty()
        return names.mapNotNull { it as? String }.filter { !it.startsWith(".") }.mapNotNull { name ->
            NSData.dataWithContentsOfFile("$photoDir/$name")?.let { name to it.toByteArray() }
        }.toMap()
    }

    /** Android keeps the Polaroid list in its own file; on iOS it shares the App Group defaults. */
    private fun splitStores(all: Map<String, Any>): Map<String, Map<String, Any>> {
        val (polaroids, prefs) = all.entries.partition { it.key in POLAROID_KEYS }
        return mapOf(
            "tiny_us_prefs" to prefs.associate { it.key to it.value },
            "tiny_us_polaroids" to polaroids.associate { it.key to it.value }
        )
    }

    private companion object {
        val POLAROID_KEYS = setOf("polaroids_json", "used_titles")

        /** Saved as whole numbers on iOS, but longs on Android (where reading them as ints would fail). */
        val LONG_KEYS = setOf("tiny_care_next_trigger", "mood_last_updated")
    }
}
