package com.example.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.PolaroidMemory

/**
 * The couple's Polaroid photos as the shared screens use them. Each platform keeps the images
 * (files, the photo library) and draws the cards.
 */
interface PolaroidPhotos {
    fun getPolaroids(): List<PolaroidMemory>
    fun deletePolaroid(id: String)

    /** The finished card for [memory], or null if its image is gone. */
    fun loadCard(memory: PolaroidMemory): ImageBitmap?

    /** Saves [card] to the phone's photo library; true when it worked. */
    fun saveToGallery(card: ImageBitmap, title: String): Boolean
}
