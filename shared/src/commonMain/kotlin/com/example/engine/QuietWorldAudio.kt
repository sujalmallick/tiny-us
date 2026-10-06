package com.example.engine

import com.example.scene.WeatherType

/**
 * A sound system that makes no sound: it keeps the state the world reads (weather, indoors,
 * music box) but plays nothing. Used for headless worlds (tests, previews) and wherever a silent
 * world is wanted.
 */
class QuietWorldAudio : WorldAudio {
    override var currentWeather: WeatherType? = null
        private set
    override var isEnabled: Boolean = false
    override var isIndoor: Boolean = false
        private set
    override var musicBoxState: MusicBoxState = MusicBoxState.STOPPED
        private set
    override val isPlayingMusic: Boolean get() = musicBoxState == MusicBoxState.PLAYING
    override val playlist: List<Song> = emptyList()
    override val currentSong: Song = Song(id = "quiet", title = "", artist = "", vibe = "")

    override fun playWeatherBgm(weather: WeatherType, isAutomaticDrift: Boolean) {
        currentWeather = weather
    }

    override fun setIndoor(indoor: Boolean, smooth: Boolean) {
        isIndoor = indoor
    }

    override fun startRainAmbient() = Unit
    override fun stopRainAmbient() = Unit
    override fun playThunder() = Unit

    override fun playSong(song: Song) {
        musicBoxState = MusicBoxState.PLAYING
    }

    override fun togglePlayPause() {
        musicBoxState = if (musicBoxState == MusicBoxState.PLAYING) MusicBoxState.PAUSED else MusicBoxState.PLAYING
    }

    override fun pauseMusic() {
        if (musicBoxState == MusicBoxState.PLAYING) musicBoxState = MusicBoxState.PAUSED
    }

    override fun resumeMusic() {
        if (musicBoxState == MusicBoxState.PAUSED) musicBoxState = MusicBoxState.PLAYING
    }

    override fun nextSong() = Unit
    override fun previousSong() = Unit
    override fun pauseAll() = Unit
    override fun resumeAll() = Unit
    override fun release() = Unit

    override fun playHeartChime() = Unit
    override fun playCameraShutter() = Unit
    override fun playBubblePop() = Unit
    override fun playStarTwinkle() = Unit
    override fun playLeafRustle() = Unit
    override fun playFootstep() = Unit
    override fun playBirdChirp() = Unit
    override fun playCookingBubbles() = Unit
    override fun playScooterHorn() = Unit
    override fun playCatPurr() = Unit
    override fun playWindChime() = Unit
    override fun playThinkingOfYouChime() = Unit
    override fun playTickTick() = Unit
    override fun playSeagullCall() = Unit
    override fun playFoghorn() = Unit
    override fun playBoatHorn() = Unit
    override fun playReelClick() = Unit
    override fun playWoodKnock() = Unit
    override fun playWaterDrip() = Unit
    override fun playWoodCreak() = Unit
    override fun playCandleFlicker() = Unit
    override fun playSoftThud() = Unit
    override fun playCatChirp() = Unit
    override fun playSoftRoll() = Unit
    override fun playPaperFlip() = Unit
    override fun playPagodaChime() = Unit
    override fun playLavenderRustle() = Unit
    override fun playMushroomChime() = Unit
    override fun playStarArpeggio() = Unit
    override fun playNeonBuzz() = Unit
    override fun playSteamHiss() = Unit
    override fun playSpiceZing() = Unit
    override fun playChalkSqueak() = Unit
    override fun playBambooKnock() = Unit
    override fun playSaucerSip() = Unit
}
