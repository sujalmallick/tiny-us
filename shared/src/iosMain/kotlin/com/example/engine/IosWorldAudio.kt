@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.example.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.scene.WeatherType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Whole packages: some of these calls are Objective-C category methods (Kotlin extensions).
import platform.AVFAudio.*
import platform.Foundation.*

/**
 * The world's sound on iOS, matching Android's AmbientAudio: the same weather music (the bundled
 * bgm mp3s), softer indoors and while the music box plays, crossfading when the weather drifts;
 * the same synthesized taps, thunder, rain and music-box songs (shared [SoundCue], [ToneSynth] and
 * [MusicBoxSongs]), each rendered once to a WAV and played with AVAudioPlayer. Runs on the main
 * thread, like the world that calls it.
 */
class IosWorldAudio : WorldAudio {
    private val scope = MainScope()

    override var currentWeather: WeatherType? by mutableStateOf(null)
        private set

    override var isIndoor: Boolean = false
        private set

    override var musicBoxState: MusicBoxState by mutableStateOf(MusicBoxState.STOPPED)
        private set

    override val isPlayingMusic: Boolean get() = musicBoxState == MusicBoxState.PLAYING

    override val playlist: List<Song> = MusicBoxSongs.playlist

    override var currentSong: Song by mutableStateOf(playlist[0])
        private set

    /** The world volume from Settings (0..1). */
    var volume: Float = 0.72f
        set(value) {
            field = value.coerceIn(0f, 1f)
            applyVolumes(fadeSeconds = 0.2)
        }

