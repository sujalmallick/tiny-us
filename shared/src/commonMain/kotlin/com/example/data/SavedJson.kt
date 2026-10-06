package com.example.data

/**
 * The part of Android's org.json that the saved lists use (memories, notes, dreams, adventures,
 * moments, mini-games, signals), in common code so iOS reads and writes the same data. It writes
 * exactly what org.json wrote (keys in insertion order, "/" escaped as "\/"), and reads values the
 * same forgiving way (numbers as strings, "true" as a boolean), so existing installs keep their data.
 */
internal object JsonNull {
    override fun toString() = "null"
}

internal class JSONException(message: String) : Exception(message)

internal class JSONObject() {
    private val values = LinkedHashMap<String, Any>()

    constructor(json: String) : this() {
        val parsed = JsonReader(json).readTop()
        if (parsed !is JSONObject) throw JSONException("Not a JSON object")
        values.putAll(parsed.values)
    }

    internal fun set(name: String, value: Any) {
        values[name] = value
    }

    fun put(name: String, value: String): JSONObject = apply { values[name] = value }
    fun put(name: String, value: Int): JSONObject = apply { values[name] = value }
    fun put(name: String, value: Long): JSONObject = apply { values[name] = value }
    fun put(name: String, value: Boolean): JSONObject = apply { values[name] = value }
    fun put(name: String, value: JSONArray): JSONObject = apply { values[name] = value }
    fun put(name: String, value: JSONObject): JSONObject = apply { values[name] = value }

    fun has(name: String): Boolean = name in values
    fun isNull(name: String): Boolean = values[name].let { it == null || it == JsonNull }

    private fun get(name: String): Any = values[name] ?: throw JSONException("No value for $name")

    fun getString(name: String): String = get(name).toString()
    fun optString(name: String, fallback: String = ""): String = values[name]?.toString() ?: fallback

    fun getLong(name: String): Long = asLong(get(name)) ?: throw JSONException("$name is not a number")
    fun getInt(name: String): Int = asLong(get(name))?.toInt() ?: throw JSONException("$name is not a number")
    fun optLong(name: String, fallback: Long = 0L): Long = values[name]?.let(::asLong) ?: fallback
    fun optBoolean(name: String, fallback: Boolean = false): Boolean = values[name]?.let(::asBoolean) ?: fallback

    fun optInt(name: String, fallback: Int = 0): Int = values[name]?.let(::asLong)?.toInt() ?: fallback

    fun getJSONArray(name: String): JSONArray = get(name) as? JSONArray ?: throw JSONException("$name is not an array")
    fun optJSONArray(name: String): JSONArray? = values[name] as? JSONArray
    fun optJSONObject(name: String): JSONObject? = values[name] as? JSONObject

    /** The names in this object, in the order they were added. */
    fun keys(): Iterator<String> = values.keys.toList().iterator()

    override fun toString(): String = buildString {
        append('{')
        values.entries.forEachIndexed { i, (k, v) ->
            if (i > 0) append(',')
            appendQuoted(k)
            append(':')
            appendValue(v)
        }
        append('}')
    }
}

internal class JSONArray() {
    private val values = ArrayList<Any>()

    constructor(json: String) : this() {
        val parsed = JsonReader(json).readTop()
        if (parsed !is JSONArray) throw JSONException("Not a JSON array")
        values.addAll(parsed.values)
    }

    internal fun add(value: Any) {
        values += value
    }

    fun put(value: String): JSONArray = apply { values += value }
    fun put(value: JSONObject): JSONArray = apply { values += value }

    fun length(): Int = values.size

    fun getJSONObject(index: Int): JSONObject =
        values.getOrNull(index) as? JSONObject ?: throw JSONException("Item $index is not an object")

    fun getString(index: Int): String = (values.getOrNull(index) ?: throw JSONException("No item $index")).toString()
    fun optString(index: Int, fallback: String = ""): String = values.getOrNull(index)?.toString() ?: fallback
    fun optJSONObject(index: Int): JSONObject? = values.getOrNull(index) as? JSONObject

    override fun toString(): String = buildString {
        append('[')
        values.forEachIndexed { i, v ->
            if (i > 0) append(',')
            appendValue(v)
        }
        append(']')
    }
}

