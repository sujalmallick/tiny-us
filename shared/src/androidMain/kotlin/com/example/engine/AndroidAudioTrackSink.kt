package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

/**
 * Android AudioTrack implementation of the PcmAudioSink contract.
 */
class AndroidAudioTrackSink : PcmAudioSink {
    private var track: AudioTrack? = null

    override fun playStaticBuffer(buffer: ShortArray, sampleRate: Int, volume: Float) {
        stop()
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(buffer.size * 2)

            val newTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                minBuf,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            newTrack.write(buffer, 0, buffer.size)
            newTrack.setVolume(volume.coerceIn(0f, 1f))
            newTrack.play()
            track = newTrack
        } catch (_: Exception) {}
    }

    override fun stop() {
        try {
            track?.stop()
            track?.release()
        } catch (_: Exception) {}
        track = null
    }

    override fun release() {
        stop()
    }
}
