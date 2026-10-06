package com.example.data.backup

import com.example.data.JSONArray
import com.example.data.JSONObject

/**
 * What's inside a backup, the same as Android's TinyBackup writes it: `manifest.json`, one typed
 * JSON file per preferences store (`prefs/<name>.json`) and the Polaroid images
 * (`files/polaroids/<file>`). The app lock is deliberately left out: a restored phone starts
 * unlocked.
 */
object BackupContent {
    const val MIN_PASSWORD_LENGTH = 6
    const val FORMAT = "tinyus-backup"
    const val VERSION = 1
    val PREF_FILES = listOf("tiny_us_prefs", "tiny_us_polaroids")
    const val PHOTO_DIR = "polaroids"

    /** A backup's saved data: each store's values (String, Int, Long, Float, Boolean, Set<String>) and the photos by file name. */
    class Contents(val prefs: Map<String, Map<String, Any>>, val photos: Map<String, ByteArray>)

    fun build(contents: Contents, createdAtMillis: Long): ByteArray {
        val entries = mutableListOf<Pair<String, ByteArray>>()
        entries += "manifest.json" to JSONObject().put("format", FORMAT).put("version", VERSION).put("createdAt", createdAtMillis).toString().encodeToByteArray()
        PREF_FILES.forEach { name ->
            entries += "prefs/$name.json" to prefsToJson(contents.prefs[name].orEmpty()).toString().encodeToByteArray()
        }
        contents.photos.forEach { (file, bytes) -> entries += "files/$PHOTO_DIR/$file" to bytes }
        return TinyZip.write(entries)
    }

    fun parse(zip: ByteArray): Contents {
        var manifestOk = false
        val prefs = HashMap<String, Map<String, Any>>()
        val photos = LinkedHashMap<String, ByteArray>()
        TinyZip.read(zip).forEach { (name, data) ->
            when {
                name == "manifest.json" -> {
                    val m = runCatching { JSONObject(data.decodeToString()) }.getOrNull() ?: throw BackupError("Backup file is damaged")
                    if (m.optString("format") != FORMAT || m.optInt("version") > VERSION) {
                        throw BackupError("This backup was made by a newer Tiny Us. Please update the app first.")
                    }
                    manifestOk = true
                }
                name.startsWith("prefs/") && name.endsWith(".json") -> {
                    val store = name.removePrefix("prefs/").removeSuffix(".json")
                    if (store in PREF_FILES) prefs[store] = jsonToPrefs(JSONObject(data.decodeToString()))
                }
                name.startsWith("files/$PHOTO_DIR/") -> {
                    val file = name.substringAfterLast('/')
                    // Only plain file names: never let an entry escape the photo folder.
                    if (file.isNotEmpty() && !file.startsWith(".") && '\' !in file) photos[file] = data
                }
            }
        }
        if (!manifestOk) throw BackupError("Not a Tiny Us backup")
        return Contents(prefs, photos)
    }

    /** Typed so ints stay ints and string sets stay sets on restore (Android's TinyBackup format). */
    fun prefsToJson(values: Map<String, Any>): JSONObject {
        val obj = JSONObject()
        values.forEach { (key, value) ->
            val typed = when (value) {
                is String -> JSONObject().put("t", "s").put("v", value)
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is Set<*> -> JSONObject().put("t", "ss").put("v", JSONArray().apply { value.filterIsInstance<String>().forEach { put(it) } })
                else -> null
            }
            if (typed != null) obj.put(key, typed)
        }
        return obj
    }

    fun jsonToPrefs(obj: JSONObject): Map<String, Any> {
        val out = LinkedHashMap<String, Any>()
        obj.keys().forEach { key ->
            val typed = obj.optJSONObject(key) ?: return@forEach
            out[key] = when (typed.optString("t")) {
                "s" -> typed.optString("v")
                "i" -> typed.optInt("v")
                "l" -> typed.optLong("v")
                "f" -> typed.optDouble("v").toFloat()
                "b" -> typed.optBoolean("v")
                "ss" -> typed.optJSONArray("v")?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.toSet() } ?: emptySet<String>()
                else -> return@forEach
            }
        }
        return out
    }

    /** Polaroid records store absolute paths; point them at [photoDir] on this phone. */
    fun remapPhotoPaths(values: Map<String, Any>, photoDir: String): Map<String, Any> {
        val raw = values["polaroids_json"] as? String ?: return values
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return values
        val fixed = JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val name = item.optString("imagePath").substringAfterLast('/').substringAfterLast('\')
            if (name.isNotEmpty()) item.set("imagePath", "$photoDir/$name")
            fixed.put(item)
        }
        return values + ("polaroids_json" to fixed.toString())
    }
}
