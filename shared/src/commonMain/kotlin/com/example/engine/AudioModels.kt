package com.example.engine

enum class MusicBoxState {
    STOPPED,
    PLAYING,
    PAUSED
}

data class MusicalNote(
    val freq: Double,
    val durationMs: Int,
    val delayAfterMs: Long = durationMs.toLong() + 35L
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val vibe: String,
    val rawResId: Int? = null,
    val notes: List<MusicalNote> = emptyList()
)

/**
 * Platform-agnostic interface for streaming or playing generated PCM audio buffers.
 */
interface PcmAudioSink {
    fun playStaticBuffer(buffer: ShortArray, sampleRate: Int = 22050, volume: Float = 1.0f)
    fun stop()
    fun release()
}
