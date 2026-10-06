@file:OptIn(ExperimentalForeignApi::class)

package com.example.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.* // dataWithContentsOfFile is an Objective-C category method
import platform.posix.memcpy

/**
 * The same tiny_pixel.ttf as Android (shared/src/androidMain/res/font), bundled into the iOS app by
 * the Xcode project. Without it (e.g. in tests) text falls back to the default font.
 */
actual val PixelFamily: FontFamily by lazy {
    val bytes = bundledFont("tiny_pixel")
    if (bytes == null) FontFamily.Default else FontFamily(Font(identity = "tiny_pixel", data = bytes))
}

private fun bundledFont(name: String): ByteArray? {
    val path = NSBundle.mainBundle.pathForResource(name, ofType = "ttf") ?: return null
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val size = data.length.toInt()
    if (size == 0) return null
    return ByteArray(size).apply { usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
}
