package com.example.scene

/** Pip the seagull's little routine at the Seaside Pier. */
enum class GullState {
    /** Off-screen, about to swoop back in. */
    AWAY,
    /** Gliding in towards a railing perch. */
    FLYING,
    PERCHED,
    /** Shuffling along the railing towards an ice cream. */
    SNEAKING,
    /** Diving at the cones. */
    STEALING,
    /** Flapping off with (or without) the loot. */
    ESCAPING;

    val isAirborne: Boolean get() = this == FLYING || this == STEALING || this == ESCAPING
    val isVisible: Boolean get() = this != AWAY
}

/** Grandpa Bao's fishing cycle: cast, wait for a nibble, reel in, show the catch. */
enum class PierFishingPhase { IDLE, CASTING, WAITING, REELING, SHOWING }

enum class PierCatch(val label: String, val message: String) {
    FISH("a little silver fish", "Grandpa Bao: 'A silver one! Mochi, this is for you.' 🐟"),
    STARFISH("a starfish", "Grandpa Bao: 'A starfish. Make a wish together before it goes home.' ⭐"),
    OLD_BOOT("an old boot", "Grandpa Bao: 'An old boot! Forty years and the sea still jokes with me.' 👢"),
    LOVE_LETTER("a letter in a jar", "Grandpa Bao: 'A letter in a jar. Some love finds its way back.' 💌"),
    SEAWEED("a tangle of seaweed", "Grandpa Bao: 'Seaweed. Patience, like love, is most of fishing.' 🌿")
}

/** Grandpa Bao's gentle pier-side wisdom, said as he casts. */
val GRANDPA_BAO_LINES = listOf(
    "Grandpa Bao: 'Fifty summers on this pier. The best catch was always company.' 🎣",
    "Grandpa Bao: 'Hold hands while the tide is kind, little ones.'",
    "Grandpa Bao: 'My wife hated fishing. She loved the sunsets. So we came for both.' 🌅",
    "Grandpa Bao: 'Patience with the sea, patience with each other.'",
    "Grandpa Bao: 'Keep that seagull away from your ice cream. He is a criminal.' 🐦",
    "Grandpa Bao: 'Every wave comes back. That is how you know it loves the shore.' 🌊",
    "Grandpa Bao: 'Cast gently. Some things only come when you stop chasing them.'",
    "Grandpa Bao: 'Share the last bite. That is the whole secret.' 🍦",
    "Grandpa Bao: 'Look at you two, smiling at the water like it told a joke.'",
    "Grandpa Bao: 'When the lighthouse blinks, it is saying goodnight to the boats.' 🗼"
)
