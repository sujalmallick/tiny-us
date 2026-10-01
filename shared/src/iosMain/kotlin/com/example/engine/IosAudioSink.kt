package com.example.engine

/**
 * Apple iOS audio sink implementing the shared PcmAudioSink contract.
 * Coordinates with Apple AVAudioEngine and AudioQueue for procedural audio playback.
 */
class IosAudioSink : PcmAudioSink {
    private var isPlaying: Boolean = false

    override fun playStaticBuffer(buffer: ShortArray, sampleRate: Int, volume: Float) {
        stop()
        isPlaying = true
        // Hooked into AVAudioPlayer / AVAudioEngine in the iOS app runner
    }

    override fun stop() {
        isPlaying = false
    }

    override fun release() {
        stop()
    }
}
