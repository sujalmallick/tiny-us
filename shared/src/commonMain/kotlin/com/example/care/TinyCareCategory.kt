package com.example.care

enum class TinyCareCategory(
    val id: String,
    val title: String,
    val description: String
) {
    HYDRATION(
        id = "hydration",
        title = "Hydration",
        description = "Gentle reminders to drink water"
    ),
    BREAKS(
        id = "breaks",
        title = "Breaks",
        description = "Pauses to rest your mind and breathe"
    ),
    FOOD(
        id = "food",
        title = "Nourishment",
        description = "Reminders to eat and refuel"
    ),
    MOVEMENT(
        id = "movement",
        title = "Movement",
        description = "Stretches and posture check-ins"
    ),
    SLEEP(
        id = "sleep",
        title = "Rest and Sleep",
        description = "Winding down as the evening ends"
    ),
    GENERAL(
        id = "general",
        title = "Quiet Care",
        description = "Kind thoughts and resting your eyes"
    );

    companion object {
        fun fromId(id: String): TinyCareCategory? = values().firstOrNull { it.id == id }
        fun allIds(): Set<String> = values().map { it.id }.toSet()
    }
}
