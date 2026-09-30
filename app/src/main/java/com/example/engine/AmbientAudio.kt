package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import com.example.scene.WeatherType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections
import kotlin.coroutines.coroutineContext
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.tanh

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

class AmbientAudio(var context: Context? = null) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val playbackMutex = Mutex()
    private var musicJob: Job? = null
    private val musicLock = Any()
    private val sfxLock = Any()

    // Weather BGM Layer
    private val weatherLock = Any()
    @Volatile
    var weatherPlayer: MediaPlayer? = null
        private set
    @Volatile
    var weatherFadingOutPlayer: MediaPlayer? = null
        private set
    @Volatile
    private var weatherEqualizer: Equalizer? = null

    private var weatherCrossfadeJob: Job? = null
    private var indoorTransitionJob: Job? = null
    private var duckingTransitionJob: Job? = null

    var currentWeather: WeatherType? by mutableStateOf(null)
        private set

    val isCrossfading: Boolean
        get() = weatherCrossfadeJob?.isActive == true

    var baseWeatherVolume: Float = 1.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateWeatherVolume()
        }

    var indoorVolumeMultiplier: Float = 1.0f
        private set

    var musicBoxDuckingMultiplier: Float = 1.0f
        private set

    var isIndoor: Boolean = false
        private set

    val isMusicBoxDucked: Boolean
        get() = musicBoxDuckingMultiplier < 0.99f

    val effectiveWeatherVolume: Float
        get() = computeEffectiveWeatherVolume()

    val weatherTrackStartOffsets = mutableMapOf<WeatherType, Int>(
        WeatherType.SUNNY to 0,
        WeatherType.RAIN to 0,
        WeatherType.SAKURA to 0,
        WeatherType.AUTUMN to 0,
        WeatherType.SNOW to 0
    )

    fun setTrackStartOffset(weather: WeatherType, offsetMs: Int) {
        weatherTrackStartOffsets[weather] = offsetMs.coerceAtLeast(0)
    }

    fun getTrackStartOffset(weather: WeatherType): Int {
        return weatherTrackStartOffsets[weather] ?: 0
    }

    fun getWeatherRawResId(weather: WeatherType): Int = when (weather) {
        WeatherType.SUNNY -> R.raw.bgm_sunny
        WeatherType.RAIN -> R.raw.bgm_rain
        WeatherType.SAKURA -> R.raw.bgm_sakura
        WeatherType.AUTUMN -> R.raw.bgm_autumn
        WeatherType.SNOW -> R.raw.bgm_snow
    }

    fun computeEffectiveWeatherVolume(): Float {
        if (!isEnabled) return 0f
        return (baseWeatherVolume * indoorVolumeMultiplier * musicBoxDuckingMultiplier).coerceIn(0f, 1f)
    }

    fun updateWeatherVolume() {
        val effectiveVol = computeEffectiveWeatherVolume()
        synchronized(weatherLock) {
            if (weatherCrossfadeJob?.isActive != true) {
                weatherPlayer?.let { player ->
                    try {
                        player.setVolume(effectiveVol, effectiveVol)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    @Volatile
    private var currentMusicTrack: AudioTrack? = null
    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    private var savedMpSeekPosition: Int = 0

    @Volatile
    private var rainTrack: AudioTrack? = null
    @Volatile
    private var footstepTrack: AudioTrack? = null
    private val activeSfxTracks = Collections.synchronizedSet(mutableSetOf<AudioTrack>())

    var musicBoxState: MusicBoxState by mutableStateOf(MusicBoxState.STOPPED)
        private set

    val isPlayingMusic: Boolean
        get() = musicBoxState == MusicBoxState.PLAYING

    private var currentNoteIndex: Int = 0

    private fun initFootstepTrack() {
        if (footstepTrack != null) return
        try {
            val numSamples = (sampleRate * 35 / 1000).coerceAtLeast(50)
            val buffer = ShortArray(numSamples)
            var last = 0f
            for (i in 0 until numSamples) {
                val white = (Math.random() * 2.0 - 1.0).toFloat()
                last = (last + (0.05f * white)) / 1.05f
                val env = 1.0 - (i.toDouble() / numSamples)
                buffer[i] = (last * env * 0.16f * Short.MAX_VALUE).toInt().toShort()
            }
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(buffer.size * 2)

            val track = AudioTrack(
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
            track.write(buffer, 0, buffer.size)
            footstepTrack = track
        } catch (_: Exception) {}
    }

    fun updateContext(newContext: Context) {
        context = newContext.applicationContext
        if (isEnabled && weatherPlayer == null && currentWeather != null) {
            playWeatherBgm(currentWeather!!, isAutomaticDrift = false)
        }
    }

    @Volatile
    var isEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                stopAllSfx()
                stopRainAmbient()
                pauseWeatherBgm()
            } else {
                resumeWeatherBgm()
            }
            // Master mute: adjust active track and media player volume directly without touching musicBoxState
            synchronized(musicLock) {
                val v = if (value) 1.0f else 0.0f
                mediaPlayer?.let { mp ->
                    try {
                        mp.setVolume(v, v)
                    } catch (_: Exception) {}
                }
                currentMusicTrack?.let { track ->
                    try {
                        track.setVolume(v)
                    } catch (_: Exception) {}
                }
            }
            updateWeatherVolume()
        }

    private val sampleRate = 22050

    // Complete rich offline retro music box & chiptune playlist
    val playlist: List<Song> = listOf(
        Song(
            id = "died_in_arms",
            title = "(I Just) Died In Your Arms",
            artist = "Cutting Crew",
            vibe = "Iconic 80s Chorus • Our Song",
            rawResId = R.raw.died_in_your_arms,
            notes = listOf(
                MusicalNote(369.99, 320, 380), // F#4 ("Oh...")
                MusicalNote(493.88, 520, 600), // B4 ("...I...")
                MusicalNote(493.88, 220, 260), // B4 ("I")
                MusicalNote(554.37, 220, 260), // C#5 ("just")
                MusicalNote(587.33, 280, 320), // D5 ("died")
                MusicalNote(554.37, 240, 280), // C#5 ("in")
                MusicalNote(493.88, 240, 280), // B4 ("your")
                MusicalNote(440.00, 240, 280), // A4 ("arms")
                MusicalNote(493.88, 480, 600), // B4 ("tonight")
                MusicalNote(493.88, 220, 260), // B4 ("It")
                MusicalNote(554.37, 220, 260), // C#5 ("must've")
                MusicalNote(587.33, 280, 320), // D5 ("been")
                MusicalNote(554.37, 240, 280), // C#5 ("something")
                MusicalNote(493.88, 240, 280), // B4 ("you")
                MusicalNote(440.00, 240, 280), // A4 ("said")
                MusicalNote(369.99, 520, 650), // F#4
                MusicalNote(493.88, 220, 260), // B4 ("I")
                MusicalNote(554.37, 220, 260), // C#5 ("just")
                MusicalNote(587.33, 280, 320), // D5 ("died")
                MusicalNote(554.37, 240, 280), // C#5 ("in")
                MusicalNote(493.88, 240, 280), // B4 ("your")
                MusicalNote(739.99, 450, 520), // F#5 ("arms")
                MusicalNote(659.25, 320, 360), // E5 ("to-")
                MusicalNote(587.33, 320, 380), // D5 ("-night")
                MusicalNote(493.88, 700, 950)  // B4
            )
        ),
        Song(
            id = "eyes_off_you",
            title = "Can't Take My Eyes Off You",
            artist = "Frankie Valli",
            vibe = "I Love You Baby • Horn Fanfare",
            rawResId = R.raw.cant_take_my_eyes_off_you,
            notes = listOf(
                MusicalNote(523.25, 260, 300), // C5 ("I")
                MusicalNote(659.25, 260, 300), // E5 ("love")
                MusicalNote(783.99, 320, 360), // G5 ("you")
                MusicalNote(880.00, 480, 580), // A5 ("ba-by")
                MusicalNote(783.99, 220, 260), // G5 ("and")
                MusicalNote(698.46, 220, 260), // F5 ("if")
                MusicalNote(659.25, 240, 280), // E5 ("it's")
                MusicalNote(587.33, 240, 280), // D5 ("quite")
                MusicalNote(523.25, 450, 580), // C5 ("all right")
                MusicalNote(523.25, 260, 300), // C5 ("I")
                MusicalNote(659.25, 260, 300), // E5 ("need")
                MusicalNote(783.99, 320, 360), // G5 ("you")
                MusicalNote(880.00, 480, 580), // A5 ("ba-by")
                MusicalNote(783.99, 220, 260), // G5 ("to")
                MusicalNote(698.46, 220, 260), // F5 ("warm")
                MusicalNote(659.25, 240, 280), // E5 ("the")
                MusicalNote(587.33, 240, 280), // D5 ("lone-")
                MusicalNote(659.25, 520, 650), // E5 ("-ly night")
                MusicalNote(523.25, 240, 280), // C5 ("I")
                MusicalNote(659.25, 240, 280), // E5 ("love")
                MusicalNote(783.99, 300, 340), // G5 ("you")
                MusicalNote(880.00, 450, 520), // A5 ("ba-by")
                MusicalNote(783.99, 220, 260), // G5 ("trust")
                MusicalNote(698.46, 220, 260), // F5 ("in")
                MusicalNote(659.25, 220, 260), // E5 ("me")
                MusicalNote(587.33, 300, 360), // D5 ("when")
                MusicalNote(523.25, 750, 950)  // C5 ("I say")
            )
        ),
        Song(
            id = "wicked_game",
            title = "Wicked Game",
            artist = "Chris Isaak",
            vibe = "Dreamy Midnight Romance",
            rawResId = R.raw.wicked_game,
            notes = listOf(
                MusicalNote(493.88, 380, 420), // B4
                MusicalNote(587.33, 380, 420), // D5
                MusicalNote(739.99, 450, 500), // F#5
                MusicalNote(659.25, 450, 520), // E5
                MusicalNote(587.33, 320, 360), // D5
                MusicalNote(554.37, 320, 360), // C#5
                MusicalNote(493.88, 480, 560), // B4
                MusicalNote(440.00, 420, 480), // A4
                MusicalNote(493.88, 550, 680), // B4
                MusicalNote(493.88, 300, 340), // B4 ("No")
                MusicalNote(587.33, 300, 340), // D5 ("I")
                MusicalNote(554.37, 300, 340), // C#5 ("don't")
                MusicalNote(493.88, 300, 340), // B4 ("wan-")
                MusicalNote(440.00, 300, 340), // A4 ("-na")
                MusicalNote(493.88, 480, 560), // B4 ("fall in love")
                MusicalNote(415.30, 420, 480), // G#4 ("with")
                MusicalNote(369.99, 380, 440), // F#4 ("you")
                MusicalNote(329.63, 850, 1100) // E4
            )
        ),
        Song(
            id = "golden_brown",
            title = "Golden Brown",
            artist = "The Stranglers",
            vibe = "Vintage Harpsichord Waltz",
            rawResId = R.raw.golden_brown,
            notes = listOf(
                MusicalNote(466.16, 180, 210), // Bb4
                MusicalNote(587.33, 180, 210), // D5
                MusicalNote(698.46, 180, 210), // F5
                MusicalNote(783.99, 260, 300), // G5
                MusicalNote(698.46, 180, 210), // F5
                MusicalNote(587.33, 180, 210), // D5
                MusicalNote(466.16, 180, 210), // Bb4
                MusicalNote(587.33, 180, 210), // D5
                MusicalNote(698.46, 180, 210), // F5
                MusicalNote(783.99, 260, 300), // G5
                MusicalNote(698.46, 180, 210), // F5
                MusicalNote(587.33, 180, 210), // D5
                MusicalNote(523.25, 180, 210), // C5
                MusicalNote(622.25, 180, 210), // Eb5
                MusicalNote(783.99, 260, 300), // G5
                MusicalNote(698.46, 180, 210), // F5
                MusicalNote(622.25, 180, 210), // Eb5
                MusicalNote(587.33, 480, 650)  // D5
            )
        ),
        Song(
            id = "heartbeat",
            title = "Heartbeat",
            artist = "Cozy Love Pulse",
            vibe = "Acoustic Warmth • Gentle",
            notes = listOf(
                MusicalNote(146.83, 130, 180), // bass thump
                MusicalNote(146.83, 130, 260), // double thump
                MusicalNote(587.33, 250, 290), // D5
                MusicalNote(739.99, 250, 290), // F#5
                MusicalNote(880.00, 360, 420), // A5
                MusicalNote(987.77, 330, 370), // B5
                MusicalNote(880.00, 310, 350), // A5
                MusicalNote(739.99, 270, 310), // F#5
                MusicalNote(659.25, 270, 310), // E5
                MusicalNote(587.33, 500, 750)  // D5
            )
        ),
        Song(
            id = "until_i_found_you",
            title = "Until I Found You",
            artist = "Stephen Sanchez",
            vibe = "50s Ballad • Pure Affection",
            notes = listOf(
                MusicalNote(392.00, 260, 300), // G4 ("I")
                MusicalNote(493.88, 260, 300), // B4 ("would")
                MusicalNote(587.33, 320, 360), // D5 ("ne-")
                MusicalNote(659.25, 380, 440), // E5 ("-ver fall in love")
                MusicalNote(587.33, 300, 340), // D5
                MusicalNote(493.88, 280, 320), // B4 ("until")
                MusicalNote(440.00, 280, 320), // A4 ("I found")
                MusicalNote(392.00, 420, 500), // G4 ("her")
                MusicalNote(440.00, 260, 300), // A4 ("I said")
                MusicalNote(493.88, 420, 480), // B4 ("ooh")
                MusicalNote(587.33, 280, 320), // D5 ("I'm")
                MusicalNote(659.25, 380, 440), // E5 ("fal-")
                MusicalNote(587.33, 300, 340), // D5 ("-ling")
                MusicalNote(493.88, 280, 320), // B4 ("for")
                MusicalNote(440.00, 300, 340), // A4 ("ya")
                MusicalNote(392.00, 750, 950)  // G4
            )
        ),
        Song(
            id = "lullaby_theme",
            title = "Tiny Us Lullaby",
            artist = "Our Theme Song",
            vibe = "Cozy Music Box • Original",
            notes = listOf(
                MusicalNote(523.25, 380, 500), // C5
                MusicalNote(659.25, 380, 500), // E5
                MusicalNote(783.99, 380, 500), // G5
                MusicalNote(880.00, 380, 500), // A5
                MusicalNote(1046.50, 420, 550),// C6
                MusicalNote(880.00, 380, 500), // A5
                MusicalNote(783.99, 380, 500), // G5
                MusicalNote(659.25, 380, 500), // E5
                MusicalNote(587.33, 380, 500), // D5
                MusicalNote(659.25, 380, 500), // E5
                MusicalNote(783.99, 380, 500), // G5
                MusicalNote(523.25, 550, 800)  // C5
            )
        ),
        Song(
            id = "midnight_slumber",
            title = "Midnight Slumber",
            artist = "Cozy Lullaby",
            vibe = "Dreamy Midnight • Soothing",
            notes = listOf(
                MusicalNote(440.00, 480, 620), // A4
                MusicalNote(523.25, 480, 620), // C5
                MusicalNote(659.25, 650, 850), // E5
                MusicalNote(587.33, 480, 620), // D5
                MusicalNote(523.25, 480, 620), // C5
                MusicalNote(493.88, 550, 750), // B4
                MusicalNote(440.00, 700, 950), // A4
                MusicalNote(392.00, 480, 620), // G4
                MusicalNote(440.00, 480, 620), // A4
                MusicalNote(523.25, 650, 850), // C5
                MusicalNote(659.25, 500, 650), // E5
                MusicalNote(783.99, 750, 1000) // G5
            )
        )
    )

    var currentSong: Song by mutableStateOf(playlist[0])
        private set

    fun playSong(song: Song) {
        val sameSong = currentSong.id == song.id
        currentSong = song
        if (!sameSong) {
            currentNoteIndex = 0
            savedMpSeekPosition = 0
            synchronized(musicLock) {
                mediaPlayer?.let { mp ->
                    try {
                        mp.stop()
                        mp.release()
                    } catch (_: Exception) {}
                }
                mediaPlayer = null
            }
        }
        musicBoxState = MusicBoxState.PLAYING
        setMusicBoxDucking(duck = true, smooth = true)
        synchronized(musicLock) {
            currentMusicTrack?.let { track ->
                try {
                    track.pause()
                    track.flush()
                } catch (_: Exception) {}
            }
        }
        scope.launch {
            startPlaybackJob()
        }
    }

    fun togglePlayPause() {
        when (musicBoxState) {
            MusicBoxState.PLAYING -> pauseMusic()
            MusicBoxState.PAUSED -> resumeMusic()
            MusicBoxState.STOPPED -> playSong(currentSong)
        }
    }

    fun pauseMusic() {
        if (musicBoxState != MusicBoxState.PLAYING) return
        musicBoxState = MusicBoxState.PAUSED
        setMusicBoxDucking(duck = false, smooth = true)

        // Synchronous, immediate hardware stop/pause for guaranteed instant silence
        synchronized(musicLock) {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        mp.pause()
                        savedMpSeekPosition = mp.currentPosition
                    }
                } catch (_: Exception) {}
            }
            currentMusicTrack?.let { track ->
                try {
                    track.pause()
                    track.flush()
                } catch (_: Exception) {}
            }
        }

        // Cancel and join the coroutine so it tears down completely before any next start
        scope.launch {
            playbackMutex.withLock {
                musicJob?.cancelAndJoin()
                musicJob = null
            }
        }
    }

    fun resumeMusic() {
        if (musicBoxState == MusicBoxState.PLAYING) return
        musicBoxState = MusicBoxState.PLAYING
        setMusicBoxDucking(duck = true, smooth = true)
        scope.launch {
            startPlaybackJob()
        }
    }

    fun nextSong() {
        val nextIdx = (playlist.indexOf(currentSong) + 1) % playlist.size
        playSong(playlist[nextIdx])
    }

    fun previousSong() {
        val currentIdx = playlist.indexOf(currentSong)
        val prevIdx = if (currentIdx <= 0) playlist.size - 1 else currentIdx - 1
        playSong(playlist[prevIdx])
    }

    /**
     * Plays a single SFX note using MODE_STATIC (reliable, instantly mutable).
     */
    private fun playSfxNoteStatic(freq: Double, durationMs: Int, volume: Float, isMusicBox: Boolean) {
        if (!isEnabled) return

        val numSamples = (sampleRate * durationMs / 1000).coerceAtLeast(64)
        val buffer = generateToneSamples(freq, numSamples, volume, isMusicBox)
        val bufferBytes = buffer.size * 2
        val minBufBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val finalBufferBytes = bufferBytes.coerceAtLeast(minBufBytes)
        val finalBufferShorts = finalBufferBytes / 2
        val writeBuffer = if (buffer.size < finalBufferShorts) {
            val padded = ShortArray(finalBufferShorts)
            System.arraycopy(buffer, 0, padded, 0, buffer.size)
            padded
        } else {
            buffer
        }

        var track: AudioTrack? = null
        try {
            track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                finalBufferBytes,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            val written = track.write(writeBuffer, 0, writeBuffer.size)
            if (written < 0 || track.state != AudioTrack.STATE_INITIALIZED) {
                try { track.release() } catch (_: Exception) {}
                return
            }

            if (!isEnabled) {
                try { track.release() } catch (_: Exception) {}
                return
            }

            activeSfxTracks.add(track)
            track.play()

            val playDurationMs = (numSamples * 1000L) / sampleRate
            val deadline = System.currentTimeMillis() + playDurationMs
            while (System.currentTimeMillis() < deadline) {
                if (!isEnabled) break
                Thread.sleep(20L)
            }
        } catch (_: Exception) {
        } finally {
            if (track != null) {
                activeSfxTracks.remove(track)
                try {
                    track.pause()
                    track.stop()
                    track.flush()
                    track.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun startMusic() {
        resumeMusic()
    }

    fun stopMusic() {
        if (musicBoxState == MusicBoxState.STOPPED) return
        musicBoxState = MusicBoxState.STOPPED
        setMusicBoxDucking(duck = false, smooth = true)
        currentNoteIndex = 0
        savedMpSeekPosition = 0

        // Synchronous, immediate AudioTrack & MediaPlayer stop, flush, and release
        synchronized(musicLock) {
            mediaPlayer?.let { mp ->
                try {
                    mp.stop()
                    mp.release()
                } catch (_: Exception) {}
            }
            mediaPlayer = null

            currentMusicTrack?.let { track ->
                try {
                    track.pause()
                    track.flush()
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
            currentMusicTrack = null
        }

        scope.launch {
            playbackMutex.withLock {
                musicJob?.cancelAndJoin()
                musicJob = null
            }
        }
    }

    private suspend fun startPlaybackJob() {
        playbackMutex.withLock {
            android.util.Log.d("AmbientAudio", "startPlaybackJob entered, musicBoxState=$musicBoxState, currentSong=${currentSong.title}")
            // Cancel and wait for any previous playback coroutine to completely tear down
            musicJob?.cancelAndJoin()
            musicJob = null

            if (musicBoxState != MusicBoxState.PLAYING) {
                android.util.Log.d("AmbientAudio", "startPlaybackJob cancelled because state is not PLAYING ($musicBoxState)")
                return@withLock
            }

            // Case A: Play real MP3 song if rawResId is available
            if (currentSong.rawResId != null) {
                // Ensure procedural track is paused and flushed
                synchronized(musicLock) {
                    currentMusicTrack?.let { track ->
                        try {
                            track.pause()
                            track.flush()
                        } catch (_: Exception) {}
                    }
                }

                val ctx = context
                if (ctx != null) {
                    synchronized(musicLock) {
                        try {
                            var mp = mediaPlayer
                            if (mp == null) {
                                mp = MediaPlayer.create(ctx, currentSong.rawResId!!)
                                mp?.isLooping = true
                                val vol = if (isEnabled) 1.0f else 0.0f
                                mp?.setVolume(vol, vol)
                                if (savedMpSeekPosition > 0) {
                                    mp?.seekTo(savedMpSeekPosition)
                                }
                                mediaPlayer = mp
                            }
                            val vol = if (isEnabled) 1.0f else 0.0f
                            mp?.setVolume(vol, vol)
                            if (mp?.isPlaying == false) {
                                mp?.start()
                            }
                            android.util.Log.d("AmbientAudio", "MediaPlayer started for ${currentSong.title}, isPlaying=${mp?.isPlaying}")
                        } catch (e: Exception) {
                            android.util.Log.e("AmbientAudio", "Error starting MediaPlayer for ${currentSong.title}", e)
                        }
                    }

                    musicJob = scope.launch(Dispatchers.IO) {
                        try {
                            while (coroutineContext.isActive && musicBoxState == MusicBoxState.PLAYING) {
                                delay(300)
                            }
                        } catch (_: CancellationException) {
                        }
                    }
                } else {
                    android.util.Log.e("AmbientAudio", "Context is null, cannot start MediaPlayer for ${currentSong.title}")
                }
                return@withLock
            }

            // Case B: Procedural Chiptune audio for custom theme songs without rawResId
            synchronized(musicLock) {
                // Tear down any existing MediaPlayer before starting procedural track
                mediaPlayer?.let { mp ->
                    try {
                        mp.stop()
                        mp.release()
                    } catch (_: Exception) {}
                }
                mediaPlayer = null
                savedMpSeekPosition = 0

                var track = currentMusicTrack
                if (track == null || track!!.state != AudioTrack.STATE_INITIALIZED) {
                    try { track?.release() } catch (_: Exception) {}
                    val minBuf = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    track = AudioTrack(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                        minBuf * 4,
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    )
                    currentMusicTrack = track
                }
                val effectiveVol = if (isEnabled) 1.0f else 0.0f
                track!!.setVolume(effectiveVol)
                if (track!!.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track!!.play()
                }
            }

            val finalTrack = currentMusicTrack ?: return@withLock
            musicJob = scope.launch(Dispatchers.IO) {
                runProceduralPlayback(finalTrack)
            }
        }
    }

    private suspend fun runProceduralPlayback(track: AudioTrack) {
        android.util.Log.d("AmbientAudio", "runProceduralPlayback launched on thread ${Thread.currentThread().name}")
        val silenceChunk = ShortArray(512)
        try {
            while (coroutineContext.isActive && musicBoxState == MusicBoxState.PLAYING) {
                val song = currentSong
                val notes = song.notes
                if (notes.isEmpty()) {
                    android.util.Log.w("AmbientAudio", "Song ${song.title} has no notes!")
                    break
                }

                val startIdx = currentNoteIndex.coerceIn(0, notes.size - 1)
                android.util.Log.d("AmbientAudio", "Playing ${song.title} from note index $startIdx / ${notes.size}")
                for (i in startIdx until notes.size) {
                    if (!coroutineContext.isActive || musicBoxState != MusicBoxState.PLAYING) break
                    currentNoteIndex = i
                    val note = notes[i]
                    if (note.freq > 0.0) {
                        val numSamples = (sampleRate * note.durationMs / 1000).coerceAtLeast(64)
                        val tone = generateToneSamples(note.freq, numSamples, volume = 0.65f, isMusicBox = true)
                        var written = 0
                        while (written < tone.size && coroutineContext.isActive && musicBoxState == MusicBoxState.PLAYING) {
                            val chunk = (tone.size - written).coerceAtMost(512)
                            val res = track.write(tone, written, chunk, AudioTrack.WRITE_BLOCKING)
                            if (res <= 0) break
                            written += res
                        }
                    }
                    if (!coroutineContext.isActive || musicBoxState != MusicBoxState.PLAYING) break

                    // Responsive silence gap between notes
                    val gapMs = (note.delayAfterMs - note.durationMs).coerceAtLeast(20L)
                    val gapSamples = (sampleRate * gapMs / 1000).toInt()
                    var gapWritten = 0
                    while (gapWritten < gapSamples && coroutineContext.isActive && musicBoxState == MusicBoxState.PLAYING) {
                        val chunk = (gapSamples - gapWritten).coerceAtMost(512)
                        val res = track.write(silenceChunk, 0, chunk, AudioTrack.WRITE_BLOCKING)
                        if (res <= 0) break
                        gapWritten += res
                    }

                    if (coroutineContext.isActive && musicBoxState == MusicBoxState.PLAYING) {
                        currentNoteIndex = (i + 1) % notes.size
                    }
                }

                // Peaceful silence between full loops
                if (coroutineContext.isActive && musicBoxState == MusicBoxState.PLAYING) {
                    currentNoteIndex = 0
                    val loopGapSamples = (sampleRate * 1200L / 1000).toInt()
                    var loopWritten = 0
                    while (loopWritten < loopGapSamples && coroutineContext.isActive && musicBoxState == MusicBoxState.PLAYING) {
                        val chunk = (loopGapSamples - loopWritten).coerceAtMost(512)
                        val res = track.write(silenceChunk, 0, chunk, AudioTrack.WRITE_BLOCKING)
                        if (res <= 0) break
                        loopWritten += res
                    }
                }
            }
        } catch (_: CancellationException) {
            android.util.Log.d("AmbientAudio", "runProceduralPlayback cancelled normally")
        } catch (e: Exception) {
            android.util.Log.e("AmbientAudio", "runProceduralPlayback error", e)
        } finally {
            android.util.Log.d("AmbientAudio", "runProceduralPlayback exited")
        }
    }

    fun release() {
        stopMusic()
        stopWeatherBgm()
        stopAllSfx()
        stopRainAmbient()
        try {
            footstepTrack?.release()
            footstepTrack = null
        } catch (_: Exception) {}
    }

    fun setIndoor(indoor: Boolean, smooth: Boolean = true) {
        isIndoor = indoor
        val targetMultiplier = if (indoor) 0.40f else 1.0f
        applyIndoorEffect(indoor)

        if (!smooth) {
            indoorTransitionJob?.cancel()
            indoorTransitionJob = null
            indoorVolumeMultiplier = targetMultiplier
            updateWeatherVolume()
            return
        }

        if (indoorVolumeMultiplier == targetMultiplier) return

        indoorTransitionJob?.cancel()
        indoorTransitionJob = scope.launch(Dispatchers.Default) {
            val start = indoorVolumeMultiplier
            val durationMs = 800L
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                indoorVolumeMultiplier = start + (targetMultiplier - start) * progress
                updateWeatherVolume()
                if (progress >= 1f) break
                delay(30)
            }
        }
    }

    fun setMusicBoxDucking(duck: Boolean, smooth: Boolean = true) {
        val targetMultiplier = if (duck) 0.25f else 1.0f

        if (!smooth) {
            duckingTransitionJob?.cancel()
            duckingTransitionJob = null
            musicBoxDuckingMultiplier = targetMultiplier
            updateWeatherVolume()
            return
        }

        if (musicBoxDuckingMultiplier == targetMultiplier) return

        duckingTransitionJob?.cancel()
        duckingTransitionJob = scope.launch(Dispatchers.Default) {
            val start = musicBoxDuckingMultiplier
            val durationMs = 800L
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                musicBoxDuckingMultiplier = start + (targetMultiplier - start) * progress
                updateWeatherVolume()
                if (progress >= 1f) break
                delay(30)
            }
        }
    }

    fun playWeatherBgm(weather: WeatherType, isAutomaticDrift: Boolean = false) {
        val sameWeather = currentWeather == weather
        currentWeather = weather

        val ctx = context
        if (ctx == null) {
            return
        }

        if (sameWeather && weatherPlayer?.isPlaying == true) {
            return
        }

        val rawResId = getWeatherRawResId(weather)

        if (!isAutomaticDrift) {
            // Manual weather change: switch immediately with NO crossfade
            weatherCrossfadeJob?.cancel()
            weatherCrossfadeJob = null

            synchronized(weatherLock) {
                weatherFadingOutPlayer?.let { p ->
                    try {
                        p.stop()
                        p.release()
                    } catch (_: Exception) {}
                }
                weatherFadingOutPlayer = null

                weatherPlayer?.let { p ->
                    try {
                        p.stop()
                        p.release()
                    } catch (_: Exception) {}
                }
                weatherPlayer = null

                try {
                    weatherEqualizer?.release()
                } catch (_: Throwable) {}
                weatherEqualizer = null

                if (!isEnabled) return

                try {
                    val p = MediaPlayer.create(ctx, rawResId)
                    if (p != null) {
                        p.isLooping = true
                        val offsetMs = getTrackStartOffset(weather)
                        if (offsetMs > 0) {
                            p.seekTo(offsetMs)
                        }
                        val vol = computeEffectiveWeatherVolume()
                        p.setVolume(vol, vol)
                        p.start()
                        weatherPlayer = p
                        applyIndoorEffect(isIndoor)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AmbientAudio", "Error starting weather BGM for $weather", e)
                }
            }
        } else {
            // Automatic weather drift: smoothly crossfade from current track to new track
            weatherCrossfadeJob?.cancel()

            val oldPlayer = synchronized(weatherLock) {
                weatherFadingOutPlayer?.let { p ->
                    try {
                        p.stop()
                        p.release()
                    } catch (_: Exception) {}
                }
                weatherFadingOutPlayer = weatherPlayer
                weatherPlayer = null
                weatherFadingOutPlayer
            }

            if (!isEnabled) {
                synchronized(weatherLock) {
                    oldPlayer?.let {
                        try { it.stop(); it.release() } catch (_: Exception) {}
                    }
                    weatherFadingOutPlayer = null
                }
                return
            }

            var newPlayer: MediaPlayer? = null
            try {
                newPlayer = MediaPlayer.create(ctx, rawResId)
                newPlayer?.isLooping = true
                val offsetMs = getTrackStartOffset(weather)
                if (offsetMs > 0) {
                    newPlayer?.seekTo(offsetMs)
                }
                newPlayer?.setVolume(0f, 0f)
                newPlayer?.start()
            } catch (e: Exception) {
                android.util.Log.e("AmbientAudio", "Error creating crossfade new player for $weather", e)
            }

            if (newPlayer == null) {
                synchronized(weatherLock) {
                    weatherPlayer = oldPlayer
                    weatherFadingOutPlayer = null
                }
                return
            }

            synchronized(weatherLock) {
                weatherPlayer = newPlayer
                applyIndoorEffect(isIndoor)
            }

            if (oldPlayer == null) {
                weatherCrossfadeJob = scope.launch(Dispatchers.Default) {
                    val durationMs = 2500L
                    val startTime = System.currentTimeMillis()
                    while (isActive) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                        val effectiveVol = computeEffectiveWeatherVolume()
                        val newVol = progress * effectiveVol
                        synchronized(weatherLock) {
                            try { newPlayer.setVolume(newVol, newVol) } catch (_: Exception) {}
                        }
                        if (progress >= 1f) break
                        delay(30)
                    }
                }
                return
            }

            weatherCrossfadeJob = scope.launch(Dispatchers.Default) {
                val durationMs = 2500L
                val startTime = System.currentTimeMillis()
                try {
                    while (isActive) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                        val effectiveVol = computeEffectiveWeatherVolume()
                        val oldVol = (1f - progress) * effectiveVol
                        val newVol = progress * effectiveVol
                        synchronized(weatherLock) {
                            try { oldPlayer.setVolume(oldVol, oldVol) } catch (_: Exception) {}
                            try { newPlayer.setVolume(newVol, newVol) } catch (_: Exception) {}
                        }
                        if (progress >= 1f) break
                        delay(30)
                    }
                } finally {
                    synchronized(weatherLock) {
                        try {
                            oldPlayer.stop()
                            oldPlayer.release()
                        } catch (_: Exception) {}
                        if (weatherFadingOutPlayer == oldPlayer) {
                            weatherFadingOutPlayer = null
                        }
                        val finalVol = computeEffectiveWeatherVolume()
                        try {
                            newPlayer.setVolume(finalVol, finalVol)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private fun applyIndoorEffect(indoor: Boolean) {
        try {
            val player = weatherPlayer ?: return
            if (weatherEqualizer == null && indoor) {
                try {
                    weatherEqualizer = Equalizer(0, player.audioSessionId)
                } catch (_: Throwable) {
                    weatherEqualizer = null
                }
            }
            val eq = weatherEqualizer ?: return
            if (!eq.hasControl()) return
            if (indoor) {
                eq.enabled = true
                val numBands = eq.numberOfBands
                if (numBands > 0) {
                    val topBand = (numBands - 1).toShort()
                    val range = eq.bandLevelRange
                    val cut = (-400).toShort().coerceIn(range[0], range[1])
                    eq.setBandLevel(topBand, cut)
                    if (numBands > 2) {
                        val secondTop = (numBands - 2).toShort()
                        val midCut = (-200).toShort().coerceIn(range[0], range[1])
                        eq.setBandLevel(secondTop, midCut)
                    }
                }
            } else {
                val numBands = eq.numberOfBands
                for (b in 0 until numBands) {
                    eq.setBandLevel(b.toShort(), 0)
                }
                eq.enabled = false
            }
        } catch (_: Throwable) {}
    }

    fun pauseWeatherBgm() {
        synchronized(weatherLock) {
            weatherCrossfadeJob?.cancel()
            weatherPlayer?.let { p ->
                try {
                    if (p.isPlaying) p.pause()
                } catch (_: Exception) {}
            }
            weatherFadingOutPlayer?.let { p ->
                try {
                    p.stop()
                    p.release()
                } catch (_: Exception) {}
            }
            weatherFadingOutPlayer = null
        }
    }

    fun resumeWeatherBgm() {
        if (!isEnabled) return
        synchronized(weatherLock) {
            weatherPlayer?.let { p ->
                try {
                    val vol = computeEffectiveWeatherVolume()
                    p.setVolume(vol, vol)
                    if (!p.isPlaying) p.start()
                    return
                } catch (_: Exception) {}
            }
            val w = currentWeather
            if (w != null) {
                playWeatherBgm(w, isAutomaticDrift = false)
            }
        }
    }

    fun stopWeatherBgm() {
        synchronized(weatherLock) {
            weatherCrossfadeJob?.cancel()
            weatherCrossfadeJob = null
            weatherPlayer?.let { p ->
                try {
                    p.stop()
                    p.release()
                } catch (_: Exception) {}
            }
            weatherPlayer = null
            weatherFadingOutPlayer?.let { p ->
                try {
                    p.stop()
                    p.release()
                } catch (_: Exception) {}
            }
            weatherFadingOutPlayer = null
            try {
                weatherEqualizer?.release()
            } catch (_: Throwable) {}
            weatherEqualizer = null
        }
    }

    fun pauseAll() {
        pauseWeatherBgm()
        synchronized(musicLock) {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        mp.pause()
                        savedMpSeekPosition = mp.currentPosition
                    }
                } catch (_: Exception) {}
            }
            currentMusicTrack?.let { track ->
                try {
                    track.pause()
                    track.flush()
                } catch (_: Exception) {}
            }
        }
    }

    fun resumeAll() {
        if (!isEnabled) return
        resumeWeatherBgm()
        if (musicBoxState == MusicBoxState.PLAYING) {
            resumeMusic()
        }
    }

    private fun stopAllSfx() {
        synchronized(sfxLock) {
            val copy = activeSfxTracks.toList()
            activeSfxTracks.clear()
            for (t in copy) {
                try {
                    t.pause()
                    t.stop()
                    t.flush()
                    t.release()
                } catch (_: Exception) {}
            }
        }
    }

    @Volatile
    private var cachedThunderBuffer: ShortArray? = null
    private val thunderLock = Any()

    private fun getOrCreateThunderBuffer(): ShortArray {
        cachedThunderBuffer?.let { return it }
        synchronized(thunderLock) {
            cachedThunderBuffer?.let { return it }
            val durationMs = 2600
            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)

            // Multi-stage echo reflections: strike clap followed by cloud reverberations
            val echoes = listOf(
                Triple(0.10, 1.00, 0.15),  // Concussive strike clap
                Triple(0.45, 0.85, 0.25),  // First cloud echo wave
                Triple(1.00, 0.70, 0.35),  // Second rolling boom
                Triple(1.80, 0.50, 0.40)   // Lingering rolling echo
            )

            var lp1 = 0.0
            var lp2 = 0.0
            var bassPhase = 0.0

            for (i in 0 until numSamples) {
                val tSec = i.toDouble() / sampleRate
                val progress = tSec / 2.6

                // 1. Initial lightning strike crack (crisp electrical snap in first 80ms)
                val snapEnv = if (tSec < 0.015) tSec / 0.015 else exp(-(tSec - 0.015) * 28.0)
                val snapWhite = (kotlin.random.Random.nextDouble() * 2.0 - 1.0)
                val snapNoise = snapWhite * snapEnv * 0.55

                // 2. Multi-stage rolling thunder envelope
                var rumbleGain = 0.0
                for ((center, gain, width) in echoes) {
                    val dt = tSec - center
                    val w = if (dt < 0) width * 0.4 else width
                    val bell = exp(-(dt * dt) / (2.0 * w * w))
                    rumbleGain += bell * gain
                }
                val masterEnv = exp(-progress * 1.8) * (1.0 - exp(-tSec * 40.0))
                val totalRumble = (rumbleGain * masterEnv).coerceAtMost(1.2)

                // 3. Resonant bass sweep from 130 Hz down to 68 Hz with rich harmonics
                val bassFreq = 130.0 - progress * 62.0
                bassPhase += 2.0 * Math.PI * bassFreq / sampleRate
                val bassWave = sin(bassPhase) * 0.45 + sin(bassPhase * 1.5) * 0.30 + sin(bassPhase * 2.0) * 0.15

                // 4. Low-pass roar noise (thunder rumbling through clouds: cutoff ~350Hz)
                val white = (kotlin.random.Random.nextDouble() * 2.0 - 1.0)
                lp1 += 0.09 * (white - lp1)
                lp2 += 0.09 * (lp1 - lp2)

                // 5. Combine strike snap + deep resonant bass + rolling roar
                val combined = (bassWave * 0.50 + lp2 * 1.8) * totalRumble + snapNoise

                // Soft saturation for explosive acoustic body
                val saturated = tanh(combined * 1.5) * 0.92

                buffer[i] = (saturated * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            cachedThunderBuffer = buffer
            return buffer
        }
    }

    fun startRainAmbient() {
        if (!isEnabled || rainTrack != null) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled || rainTrack != null) return@launch
                // Prewarm thunder buffer in background so first thunder strike is instant
                scope.launch { getOrCreateThunderBuffer() }

                // 2 seconds of soothing rain ambient with seamless loop
                val durationSec = 2
                val numSamples = sampleRate * durationSec
                val rawSamples = DoubleArray(numSamples)

                // 1. Layer 1: Warm low-pass filtered bed (rain on grass, leaves, and cottage roof)
                var b0 = 0.0
                var b1 = 0.0
                var b2 = 0.0
                var brown = 0.0
                var lp1 = 0.0
                var lp2 = 0.0

                for (i in 0 until numSamples) {
                    val white = kotlin.random.Random.nextDouble() * 2.0 - 1.0
                    // Pink noise generation
                    b0 = 0.99765 * b0 + white * 0.055
                    b1 = 0.96300 * b1 + white * 0.115
                    b2 = 0.57000 * b2 + white * 0.220
                    val pink = (b0 + b1 + b2) * 0.35
                    brown = (brown + 0.06 * white) / 1.06

                    // Warm blend
                    val rainBed = pink * 0.45 + brown * 0.55

                    // Dual-pole low pass filter (cutoff ~700 Hz: removes all harsh rushing flood hiss)
                    lp1 += 0.18 * (rainBed - lp1)
                    lp2 += 0.18 * (lp1 - lp2)

                    // Gentle natural swell (soft breeze undulating rain against window)
                    val tSec = i.toDouble() / sampleRate
                    val breeze = 0.88 + 0.12 * sin(2.0 * Math.PI * tSec / 4.2)
                    rawSamples[i] = lp2 * breeze
                }

                // 2. Layer 2: Soft, gentle individual raindrop patter (cozy muffled plinks)
                val numDrops = 35
                for (d in 0 until numDrops) {
                    val dropStart = kotlin.random.Random.nextInt(numSamples - 1000)
                    val dropDur = kotlin.random.Random.nextInt(250, 550) // ~11ms - 25ms
                    val dropFreq = kotlin.random.Random.nextDouble(420.0, 880.0) // warm soothing frequencies
                    val dropVol = kotlin.random.Random.nextDouble(0.04, 0.09) // soft gentle patter
                    for (j in 0 until dropDur) {
                        val progress = j.toDouble() / dropDur
                        val decay = exp(-progress * 8.0)
                        val dropTone = sin(2.0 * Math.PI * j * dropFreq / sampleRate)
                        rawSamples[dropStart + j] += dropTone * decay * dropVol
                    }
                }

                // 3. Seamless crossfade at buffer boundaries (0.3s)
                val fadeLen = (sampleRate * 0.30).toInt()
                for (i in 0 until fadeLen) {
                    val t = i.toDouble() / fadeLen
                    val wIn = sin(t * Math.PI * 0.5)
                    val wOut = cos(t * Math.PI * 0.5)
                    val blended = rawSamples[i] * wIn + rawSamples[numSamples - fadeLen + i] * wOut
                    rawSamples[i] = blended
                    rawSamples[numSamples - fadeLen + i] = blended
                }

                // 4. Normalized master gain (gentle, clearly audible volume)
                var maxAmp = 0.0
                for (i in 0 until numSamples) {
                    val a = kotlin.math.abs(rawSamples[i])
                    if (a > maxAmp) maxAmp = a
                }
                val scale = if (maxAmp > 0.001) (0.60 / maxAmp) else 1.0
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    buffer[i] = (rawSamples[i] * scale * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val finalBytes = (buffer.size * 2).coerceAtLeast(minBuf)
                val track = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                    finalBytes,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                val written = track.write(buffer, 0, buffer.size)
                if (written > 0) {
                    track.setLoopPoints(0, buffer.size, -1)
                    val v = if (isEnabled) 1.0f else 0.0f
                    track.setVolume(v)
                    track.play()
                    rainTrack = track
                    android.util.Log.d("AmbientAudio", "startRainAmbient playing successfully")
                } else {
                    android.util.Log.e("AmbientAudio", "startRainAmbient write failed: $written")
                    track.release()
                }
            } catch (e: Exception) {
                android.util.Log.e("AmbientAudio", "startRainAmbient error", e)
            }
        }
    }

    fun stopRainAmbient() {
        try {
            rainTrack?.pause()
            rainTrack?.stop()
            rainTrack?.flush()
            rainTrack?.release()
        } catch (_: Exception) {}
        rainTrack = null
    }

    fun playThunder() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val buffer = getOrCreateThunderBuffer()
                val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    .coerceAtLeast(buffer.size * 2)
                val track = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
                track.write(buffer, 0, buffer.size)
                track.setVolume(1.0f)
                activeSfxTracks.add(track)
                track.play()
                delay(2600L)
                activeSfxTracks.remove(track)
                try {
                    track.pause()
                    track.stop()
                    track.flush()
                    track.release()
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    fun playHeartChime() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(
                    Triple(1046.50, 110, 0.45f), // C6
                    Triple(1318.51, 140, 0.45f), // E6
                    Triple(1567.98, 190, 0.45f)  // G6
                )
                for ((freq, dur, vol) in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(freq, dur, vol, isMusicBox = true)
                    delay(20)
                }
            } catch (_: Exception) {}
        }
    }

    /** Procedural mechanical instant camera shutter click with warm chime. */
    fun playCameraShutter() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                // Mechanical shutter click
                playSfxNoteStatic(1760.0, 25, 0.45f, isMusicBox = false)
                delay(35)
                playSfxNoteStatic(880.0, 40, 0.40f, isMusicBox = false)
                delay(55)
                // Gentle starlight chime
                playSfxNoteStatic(1318.51, 90, 0.35f, isMusicBox = true) // E6
                delay(25)
                playSfxNoteStatic(1760.00, 140, 0.40f, isMusicBox = true) // A6
            } catch (_: Exception) {}
        }
    }

    fun playBubblePop() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(784.0, 60, 0.38f, isMusicBox = false)
                delay(15)
                if (!isEnabled) return@launch
                playSfxNoteStatic(1174.6, 85, 0.38f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }

    fun playStarTwinkle() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(
                    Triple(1318.5, 75, 0.38f),
                    Triple(1567.98, 105, 0.38f),
                    Triple(2093.0, 140, 0.38f)
                )
                for ((freq, dur, vol) in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(freq, dur, vol, isMusicBox = true)
                    delay(15)
                }
            } catch (_: Exception) {}
        }
    }

    fun playLeafRustle() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) { playNoise(140, 0.22f) }
    }

    fun playFootstep() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (footstepTrack == null) {
                    initFootstepTrack()
                }
                footstepTrack?.apply {
                    if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                        stop()
                    }
                    reloadStaticData()
                    play()
                }
            } catch (_: Exception) {}
        }
    }

    fun playBirdChirp() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(2349.32, 60, 0.35f, isMusicBox = true)
                delay(15)
                if (!isEnabled) return@launch
                playSfxNoteStatic(2793.83, 80, 0.35f, isMusicBox = true)
            } catch (_: Exception) {}
        }
    }

    fun playCookingBubbles() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(587.33, 40, 0.30f, isMusicBox = false)
                delay(10)
                if (!isEnabled) return@launch
                playSfxNoteStatic(783.99, 45, 0.30f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }

    fun playScooterHorn() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(880.0, 60, 0.40f, isMusicBox = false)
                delay(20)
                if (!isEnabled) return@launch
                playSfxNoteStatic(1174.66, 90, 0.45f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }

    fun playCatPurr() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                repeat(4) {
                    if (!isEnabled) return@launch
                    playSfxNoteStatic(140.0, 55, 0.28f, isMusicBox = false)
                    delay(30)
                    if (!isEnabled) return@launch
                    playSfxNoteStatic(115.0, 75, 0.24f, isMusicBox = false)
                    delay(60)
                }
                if (isEnabled) {
                    playSfxNoteStatic(880.0, 45, 0.22f, isMusicBox = true)
                }
            } catch (_: Exception) {}
        }
    }

    fun playWindChime() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(1046.50, 80, 0.25f, isMusicBox = true)
                delay(40)
                if (!isEnabled) return@launch
                playSfxNoteStatic(1318.51, 90, 0.28f, isMusicBox = true)
                delay(40)
                if (!isEnabled) return@launch
                playSfxNoteStatic(1567.98, 120, 0.30f, isMusicBox = true)
            } catch (_: Exception) {}
        }
    }

    fun playThinkingOfYouChime() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(523.25, 659.25, 783.99, 1046.50)
                for (f in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(f, 70, 0.35f, isMusicBox = true)
                    delay(50)
                }
            } catch (_: Exception) {}
        }
    }

    private fun playNoise(durationMs: Int, volume: Float) {
        if (!isEnabled) return
        val numSamples = (sampleRate * durationMs / 1000).coerceAtLeast(50)
        val buffer = ShortArray(numSamples)
        var last = 0f
        for (i in 0 until numSamples) {
            val white = (Math.random() * 2.0 - 1.0).toFloat()
            last = (last + (0.05f * white)) / 1.05f
            val env = 1.0 - (i.toDouble() / numSamples)
            buffer[i] = (last * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
        val bufferBytes = buffer.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(bufferBytes)

        var track: AudioTrack? = null
        try {
            track = AudioTrack(
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
            track.write(buffer, 0, buffer.size)

            if (!isEnabled) {
                try { track.release() } catch (_: Exception) {}
                return
            }

            activeSfxTracks.add(track)
            track.play()

            val playDurationMs = (numSamples * 1000L) / sampleRate
            val deadline = System.currentTimeMillis() + playDurationMs
            while (System.currentTimeMillis() < deadline) {
                if (!isEnabled) break
                Thread.sleep(20L)
            }
        } catch (_: Exception) {
        } finally {
            if (track != null) {
                activeSfxTracks.remove(track)
                try {
                    track.pause()
                    track.stop()
                    track.flush()
                    track.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun generateToneSamples(
        frequency: Double,
        numSamples: Int,
        volume: Float = 0.55f,
        isMusicBox: Boolean = true
    ): ShortArray {
        val buffer = ShortArray(numSamples)
        val decaySamples = (numSamples * 0.85).toInt().coerceAtLeast(1)
        val attackSamples = (numSamples * 0.05).toInt().coerceIn(120, 360)
        val releaseSamples = 160.coerceAtMost(numSamples / 4)

        for (i in 0 until numSamples) {
            val t = 2.0 * Math.PI * i / (sampleRate / frequency)
            var sample = sin(t)

            if (isMusicBox) {
                // Rich harmonic celesta/music box timbre (fundamental + 2nd + 3rd + detuned chime)
                sample += 0.35 * sin(2.0 * t)
                sample += 0.18 * sin(3.0 * t)
                sample += 0.08 * sin(4.004 * t)
                sample /= 1.61
            }

            // Smooth cosine attack ramp to eliminate starting clicks
            val attackGain = if (i < attackSamples) {
                0.5 * (1.0 - cos(Math.PI * i / attackSamples))
            } else {
                1.0
            }

            // Exponential musical ring decay
            val decayGain = if (i >= attackSamples) {
                val progress = ((i - attackSamples).toDouble() / decaySamples).coerceIn(0.0, 1.0)
                exp(-2.6 * progress)
            } else {
                1.0
            }

            // Smooth cosine release fade to zero on buffer tail to eliminate ending pops
            val releaseGain = if (i >= numSamples - releaseSamples) {
                val relIdx = i - (numSamples - releaseSamples)
                0.5 * (1.0 + cos(Math.PI * relIdx / releaseSamples))
            } else {
                1.0
            }

            val envelope = attackGain * decayGain * releaseGain
            val finalSample = (sample * envelope * volume * Short.MAX_VALUE).toInt()
            buffer[i] = finalSample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    // Kitchen: clock tick sound — two short high sine pulses like clock hands
    fun playTickTick() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(1200.0, 35, 0.28f, isMusicBox = false)
                delay(80)
                if (!isEnabled) return@launch
                playSfxNoteStatic(1200.0, 35, 0.22f, isMusicBox = false)
                delay(80)
                if (!isEnabled) return@launch
                playSfxNoteStatic(1200.0, 35, 0.16f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }

    // Kitchen: wooden knock — short low square-ish burst for crate rattle
    fun playWoodKnock() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(220.0, 55, 0.32f, isMusicBox = false)
                delay(40)
                if (!isEnabled) return@launch
                playSfxNoteStatic(196.0, 50, 0.26f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }

    // Kitchen: gentle water drip — 4 descending soft sine pings for planter watering
    fun playWaterDrip() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val freqs = listOf(1200.0, 1000.0, 880.0, 740.0)
                for (f in freqs) {
                    if (!isEnabled) break
                    playSfxNoteStatic(f, 70, 0.20f, isMusicBox = true)
                    delay(55)
                }
            } catch (_: Exception) {}
        }
    }

    // Kitchen: wood creak — pitched-down burst for step-stool wobble
    fun playWoodCreak() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(320.0, 110, 0.28f, isMusicBox = false)
                delay(40)
                if (!isEnabled) return@launch
                playSfxNoteStatic(280.0, 80, 0.20f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }

    // Living Room: gentle candle flicker chime
    fun playCandleFlicker() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(659.25, 60, 0.22f, isMusicBox = true)
                delay(50)
                if (!isEnabled) return@launch
                playSfxNoteStatic(783.99, 70, 0.20f, isMusicBox = true)
            } catch (_: Exception) {}
        }
    }

    // Living Room: soft pillow thud for pouf bounce
    fun playSoftThud() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(110.0, 70, 0.32f, isMusicBox = false)
                delay(30)
                if (!isEnabled) return@launch
                playSfxNoteStatic(95.0, 50, 0.22f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }

    // Living Room: cute cat chirp for Mochi's box peek
    fun playCatChirp() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(1760.0, 45, 0.30f, isMusicBox = true)
                delay(65)
                if (!isEnabled) return@launch
                playSfxNoteStatic(2093.0, 55, 0.28f, isMusicBox = true)
            } catch (_: Exception) {}
        }
    }

    // Living Room: soft rolling sound for yarn ball
    fun playSoftRoll() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(261.63, 246.94, 220.00)
                for (n in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(n, 40, 0.24f, isMusicBox = false)
                    delay(45)
                }
            } catch (_: Exception) {}
        }
    }

    // Living Room: paper flip sound for magazine/record rack
    fun playPaperFlip() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled) return@launch
                playSfxNoteStatic(1567.98, 30, 0.24f, isMusicBox = false)
                delay(35)
                if (!isEnabled) return@launch
                playSfxNoteStatic(1760.00, 40, 0.22f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }

    // Lantern Stroll: pagoda crystal chime
    fun playPagodaChime() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(1318.51, 1567.98, 1760.00)
                for (n in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(n, 65, 0.24f, isMusicBox = true)
                    delay(55)
                }
            } catch (_: Exception) {}
        }
    }

    // Lantern Stroll: soft wind & lavender rustle
    fun playLavenderRustle() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(440.00, 392.00, 349.23)
                for (n in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(n, 80, 0.18f, isMusicBox = false)
                    delay(60)
                }
            } catch (_: Exception) {}
        }
    }

    // Lantern Stroll: bioluminescent mushroom chime
    fun playMushroomChime() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(523.25, 659.25, 783.99, 1046.50)
                for (n in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(n, 50, 0.22f, isMusicBox = true)
                    delay(45)
                }
            } catch (_: Exception) {}
        }
    }

    // Celestial: glowing constellation connect arpeggio
    fun playStarArpeggio() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(880.00, 1108.73, 1318.51, 1760.00)
                for (n in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(n, 70, 0.26f, isMusicBox = true)
                    delay(60)
                }
            } catch (_: Exception) {}
        }
    }

    // Momo Stall: neon sign hum/buzz flicker
    fun playNeonBuzz() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(220.0, 246.94, 220.0)
                for (n in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(n, 50, 0.24f, isMusicBox = false)
                    delay(45)
                }
            } catch (_: Exception) {}
        }
    }

    // Momo Stall: escaping steam hiss
    fun playSteamHiss() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(2800.0, 2400.0, 2000.0)
                for (n in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(n, 45, 0.20f, isMusicBox = false)
                    delay(40)
                }
            } catch (_: Exception) {}
        }
    }

    // Momo Stall: spicy zing chime
    fun playSpiceZing() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                playSfxNoteStatic(2093.0, 50, 0.25f, isMusicBox = true)
                delay(40)
                if (!isEnabled) return@launch
                playSfxNoteStatic(2637.0, 60, 0.22f, isMusicBox = true)
            } catch (_: Exception) {}
        }
    }

    // Momo Stall: chalk squeak on chalkboard menu
    fun playChalkSqueak() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val notes = listOf(1975.5, 2093.0, 2217.4)
                for (n in notes) {
                    if (!isEnabled) break
                    playSfxNoteStatic(n, 35, 0.20f, isMusicBox = false)
                    delay(35)
                }
            } catch (_: Exception) {}
        }
    }

    // Momo Stall: hollow bamboo crate knock
    fun playBambooKnock() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                playSfxNoteStatic(392.0, 50, 0.26f, isMusicBox = false)
                delay(45)
                if (!isEnabled) return@launch
                playSfxNoteStatic(329.63, 45, 0.22f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }

    // Momo Stall: milk saucer sip click
    fun playSaucerSip() {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                playSfxNoteStatic(880.0, 30, 0.22f, isMusicBox = false)
                delay(35)
                if (!isEnabled) return@launch
                playSfxNoteStatic(987.77, 35, 0.20f, isMusicBox = false)
            } catch (_: Exception) {}
        }
    }
}



