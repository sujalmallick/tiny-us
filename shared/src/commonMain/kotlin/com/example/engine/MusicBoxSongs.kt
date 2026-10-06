package com.example.engine

/** The music box's original chiptune melodies (no licensed songs or melodies are bundled). */
object MusicBoxSongs {
    /** Silence between one play-through and the next. */
    const val LOOP_GAP_MS = 1200L

    val playlist: List<Song> = listOf(
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

    /** The music box's note volume. */
    const val NOTE_VOLUME = 0.65f

    /** Pause after [note] before the next one starts. */
    fun gapAfterMs(note: MusicalNote): Long = (note.delayAfterMs - note.durationMs).coerceAtLeast(20L)

    /** One play-through of [song] and the pause before it repeats, as one buffer to loop. */
    fun render(song: Song, sampleRate: Int = ToneSynth.SAMPLE_RATE): ShortArray {
        val parts = ArrayList<ShortArray>()
        for (note in song.notes) {
            if (note.freq > 0.0) {
                parts += ToneSynth.tone(note.freq, ToneSynth.samplesFor(note.durationMs, 64, sampleRate), NOTE_VOLUME, true, sampleRate)
            }
            parts += ShortArray((sampleRate * gapAfterMs(note) / 1000).toInt())
        }
        parts += ShortArray((sampleRate * LOOP_GAP_MS / 1000).toInt())
        val out = ShortArray(parts.sumOf { it.size })
        var at = 0
        for (part in parts) {
            part.copyInto(out, at)
            at += part.size
        }
        return out
    }
}
