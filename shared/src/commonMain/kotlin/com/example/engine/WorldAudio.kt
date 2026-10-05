package com.example.engine

import com.example.scene.WeatherType

/**
 * Everything the world and its screens ask of the sound system: weather music, the music box and
 * the little one-shot sounds that answer taps. Android's AmbientAudio implements it today; iOS gets
 * its own implementation when the world moves to shared code (plan 08, S3).
 */
interface WorldAudio {
    /** The weather whose music is playing (or fading in), or null before the first scene. */
    val currentWeather: WeatherType?

    /** Sound on or off for the whole app. */
    var isEnabled: Boolean

    /** True inside rooms, where weather music plays softer. */
    val isIndoor: Boolean

    val musicBoxState: MusicBoxState
    val isPlayingMusic: Boolean
    val playlist: List<Song>
    val currentSong: Song

    // Weather and rooms
    fun playWeatherBgm(weather: WeatherType, isAutomaticDrift: Boolean = false)
    fun setIndoor(indoor: Boolean, smooth: Boolean = true)
    fun startRainAmbient()
    fun stopRainAmbient()
    fun playThunder()

    // Music box
    fun playSong(song: Song)
    fun togglePlayPause()
    fun pauseMusic()
    fun resumeMusic()
    fun nextSong()
    fun previousSong()

    // App lifecycle
    fun pauseAll()
    fun resumeAll()
    fun release()

    // One-shot sounds
    fun playHeartChime()
    fun playCameraShutter()
    fun playBubblePop()
    fun playStarTwinkle()
    fun playLeafRustle()
    fun playFootstep()
    fun playBirdChirp()
    fun playCookingBubbles()
    fun playScooterHorn()
    fun playCatPurr()
    fun playWindChime()
    fun playThinkingOfYouChime()
    fun playTickTick()
    fun playSeagullCall()
    fun playFoghorn()
    fun playBoatHorn()
    fun playReelClick()
    fun playWoodKnock()
    fun playWaterDrip()
    fun playWoodCreak()
    fun playCandleFlicker()
    fun playSoftThud()
    fun playCatChirp()
    fun playSoftRoll()
    fun playPaperFlip()
    fun playPagodaChime()
    fun playLavenderRustle()
    fun playMushroomChime()
    fun playStarArpeggio()
    fun playNeonBuzz()
    fun playSteamHiss()
    fun playSpiceZing()
    fun playChalkSqueak()
    fun playBambooKnock()
    fun playSaucerSip()
}