    override var isEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (value) resumePlayers() else pausePlayers()
        }

    /** False while the app is in the background. */
    private var inForeground = true

    private var weatherPlayer: AVAudioPlayer? = null
    private val fadingOut = mutableListOf<AVAudioPlayer>()
    private var rainPlayer: AVAudioPlayer? = null
    private var musicPlayer: AVAudioPlayer? = null
    private var musicSongId: String? = null
    private val sfxPlayers = mutableListOf<AVAudioPlayer>()
    private val cueData = mutableMapOf<SoundCue, NSData>()
    private val thunderData by lazy { wavData(ToneSynth.thunder()) }
    private val footstepData by lazy { wavData(ProceduralAudioSynthesizer.synthesizeFootstep(ToneSynth.SAMPLE_RATE)) }
    private var sessionReady = false

    private val audible: Boolean get() = isEnabled && inForeground
    private val master: Float get() = if (audible) volume else 0f

    /** Weather music: softer indoors (0.4) and under the music box (0.25), as on Android. */
    private val weatherVolume: Float
        get() = master * (if (isIndoor) 0.40f else 1f) * (if (isPlayingMusic) 0.25f else 1f)

    // Weather and rooms

    override fun playWeatherBgm(weather: WeatherType, isAutomaticDrift: Boolean) {
        val sameWeather = currentWeather == weather
        currentWeather = weather
        if (sameWeather && weatherPlayer?.playing == true) return
        if (!audible) return // starts when sound comes back
        val next = bundledLoop("bgm_" + weather.name.lowercase()) ?: return
        val previous = weatherPlayer
        weatherPlayer = next
        if (isAutomaticDrift && previous != null) {
            // The season moving on: a slow crossfade.
            next.volume = 0f
            next.play()
            next.setVolume(weatherVolume, fadeDuration = 2.5)
            fadeOutAndStop(previous, 2.5)
        } else {
            // Picked by hand or a new start: switch straight away.
            previous?.stop()
            next.volume = weatherVolume
            next.play()
        }
    }

    override fun setIndoor(indoor: Boolean, smooth: Boolean) {
        isIndoor = indoor
        weatherPlayer?.setVolume(weatherVolume, fadeDuration = if (smooth) 0.8 else 0.0)
    }

    override fun startRainAmbient() {
        if (rainPlayer != null) return
        val player = dataPlayer(wavData(ToneSynth.rainLoop())) ?: return
        player.numberOfLoops = -1
        player.volume = master
        rainPlayer = player
        if (audible) player.play()
    }

    override fun stopRainAmbient() {
        rainPlayer?.stop()
        rainPlayer = null
    }

    override fun playThunder() = playData(thunderData)

    // Music box

    override fun playSong(song: Song) {
        if (song.id != musicSongId || musicPlayer == null) {
            musicPlayer?.stop()
            musicPlayer = dataPlayer(wavData(MusicBoxSongs.render(song)))?.apply { numberOfLoops = -1 }
            musicSongId = song.id
        }
        currentSong = song
        musicBoxState = MusicBoxState.PLAYING
        musicPlayer?.volume = master
        if (audible) musicPlayer?.play()
        applyVolumes(fadeSeconds = 0.8)
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
        musicPlayer?.pause()
        applyVolumes(fadeSeconds = 0.8)
    }

    override fun resumeMusic() {
        if (musicBoxState == MusicBoxState.PLAYING) return
        playSong(currentSong)
    }

    override fun nextSong() {
        val nextIdx = (playlist.indexOf(currentSong) + 1) % playlist.size
        playSong(playlist[nextIdx])
    }

    override fun previousSong() {
        val currentIdx = playlist.indexOf(currentSong)
        playSong(playlist[if (currentIdx <= 0) playlist.size - 1 else currentIdx - 1])
    }

    // App lifecycle

    override fun pauseAll() {
        inForeground = false
        pausePlayers()
    }

    override fun resumeAll() {
        inForeground = true
        if (isEnabled) resumePlayers()
    }

    /** Stops everything and forgets the weather, so a new world starts its music afresh. */
    override fun release() {
        weatherPlayer?.stop()
        weatherPlayer = null
        fadingOut.forEach { it.stop() }
        fadingOut.clear()
        stopRainAmbient()
        musicPlayer?.stop()
        musicPlayer = null
        musicSongId = null
        musicBoxState = MusicBoxState.STOPPED
        stopSfx()
        currentWeather = null
    }

    // One-shot sounds

    override fun playHeartChime() = playCue(SoundCue.HEART_CHIME)
    override fun playCameraShutter() = playCue(SoundCue.CAMERA_SHUTTER)
    override fun playBubblePop() = playCue(SoundCue.BUBBLE_POP)
    override fun playStarTwinkle() = playCue(SoundCue.STAR_TWINKLE)
    override fun playLeafRustle() = playCue(SoundCue.LEAF_RUSTLE)
    override fun playFootstep() = playData(footstepData)
    override fun playBirdChirp() = playCue(SoundCue.BIRD_CHIRP)
    override fun playCookingBubbles() = playCue(SoundCue.COOKING_BUBBLES)
    override fun playScooterHorn() = playCue(SoundCue.SCOOTER_HORN)
    override fun playCatPurr() = playCue(SoundCue.CAT_PURR)
    override fun playWindChime() = playCue(SoundCue.WIND_CHIME)
    override fun playThinkingOfYouChime() = playCue(SoundCue.THINKING_OF_YOU)
    override fun playTickTick() = playCue(SoundCue.TICK_TICK)
    override fun playSeagullCall() = playCue(SoundCue.SEAGULL_CALL)
    override fun playFoghorn() = playCue(SoundCue.FOGHORN)
    override fun playBoatHorn() = playCue(SoundCue.BOAT_HORN)
    override fun playReelClick() = playCue(SoundCue.REEL_CLICK)
    override fun playWoodKnock() = playCue(SoundCue.WOOD_KNOCK)
    override fun playWaterDrip() = playCue(SoundCue.WATER_DRIP)
    override fun playWoodCreak() = playCue(SoundCue.WOOD_CREAK)
    override fun playCandleFlicker() = playCue(SoundCue.CANDLE_FLICKER)
    override fun playSoftThud() = playCue(SoundCue.SOFT_THUD)
    override fun playCatChirp() = playCue(SoundCue.CAT_CHIRP)
    override fun playSoftRoll() = playCue(SoundCue.SOFT_ROLL)
    override fun playPaperFlip() = playCue(SoundCue.PAPER_FLIP)
    override fun playPagodaChime() = playCue(SoundCue.PAGODA_CHIME)
    override fun playLavenderRustle() = playCue(SoundCue.LAVENDER_RUSTLE)
    override fun playMushroomChime() = playCue(SoundCue.MUSHROOM_CHIME)
    override fun playStarArpeggio() = playCue(SoundCue.STAR_ARPEGGIO)
    override fun playNeonBuzz() = playCue(SoundCue.NEON_BUZZ)
    override fun playSteamHiss() = playCue(SoundCue.STEAM_HISS)
    override fun playSpiceZing() = playCue(SoundCue.SPICE_ZING)
    override fun playChalkSqueak() = playCue(SoundCue.CHALK_SQUEAK)
    override fun playBambooKnock() = playCue(SoundCue.BAMBOO_KNOCK)
    override fun playSaucerSip() = playCue(SoundCue.SAUCER_SIP)

    // Players

    private fun playCue(cue: SoundCue) {
        if (!audible) return
        playData(cueData.getOrPut(cue) { wavData(cue.render()) })
    }

    private fun playData(data: NSData) {
        if (!audible) return
        sfxPlayers.removeAll { !it.playing }
        if (sfxPlayers.size >= MAX_SFX) sfxPlayers.removeAt(0).stop()
        val player = dataPlayer(data) ?: return
        player.volume = master
        player.play()
        sfxPlayers += player
    }

    private fun stopSfx() {
        sfxPlayers.forEach { it.stop() }
        sfxPlayers.clear()
    }

    private fun pausePlayers() {
        stopSfx()
        weatherPlayer?.pause()
        fadingOut.forEach { it.stop() }
        fadingOut.clear()
        rainPlayer?.pause()
        musicPlayer?.pause()
    }

    private fun resumePlayers() {
        val weather = currentWeather
        val player = weatherPlayer
        if (player != null) {
            player.volume = weatherVolume
            player.play()
        } else if (weather != null) {
            playWeatherBgm(weather, isAutomaticDrift = false)
        }
        rainPlayer?.let { it.volume = master; it.play() }
        if (musicBoxState == MusicBoxState.PLAYING) musicPlayer?.let { it.volume = master; it.play() }
    }

    private fun applyVolumes(fadeSeconds: Double) {
        weatherPlayer?.setVolume(weatherVolume, fadeDuration = fadeSeconds)
        rainPlayer?.volume = master
        musicPlayer?.volume = master
    }

    private fun fadeOutAndStop(player: AVAudioPlayer, seconds: Double) {
        fadingOut += player
        player.setVolume(0f, fadeDuration = seconds)
        scope.launch {
            delay((seconds * 1000).toLong() + 100)
            player.stop()
            fadingOut.remove(player)
        }
    }

    /** Ambient sound mixes with other apps' audio and follows the silent switch, like the classic world. */
    private fun ensureSession() {
        if (sessionReady) return
        sessionReady = true
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryAmbient, withOptions = AVAudioSessionCategoryOptionMixWithOthers, error = null)
        session.setActive(true, error = null)
    }

    private fun bundledLoop(name: String): AVAudioPlayer? {
        val url = NSBundle.mainBundle.URLForResource(name, withExtension = "mp3") ?: return null
        ensureSession()
        val player = runCatching { AVAudioPlayer(contentsOfURL = url, error = null) }.getOrNull() ?: return null
        player.numberOfLoops = -1
        player.prepareToPlay()
        return player
    }

    private fun dataPlayer(data: NSData): AVAudioPlayer? {
        ensureSession()
        val player = runCatching { AVAudioPlayer(data = data, error = null) }.getOrNull() ?: return null
        player.prepareToPlay()
        return player
    }

    private fun wavData(samples: ShortArray): NSData {
        val bytes = ToneSynth.wav(samples)
        return bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    }

    private companion object {
        /** Taps can come quickly; the oldest little sound gives way. */
        const val MAX_SFX = 10
    }
}
