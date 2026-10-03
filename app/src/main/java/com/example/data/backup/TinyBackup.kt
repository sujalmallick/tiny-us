package com.example.data.backup

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * "Backup & Restore": everything the couple has made, in one password-protected file they keep
 * wherever they like. No account, no server — the file is written and read through the system
 * file picker.
 *
 * Inside the encryption is a ZIP with `manifest.json`, one JSON file per preferences store and the
 * polaroid images. The privacy-lock settings are deliberately not included: a restored phone
 * starts unlocked and the couple can set a new PIN.
 */
object TinyBackup {
    const val MIN_PASSWORD_LENGTH = 6
    private const val FORMAT = "tinyus-backup"
    private const val VERSION = 1
    private val PREF_FILES = listOf("tiny_us_prefs", "tiny_us_polaroids")
    private const val PHOTO_DIR = "polaroids"
    private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024

    data class Summary(val prefStores: Int, val photos: Int)

    fun export(context: Context, password: CharArray, out: OutputStream): Summary {
        var photos = 0
        ZipOutputStream(ChunkedCipher.encryptingStream(out, password)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(JSONObject().put("format", FORMAT).put("version", VERSION).put("createdAt", System.currentTimeMillis()).toString().toByteArray())
            zip.closeEntry()

            PREF_FILES.forEach { name ->
                zip.putNextEntry(ZipEntry("prefs/$name.json"))
                zip.write(prefsToJson(context.getSharedPreferences(name, Context.MODE_PRIVATE)).toString().toByteArray())
                zip.closeEntry()
            }

            File(context.filesDir, PHOTO_DIR).listFiles()?.filter { it.isFile }?.forEach { file ->
                zip.putNextEntry(ZipEntry("files/$PHOTO_DIR/${file.name}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                photos++
            }
        }
        return Summary(PREF_FILES.size, photos)
    }

    /**
     * Replaces the app's data with the backup. Everything is read and checked first; existing data
     * is only touched once the whole file has decrypted and parsed correctly.
     */
    fun restore(context: Context, password: CharArray, input: InputStream): Summary {
        val prefs = HashMap<String, JSONObject>()
        val staging = File(context.cacheDir, "restore-staging").apply { deleteRecursively(); mkdirs() }
        var manifestOk = false
        try {
            val plain = ChunkedCipher.decryptingStream(input, password)
            ZipInputStream(plain).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    when {
                        name == "manifest.json" -> {
                            val m = JSONObject(String(readLimited(zip)))
                            if (m.optString("format") != FORMAT || m.optInt("version") > VERSION) {
                                throw BackupCryptoException("This backup was made by a newer Tiny Us. Please update the app first.")
                            }
                            manifestOk = true
                        }
                        name.startsWith("prefs/") && name.endsWith(".json") -> {
                            val store = name.removePrefix("prefs/").removeSuffix(".json")
                            if (store in PREF_FILES) prefs[store] = JSONObject(String(readLimited(zip)))
                        }
                        name.startsWith("files/$PHOTO_DIR/") -> {
                            val fileName = name.substringAfterLast('/')
                            // Only plain file names: never let an entry escape the staging folder.
                            if (fileName.isNotEmpty() && fileName == File(fileName).name && !fileName.startsWith(".")) {
                                File(staging, fileName).outputStream().use { out -> copyLimited(zip, out) }
                            }
                        }
                    }
                    zip.closeEntry()
                }
                // The ZIP reader can stop early; read to the end so the final sealed chunk is verified.
                val sink = ByteArray(8 * 1024)
                while (plain.read(sink) >= 0) Unit
            }
            if (!manifestOk) throw BackupCryptoException("Not a Tiny Us backup")

            // Commit: photos first, then preferences (with photo paths pointing at this phone).
            val photoDir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
            photoDir.listFiles()?.forEach { it.delete() }
            val restoredPhotos = staging.listFiles()?.count { it.renameTo(File(photoDir, it.name)) || it.copyToAndDelete(File(photoDir, it.name)) } ?: 0
            PREF_FILES.forEach { name ->
                val json = prefs[name] ?: JSONObject()
                val target = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                jsonToPrefs(if (name == "tiny_us_polaroids") remapPhotoPaths(json, photoDir) else json, target)
            }
            return Summary(prefs.size, restoredPhotos)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun File.copyToAndDelete(target: File): Boolean = runCatching {
        copyTo(target, overwrite = true); delete(); true
    }.getOrDefault(false)

    private fun readLimited(input: InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        copyLimited(input, out)
        return out.toByteArray()
    }

    private fun copyLimited(input: InputStream, out: OutputStream) {
        val buf = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_ENTRY_BYTES) throw BackupCryptoException("Backup file is damaged")
            out.write(buf, 0, n)
        }
    }

    /** Typed so ints stay ints and string sets stay sets on restore. */
    internal fun prefsToJson(prefs: SharedPreferences): JSONObject {
        val obj = JSONObject()
        prefs.all.forEach { (key, value) ->
            val typed = when (value) {
                is String -> JSONObject().put("t", "s").put("v", value)
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is Set<*> -> JSONObject().put("t", "ss").put("v", JSONArray(value.filterIsInstance<String>()))
                else -> null
            }
            if (typed != null) obj.put(key, typed)
        }
        return obj
    }

    internal fun jsonToPrefs(obj: JSONObject, prefs: SharedPreferences) {
        val editor = prefs.edit().clear()
        obj.keys().forEach { key ->
            val typed = obj.optJSONObject(key) ?: return@forEach
            when (typed.optString("t")) {
                "s" -> editor.putString(key, typed.optString("v"))
                "i" -> editor.putInt(key, typed.optInt("v"))
                "l" -> editor.putLong(key, typed.optLong("v"))
                "f" -> editor.putFloat(key, typed.optDouble("v").toFloat())
                "b" -> editor.putBoolean(key, typed.optBoolean("v"))
                "ss" -> {
                    val arr = typed.optJSONArray("v") ?: JSONArray()
                    editor.putStringSet(key, (0 until arr.length()).map { arr.optString(it) }.toSet())
                }
            }
        }
        editor.commit()
    }

    /** Polaroid records store absolute paths; point them at this phone's photo folder. */
    private fun remapPhotoPaths(store: JSONObject, photoDir: File): JSONObject {
        val typed = store.optJSONObject("polaroids_json") ?: return store
        val arr = runCatching { JSONArray(typed.optString("v")) }.getOrNull() ?: return store
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val name = item.optString("imagePath").substringAfterLast('/').substringAfterLast('\\')
            if (name.isNotEmpty()) item.put("imagePath", File(photoDir, name).absolutePath)
        }
        typed.put("v", arr.toString())
        return store
    }
}