private fun asLong(value: Any): Long? = when (value) {
    is Long -> value
    is Int -> value.toLong()
    is Double -> value.toLong()
    is String -> value.toLongOrNull() ?: value.toDoubleOrNull()?.toLong()
    else -> null
}

private fun asBoolean(value: Any): Boolean? = when (value) {
    is Boolean -> value
    is String -> when (value.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }
    else -> null
}

private fun StringBuilder.appendValue(value: Any) {
    when (value) {
        is String -> appendQuoted(value)
        else -> append(value.toString())
    }
}

/** Quotes like org.json: the usual escapes, "/" as "\/", other control characters as \u00XX. */
private fun StringBuilder.appendQuoted(s: String) {
    append('"')
    for (c in s) {
        when (c) {
            '"', '\\', '/' -> append('\\').append(c)
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\u000C' -> append("\\f")
            else -> if (c.code <= 0x1F) {
                append("\\u")
                append(c.code.toString(16).padStart(4, '0'))
            } else {
                append(c)
            }
        }
    }
    append('"')
}

/** A small, strict JSON reader producing [JSONObject], [JSONArray], strings, numbers, booleans and [JsonNull]. */
private class JsonReader(private val s: String) {
    private var i = 0

    fun readTop(): Any {
        val v = readValue()
        skipSpace()
        if (i != s.length) throw JSONException("Unexpected text at $i")
        return v
    }

    private fun skipSpace() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun expect(c: Char) {
        skipSpace()
        if (i >= s.length || s[i] != c) throw JSONException("Expected '$c' at $i")
        i++
    }

    private fun readValue(): Any {
        skipSpace()
        if (i >= s.length) throw JSONException("Unexpected end")
        return when (val c = s[i]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> readString()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", JsonNull)
            else -> if (c == '-' || c.isDigit()) readNumber() else throw JSONException("Unexpected '$c' at $i")
        }
    }

    private fun literal(word: String, value: Any): Any {
        if (!s.startsWith(word, i)) throw JSONException("Expected $word at $i")
        i += word.length
        return value
    }

    private fun readObject(): JSONObject {
        val obj = JSONObject()
        expect('{')
        skipSpace()
        if (i < s.length && s[i] == '}') { i++; return obj }
        while (true) {
            skipSpace()
            val key = readString()
            expect(':')
            obj.set(key, readValue())
            skipSpace()
            if (i < s.length && s[i] == ',') { i++; continue }
            expect('}')
            return obj
        }
    }

    private fun readArray(): JSONArray {
        val arr = JSONArray()
        expect('[')
        skipSpace()
        if (i < s.length && s[i] == ']') { i++; return arr }
        while (true) {
            arr.add(readValue())
            skipSpace()
            if (i < s.length && s[i] == ',') { i++; continue }
            expect(']')
            return arr
        }
    }

    private fun readString(): String {
        if (i >= s.length || s[i] != '"') throw JSONException("Expected a string at $i")
        i++
        val out = StringBuilder()
        while (true) {
            if (i >= s.length) throw JSONException("Unterminated string")
            val c = s[i++]
            when (c) {
                '"' -> return out.toString()
                '\\' -> {
                    if (i >= s.length) throw JSONException("Unterminated escape")
                    when (val e = s[i++]) {
                        'b' -> out.append('\b')
                        't' -> out.append('\t')
                        'n' -> out.append('\n')
                        'f' -> out.append('\u000C')
                        'r' -> out.append('\r')
                        'u' -> {
                            if (i + 4 > s.length) throw JSONException("Bad unicode escape")
                            out.append(s.substring(i, i + 4).toInt(16).toChar())
                            i += 4
                        }
                        else -> out.append(e) // \" \\ \/
                    }
                }
                else -> out.append(c)
            }
        }
    }

    /** Whole numbers become Int or Long, others Double (as org.json reads them). */
    private fun readNumber(): Any {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
        val text = s.substring(start, i)
        val whole = text.none { it == '.' || it == 'e' || it == 'E' }
        if (whole) {
            text.toLongOrNull()?.let { return if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it }
        }
        return text.toDoubleOrNull() ?: throw JSONException("Bad number $text")
    }
}
