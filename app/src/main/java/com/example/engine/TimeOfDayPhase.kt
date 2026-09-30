package com.example.engine

/**
 * Single source of truth for the real-clock time phase across Tiny Us.
 *
 * Thresholds:
 * - MORNING:   05:00 - 07:59 (hours 5..7)
 * - AFTERNOON: 08:00 - 16:59 (hours 8..16)
 * - SUNSET:    17:00 - 19:59 (hours 17..19)
 * - NIGHT:     20:00 - 04:59 (hours 20..23 and 0..4)
 */
enum class TimeOfDayPhase {
    MORNING,
    AFTERNOON,
    SUNSET,
    NIGHT;

    val isNight: Boolean get() = this == NIGHT
    val isSunset: Boolean get() = this == SUNSET
    val isMorning: Boolean get() = this == MORNING
    val isDay: Boolean get() = this == AFTERNOON || this == MORNING

    companion object {
        /**
         * Optional debug override. Only effective in debug builds or tests.
         */
        var debugOverride: TimeOfDayPhase? = null

        /**
         * Computes the phase from a 24-hour hour integer (0..23).
         */
        fun fromHour(hour: Int): TimeOfDayPhase {
            return when (hour) {
                in 5..7 -> MORNING
                in 8..16 -> AFTERNOON
                in 17..19 -> SUNSET
                else -> NIGHT
            }
        }

        /**
         * Resolves the effective phase given atmosphereMode setting and the hour.
         * Mode mapping:
         * - "DAY" -> AFTERNOON
         * - "SUNSET" -> SUNSET
         * - "NIGHT" -> NIGHT
         * - "AUTO" (or others) -> real-clock evaluation via fromHour(hour)
         */
        fun resolve(
            atmosphereMode: String,
            hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        ): TimeOfDayPhase {
            if (com.example.BuildConfig.DEBUG) {
                debugOverride?.let { return it }
            }
            return when (atmosphereMode) {
                "DAY" -> AFTERNOON
                "SUNSET" -> SUNSET
                "NIGHT" -> NIGHT
                else -> fromHour(hour)
            }
        }
    }
}
