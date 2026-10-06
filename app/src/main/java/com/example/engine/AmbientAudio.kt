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

class AmbientAudio(var context: Context? = null) : WorldAudio {
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

    override var currentWeather: WeatherType? by mutableStateOf(null)
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

    override var isIndoor: Boolean = false
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

    override var musicBoxState: MusicBoxState by mutableStateOf(MusicBoxState.STOPPED)
        private set

    override val isPlayingMusic: Boolean
        get() = musicBoxState == MusicBoxState.PLAYING

    private var currentNoteIndex: Int = 0

    private fun initFootstepTrack() {
        if (footstepTrack != null) return
        try {
            val buffer = ProceduralAudioSynthesizer.synthesizeFootstep(sampleRate)
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
    override var isEnabled: Boolean = true
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

    // Offline music box: original chiptune melodies, shared with iOS
    override val playlist: List<Song> = MusicBoxSongs.playlist

    override var currentSong: Song by mutableStateOf(playlist[0])
        private set

    override fun playSong(song: Song) {
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

    override fun togglePlayPause() {
        when (musicBoxState) {
            MusicBoxState.PLAYING -> pauseMusic()
            MusicBoxState.PAUSED -> resumeMusic()
            MusicBoxState.STOPPED -> playSong(currentSong)
        }
    }

    override fun pauseMusic() {
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

    override fun resumeMusic() {
        if (musicBoxState == MusicBoxState.PLAYING) return
        musicBoxState = MusicBoxState.PLAYING
        setMusicBoxDucking(duck = true, smooth = true)
        scope.launch {
            startPlaybackJob()
        }
    }

    override fun nextSong() {
        val nextIdx = (playlist.indexOf(currentSong) + 1) % playlist.size
        playSong(playlist[nextIdx])
    }

    override fun previousSong() {
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
        val buffer = ToneSynth.tone(freq, numSamples, volume, isMusicBox, sampleRate)
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
                        val tone = ToneSynth.tone(note.freq, numSamples, MusicBoxSongs.NOTE_VOLUME, isMusicBox = true, sampleRate = sampleRate)
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
                    val gapMs = MusicBoxSongs.gapAfterMs(note)
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
                    val loopGapSamples = (sampleRate * MusicBoxSongs.LOOP_GAP_MS / 1000).toInt()
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

    override fun release() {
        stopMusic()
        stopWeatherBgm()
        stopAllSfx()
        stopRainAmbient()
        try {
            footstepTrack?.release()
            footstepTrack = null
        } catch (_: Exception) {}
    }

    override fun setIndoor(indoor: Boolean, smooth: Boolean) {
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

    override fun playWeatherBgm(weather: WeatherType, isAutomaticDrift: Boolean) {
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

    override fun pauseAll() {
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

    override fun resumeAll() {
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
            val buffer = ToneSynth.thunder(sampleRate)
            cachedThunderBuffer = buffer
            return buffer
        }
    }

    override fun startRainAmbient() {
        if (!isEnabled || rainTrack != null) return
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled || rainTrack != null) return@launch
                // Prewarm thunder buffer in background so first thunder strike is instant
                scope.launch { getOrCreateThunderBuffer() }

                // 2 seconds of soothing rain ambient with seamless loop
                val buffer = ToneSynth.rainLoop(sampleRate)

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

    override fun stopRainAmbient() {
        try {
            rainTrack?.pause()
            rainTrack?.stop()
            rainTrack?.flush()
            rainTrack?.release()
        } catch (_: Exception) {}
        rainTrack = null
    }

    override fun playThunder() {
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

    override fun playHeartChime() = playCue(SoundCue.HEART_CHIME)

    /** Procedural mechanical instant camera shutter click with warm chime. */
    override fun playCameraShutter() = playCue(SoundCue.CAMERA_SHUTTER)

    override fun playBubblePop() = playCue(SoundCue.BUBBLE_POP)

    override fun playStarTwinkle() = playCue(SoundCue.STAR_TWINKLE)

    override fun playLeafRustle() = playCue(SoundCue.LEAF_RUSTLE)

    override fun playFootstep() {
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

    override fun playBirdChirp() = playCue(SoundCue.BIRD_CHIRP)

    override fun playCookingBubbles() = playCue(SoundCue.COOKING_BUBBLES)

    override fun playScooterHorn() = playCue(SoundCue.SCOOTER_HORN)

    override fun playCatPurr() = playCue(SoundCue.CAT_PURR)

    override fun playWindChime() = playCue(SoundCue.WIND_CHIME)

    override fun playThinkingOfYouChime() = playCue(SoundCue.THINKING_OF_YOU)

    /** Plays a shared sound recipe note by note, exactly as the world's sounds always played. */
    private fun playCue(cue: SoundCue) {
        if (!isEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                for (step in cue.steps) {
                    if (!isEnabled) break
                    when (step) {
                        is SoundStep.Note -> playSfxNoteStatic(step.freq, step.ms, step.volume, step.musicBox)
                        is SoundStep.Gap -> delay(step.ms.toLong())
                        is SoundStep.Noise -> playNoise(step.ms, step.volume)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun playNoise(durationMs: Int, volume: Float) {
        if (!isEnabled) return
        val numSamples = (sampleRate * durationMs / 1000).coerceAtLeast(50)
        val buffer = ToneSynth.noise(numSamples, volume)
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

    // Kitchen: the clock ticking
    override fun playTickTick() = playCue(SoundCue.TICK_TICK)

    // Seaside Pier: Pip's cheeky two-note squawk, twice
    override fun playSeagullCall() = playCue(SoundCue.SEAGULL_CALL)

    // Seaside Pier: soft, low lighthouse foghorn (root and fifth)
    override fun playFoghorn() = playCue(SoundCue.FOGHORN)

    // Seaside Pier: the little sailboat's cheerful "toot toot"
    override fun playBoatHorn() = playCue(SoundCue.BOAT_HORN)

    // Seaside Pier: Grandpa Bao's fishing reel ticking
    override fun playReelClick() = playCue(SoundCue.REEL_CLICK)

    override fun playWoodKnock() = playCue(SoundCue.WOOD_KNOCK)

    // Kitchen: gentle water drip — 4 descending soft sine pings for planter watering
    override fun playWaterDrip() = playCue(SoundCue.WATER_DRIP)

    // Kitchen: wood creak — pitched-down burst for step-stool wobble
    override fun playWoodCreak() = playCue(SoundCue.WOOD_CREAK)

    // Living Room: gentle candle flicker chime
    override fun playCandleFlicker() = playCue(SoundCue.CANDLE_FLICKER)

    // Living Room: soft pillow thud for pouf bounce
    override fun playSoftThud() = playCue(SoundCue.SOFT_THUD)

    // Living Room: cute cat chirp for Mochi's box peek
    override fun playCatChirp() = playCue(SoundCue.CAT_CHIRP)

    // Living Room: soft rolling sound for yarn ball
    override fun playSoftRoll() = playCue(SoundCue.SOFT_ROLL)

    // Living Room: paper flip sound for magazine/record rack
    override fun playPaperFlip() = playCue(SoundCue.PAPER_FLIP)

    // Lantern Stroll: pagoda crystal chime
    override fun playPagodaChime() = playCue(SoundCue.PAGODA_CHIME)

    // Lantern Stroll: soft wind & lavender rustle
    override fun playLavenderRustle() = playCue(SoundCue.LAVENDER_RUSTLE)

    // Lantern Stroll: bioluminescent mushroom chime
    override fun playMushroomChime() = playCue(SoundCue.MUSHROOM_CHIME)

    // Celestial: glowing constellation connect arpeggio
    override fun playStarArpeggio() = playCue(SoundCue.STAR_ARPEGGIO)

    // Momo Stall: neon sign hum/buzz flicker
    override fun playNeonBuzz() = playCue(SoundCue.NEON_BUZZ)

    // Momo Stall: escaping steam hiss
    override fun playSteamHiss() = playCue(SoundCue.STEAM_HISS)

    // Momo Stall: spicy zing chime
    override fun playSpiceZing() = playCue(SoundCue.SPICE_ZING)

    // Momo Stall: chalk squeak on chalkboard menu
    override fun playChalkSqueak() = playCue(SoundCue.CHALK_SQUEAK)

    // Momo Stall: hollow bamboo crate knock
    override fun playBambooKnock() = playCue(SoundCue.BAMBOO_KNOCK)

    // Momo Stall: milk saucer sip click
    override fun playSaucerSip() = playCue(SoundCue.SAUCER_SIP)
}



