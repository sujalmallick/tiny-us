package com.example.data.backup

import java.util.zip.Inflater

actual object RawInflate {
    actual fun inflate(data: ByteArray, size: Int): ByteArray? = runCatching {
        val inflater = Inflater(true)
        try {
            inflater.setInput(data)
            val out = ByteArray(size)
            var filled = 0
            while (filled < size && !inflater.finished()) {
                val n = inflater.inflate(out, filled, size - filled)
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                filled += n
            }
            if (filled == size) out else null
        } finally {
            inflater.end()
        }
    }.getOrNull()
}
