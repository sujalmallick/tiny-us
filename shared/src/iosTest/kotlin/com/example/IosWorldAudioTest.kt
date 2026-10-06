package com.example

import com.example.engine.IosWorldAudio
import com.example.engine.MusicBoxState
import com.example.scene.WeatherType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Drives every part of the iOS sound system on the simulator, through the real AVAudioPlayer calls. */
class IosWorldAudioTest {
    @Test
    fun everySoundPlaysAndStops() {
        val audio = IosWorldAudio()
        audio.volume = 0.1f
        audio.playWeatherBgm(WeatherType.RAIN) // no bundled mp3 in the test binary: skipped quietly
        assertEquals(WeatherType.RAIN, audio.currentWeather)
        audio.setIndoor(true, smooth = true)
        audio.startRainAmbient()
        audio.playThunder()
        audio.playFootstep()
        audio.playHeartChime(); audio.playCameraShutter(); audio.playBubblePop(); audio.playStarTwinkle()
        audio.playLeafRustle(); audio.playBirdChirp(); audio.playCookingBubbles(); audio.playScooterHorn()
        audio.playCatPurr(); audio.playWindChime(); audio.playThinkingOfYouChime(); audio.playTickTick()
        audio.playSeagullCall(); audio.playFoghorn(); audio.playBoatHorn(); audio.playReelClick()
        audio.playWoodKnock(); audio.playWaterDrip(); audio.playWoodCreak(); audio.playCandleFlicker()
        audio.playSoftThud(); audio.playCatChirp(); audio.playSoftRoll(); audio.playPaperFlip()
        audio.playPagodaChime(); audio.playLavenderRustle(); audio.playMushroomChime(); audio.playStarArpeggio()
        audio.playNeonBuzz(); audio.playSteamHiss(); audio.playSpiceZing(); audio.playChalkSqueak()
        audio.playBambooKnock(); audio.playSaucerSip()

        audio.playSong(audio.playlist[1])
        assertEquals(MusicBoxState.PLAYING, audio.musicBoxState)
        audio.nextSong()
        audio.togglePlayPause()
        assertEquals(MusicBoxState.PAUSED, audio.musicBoxState)

        audio.pauseAll()
        audio.resumeAll()
        audio.isEnabled = false
        audio.playHeartChime()
        audio.isEnabled = true
        audio.release()
        assertNull(audio.currentWeather)
        assertEquals(MusicBoxState.STOPPED, audio.musicBoxState)
    }
}
