@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, kotlin.uuid.ExperimentalUuidApi::class)

package com.example.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.example.data.KeyValueStorage
import com.example.data.PolaroidMemory
import com.example.data.PolaroidStore
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo
import platform.Foundation.* // dataWithContentsOfFile and friends are Objective-C category methods
import platform.UIKit.*
import platform.posix.memcpy

/**
 * Polaroids on iOS: PNG files in the app's Documents/polaroids, the list in [PolaroidStore], and
 * Save to Photos through UIKit. A new Polaroid is the screen as it was when the heart was tapped,
 * printed as a card ([IosPolaroidCard]) like Android's.
 */
class IosPolaroidPhotos(storage: KeyValueStorage) : PolaroidPhotos {
    private val store = PolaroidStore(storage) { path -> NSFileManager.defaultManager.removeItemAtPath(path, error = null) }

    private val folder: String by lazy {
        val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val dir = "$docs/polaroids"
        NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
        dir
    }

    override fun getPolaroids(): List<PolaroidMemory> = store.getPolaroids()

    override fun deletePolaroid(id: String) = store.deletePolaroid(id)

    /** The card for [memory]; a bare screenshot from an earlier version is printed as a card first. */
    override fun loadCard(memory: PolaroidMemory): ImageBitmap? {
        val image = decodeSkia(memory.imagePath) ?: return null
        if (image.height.toFloat() / image.width.toFloat() <= 1.7f) return image.toComposeImageBitmap()
        val card = IosPolaroidCard.render(image, memory.title, memory.date, memory.time, memory.sceneName)
        card.encodeToData(EncodedImageFormat.PNG)?.bytes?.toNSData()?.writeToFile(memory.imagePath, atomically = true)
        return card.toComposeImageBitmap()
    }

    override fun loadThumbnail(path: String, maxWidth: Int): ImageBitmap? = decode(path)

    override fun saveToGallery(card: ImageBitmap, title: String): Boolean {
        val png = encodePng(card) ?: return false
        val image = UIImage(data = png.toNSData())
        UIImageWriteToSavedPhotosAlbum(image, null, null, null)
        return true
    }

    /** Snapshots the app's window, saves it as a new Polaroid and returns it with its memory. */
    fun take(sceneName: String, sceneEnvKey: String, isNight: Boolean, isSunset: Boolean): Pair<ImageBitmap, PolaroidMemory>? {
        val window = UIApplication.sharedApplication.keyWindow ?: return null
        val renderer = UIGraphicsImageRenderer(bounds = window.bounds)
        val shot = renderer.imageWithActions { _ ->
            window.drawViewHierarchyInRect(window.bounds, afterScreenUpdates = true)
        }
        val shotPng = UIImagePNGRepresentation(shot) ?: return null
        val scene = runCatching { SkiaImage.makeFromEncoded(shotPng.toByteArray()) }.getOrNull() ?: return null

        val title = store.pickTitle(sceneEnvKey, isNight, isSunset)
        val date = store.formattedDate()
        val time = store.formattedTime()
        val card = IosPolaroidCard.render(scene, title, date, time, sceneName)
        val cardPng = card.encodeToData(EncodedImageFormat.PNG)?.bytes ?: return null

        val path = "$folder/pol_${Clock.System.now().toEpochMilliseconds()}_${Uuid.random().toString().take(6)}.png"
        if (!cardPng.toNSData().writeToFile(path, atomically = true)) return null
        val memory = PolaroidMemory(
            id = Uuid.random().toString(),
            title = title,
            date = date,
            time = time,
            sceneName = sceneName,
            sceneEnvKey = sceneEnvKey,
            imagePath = path
        )
        store.savePolaroid(memory)
        return card.toComposeImageBitmap() to memory
    }

    private fun decodeSkia(path: String): SkiaImage? {
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return runCatching { SkiaImage.makeFromEncoded(data.toByteArray()) }.getOrNull()
    }

    private fun decode(path: String): ImageBitmap? = decodeSkia(path)?.toComposeImageBitmap()

    /** The image's pixels as PNG bytes (read back and encoded by Skia, as the pixel buffer does). */
    private fun encodePng(image: ImageBitmap): ByteArray? = runCatching {
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        val bytes = ByteArray(pixels.size * 4)
        for (i in pixels.indices) {
            val c = pixels[i]
            val b = i * 4
            bytes[b] = c.toByte()
            bytes[b + 1] = (c shr 8).toByte()
            bytes[b + 2] = (c shr 16).toByte()
            bytes[b + 3] = (c ushr 24).toByte()
        }
        // Pixels are little-endian ARGB ints, which is BGRA byte order.
        val raster = SkiaImage.makeRaster(ImageInfo(image.width, image.height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL), bytes, image.width * 4)
        raster.encodeToData(EncodedImageFormat.PNG)?.bytes
    }.getOrNull()
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply { usePinned { memcpy(it.addressOf(0), bytes, length) } }
}

private fun ByteArray.toNSData(): NSData = usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
