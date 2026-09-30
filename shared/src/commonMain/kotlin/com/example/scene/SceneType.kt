package com.example.scene

enum class SceneType(
    val title: String,
    val subtitle: String,
    val environment: EnvironmentType
) {
    FLOWER(
        title = "A Flower For You",
        subtitle = "A small surprise to brighten your day",
        environment = EnvironmentType.MEADOW
    ),
    UNDER_TREE(
        title = "Under Our Tree",
        subtitle = "Quiet moments beneath the leaves",
        environment = EnvironmentType.TREE_HILL
    ),
    COOKING(
        title = "Kitchen Secret",
        subtitle = "Someone is stealing delicious bites!",
        environment = EnvironmentType.KITCHEN
    ),
    SLEEP(
        title = "Couch Snooze",
        subtitle = "Curling up together as the night dims",
        environment = EnvironmentType.LIVING_ROOM
    ),
    WALK(
        title = "Lantern Stroll",
        subtitle = "Walking side by side under the evening stars",
        environment = EnvironmentType.PATH_NIGHT
    ),
    LOOKING(
        title = "Just Looking at You",
        subtitle = "Soft silence and a gentle head pat",
        environment = EnvironmentType.TWILIGHT
    ),
    MOMO_STALL(
        title = "Street Food Date",
        subtitle = "Sharing warm dumplings at our cozy stall",
        environment = EnvironmentType.MOMO_STALL
    ),
    EVENING_RIDE(
        title = "Evening Ride",
        subtitle = "Riding our scooter together through the evening breezes",
        environment = EnvironmentType.EVENING_ROAD
    ),
    COZY_LOFT(
        title = "Midnight Loft",
        subtitle = "Cuddles, tea, and warm vinyl over the glowing city skyline",
        environment = EnvironmentType.COZY_LOFT
    )
}

enum class EnvironmentType {
    MEADOW,
    TREE_HILL,
    KITCHEN,
    LIVING_ROOM,
    PATH_NIGHT,
    TWILIGHT,
    MOMO_STALL,
    EVENING_ROAD,
    COZY_LOFT
}

enum class WeatherType(val displayName: String) {
    SUNNY("Sunny Breeze"),
    RAIN("Cozy Rain"),
    SAKURA("Cherry Blossoms"),
    AUTUMN("Autumn Leaves"),
    SNOW("Gentle Snow")
}

enum class CatState {
    SLEEPING,
    SITTING_PURR,
    BELLY_ROLL,
    PLAYFUL_POUNCE,
    WALK_FOLLOW
}

/** Visual time-of-day phases used exclusively by the Couch Snooze scene.
 *  Never touches atmosphereMode or the global isNight/isSunset used by other scenes. */
enum class CouchPhase { NIGHT, DAY, MIDDAY, EVENING }
