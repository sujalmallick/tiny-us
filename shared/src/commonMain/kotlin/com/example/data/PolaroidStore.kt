@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.example.data

import kotlin.uuid.Uuid

/**
 * The couple's Polaroid photos as saved data: the list (newest first, at most 60), the titles they
 * get and the date and time written on them. Common code on [KeyValueStorage] with the keys and
 * JSON Android always used ("tiny_us_polaroids"); each platform stores the images themselves and
 * removes them through [deleteImage].
 */
class PolaroidStore(
    private val storage: KeyValueStorage,
    private val deleteImage: (path: String) -> Unit = {}
) {
    fun getPolaroids(): List<PolaroidMemory> {
        val raw = storage.getString("polaroids_json", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                PolaroidMemory(
                    id = obj.optString("id", Uuid.random().toString()),
                    title = obj.optString("title", "Tiny Moment"),
                    date = obj.optString("date", ""),
                    time = obj.optString("time", ""),
                    sceneName = obj.optString("sceneName", ""),
                    sceneEnvKey = obj.optString("sceneEnvKey", ""),
                    imagePath = obj.optString("imagePath", "")
                )
            }.reversed() // newest first
        }.getOrDefault(emptyList())
    }

    fun savePolaroid(memory: PolaroidMemory) {
        // Insert newest at front; keep at most 60
        val current = getPolaroids().toMutableList()
        current.add(0, memory)
        write(current.take(60))
    }

    fun deletePolaroid(id: String) {
        val current = getPolaroids().toMutableList()
        val target = current.firstOrNull { it.id == id } ?: return
        deleteImage(target.imagePath)
        current.removeAll { it.id == id }
        write(current)
    }

    /** Stored oldest-first, so reading reverses it to newest-first. */
    private fun write(newestFirst: List<PolaroidMemory>) {
        val arr = JSONArray()
        newestFirst.reversed().forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("date", m.date)
                put("time", m.time)
                put("sceneName", m.sceneName)
                put("sceneEnvKey", m.sceneEnvKey)
                put("imagePath", m.imagePath)
            })
        }
        storage.putString("polaroids_json", arr.toString())
    }

    /**
     * Picks a cozy title for the captured moment. Biased toward scene-specific
     * and time-appropriate titles when available, cycling through to avoid repetition.
     */
    fun pickTitle(sceneEnvKey: String, isNight: Boolean = false, isSunset: Boolean = false): String {
        val sceneTitles = SCENE_TITLES[sceneEnvKey]
        val timeTitles = when {
            isNight -> TIME_TITLES_NIGHT
            isSunset -> TIME_TITLES_SUNSET
            else -> TIME_TITLES_DAY
        }

        // Build weighted pool: scene-specific x3, time-aware x2, universal x1
        val pool = mutableListOf<String>()
        sceneTitles?.let { pool.addAll(it); pool.addAll(it); pool.addAll(it) }
        pool.addAll(timeTitles); pool.addAll(timeTitles)
        pool.addAll(UNIVERSAL_TITLES)

        // Anti-repeat: exclude last 20 used titles to keep fresh
        val usedRaw = storage.getString("used_titles", "") ?: ""
        val used = if (usedRaw.isBlank()) emptyList() else usedRaw.split("|")
        val candidates = pool.filter { it !in used }.ifEmpty { pool }

        val picked = candidates.random()
        storage.putString("used_titles", (used + picked).takeLast(20).joinToString("|"))
        return picked
    }

    /** Today, as written on a new Polaroid ("Oct 6, 2026"). */
    fun formattedDate(): String = SavedDates.shortDate(withYear = true)

    /** Now, as written on a new Polaroid ("6:42 PM"). */
    fun formattedTime(): String = SavedDates.shortTime()

    companion object {
        /** Scene-specific titles keyed by EnvironmentType name */
        val SCENE_TITLES: Map<String, List<String>> = mapOf(
            "MEADOW" to listOf(
                "Wildflower Days", "Petals and You", "Soft Afternoon",
                "Fields We Know", "Bloom Beside Me", "A Flower For You",
                "Gentle Meadow Hours", "Where Buttercups Grow", "Sun-Dappled Grass",
                "Breeze in the Clover", "Fresh Cut Stems", "Picnic in the Sun"
            ),
            "TREE_HILL" to listOf(
                "Shade and Quiet", "Under Our Tree", "Beneath the Branches",
                "Cool Green Afternoon", "The Old Tree Knows", "Roots and Us",
                "Whispers Under Leaves", "Canopy Hours", "Leaning on the Bark",
                "Watching Clouds Drift", "Our Secret Hilltop", "Green Leaves, Soft Heart"
            ),
            "KITCHEN" to listOf(
                "Secret Ingredient", "Warm From the Stove", "Kitchen Thief",
                "Stolen Bites", "Something's Cooking", "Our Little Recipe",
                "Saturday Morning Smells", "Apron Hours", "Taste Testing",
                "Flour on Your Nose", "Warm Cinnamon Air", "Simmering Slowly"
            ),
            "LIVING_ROOM" to listOf(
                "Couch Cuddle", "Slow Evenings", "Quietly Together",
                "Blanket Weather", "Soft Lamplight", "Sofa Stories",
                "Cozy Inside", "Not Going Anywhere", "Warm Feet, Warm Soul",
                "Lazy Living Room", "Reading by Candlelight", "Curled Up Tight"
            ),
            "PATH_NIGHT" to listOf(
                "Under the Same Sky", "Lantern Glow", "Night Stroll",
                "Stars Know Us", "Side by Side", "Moonlit Walk",
                "The Long Way Home", "Late Evening Light", "Pagoda Shadows",
                "Hand in Hand", "Lavender on the Breeze", "Footsteps in the Quiet"
            ),
            "TWILIGHT" to listOf(
                "Just Looking at You", "Golden Hour Pause", "Dusk and Us",
                "Soft Goodbye to the Day", "Pink Sky Moment", "Twilight Quiet",
                "Last Light Together", "Before the Stars", "Amber Horizon",
                "Softest Gazes", "The Sky Painted Pink", "Evening Sigh"
            ),
            "MOMO_STALL" to listOf(
                "Warm Dumplings Night", "Hot Dumplings Happiness", "Our Favorite Spot",
                "Chili and Laughter", "Street Food Love", "Steam and Smiles",
                "The Best Table", "Friday Night Craving", "Extra Spicy Bites",
                "Bamboo Steamer Steam", "Our Corner Stall", "Stealing Dumplings"
            ),
            "EVENING_ROAD" to listOf(
                "Scooter Rides", "Evening Breeze", "Hold On Tight",
                "Wind in Your Hair", "Riding Together", "Cozy Evenings",
                "Streets We Rode", "Fast and Us", "Two Helmets, One Way",
                "Scooter Hum", "Passing City Lights", "Through the Warm City"
            ),
            "COZY_LOFT" to listOf(
                "Midnight Vinyl", "City Glow Cuddle", "Tea and You",
                "Loft Dreaming", "High Above the World", "Blanket and Music",
                "City Lights and Us", "Slow Midnight", "Needle on the Groove",
                "Steam from the Mug", "Rooftop Calm", "Our Little Sanctuary"
            ),
            "CAMPFIRE" to listOf(
                "Starry Campfire", "Roasting Marshmallows", "Crackling Embers",
                "Guitar Under the Stars", "Warm Flannel Nights", "Fireside Whispers",
                "Sparks in the Night", "Camping With You", "Midnight Starlight",
                "Pine Trees and Us", "Golden Warmth", "Sweet Roasts"
            ),
            "RAINY_CAFE" to listOf(
                "Rain on the Window", "Two Cups, One Table", "Latte Hearts",
                "Croissant for Two", "Cafe Corner Us", "Steam and Rain",
                "Fogged Glass Hearts", "Warm Mugs, Cold Rain", "Our Usual Table",
                "Espresso and Smiles", "Puddles Outside", "Stay a Little Longer"
            ),
            "SUNROOM" to listOf(
                "Little Greenhouse", "Growing Together", "Sunlight Through Glass",
                "Morning Mist", "Seedlings and Us", "Terracotta Afternoon",
                "Rain on the Skylight", "Fresh Blooms", "Green and Golden",
                "Where Things Grow", "Leaves and Laughter", "Our Quiet Garden"
            ),
            "SEASIDE_PIER" to listOf(
                "Salt and Sunset", "Feet Over the Water", "Pip Strikes Again",
                "Lighthouse Glow", "Two Cones, One Seagull", "Boardwalk Us",
                "Waves Say Hello", "Message in a Bottle", "Grandpa Bao's Pier",
                "Sea Breeze Hearts", "Golden Hour Tide", "Stay Till the Stars"
            )
        )

        /** Time-of-day specific pools */
        val TIME_TITLES_NIGHT: List<String> = listOf(
            "Under the Moonlight", "Curled Up at Midnight", "Counting Stars",
            "Whispering in the Dark", "While The City Sleeps", "Midnight Blue",
            "Quiet Night In", "Sleeping beside You", "Starlight Over Us",
            "Late Night Softness", "Nightfall Sanctuary", "Dim Lamps and Us"
        )

        val TIME_TITLES_SUNSET: List<String> = listOf(
            "Golden Hour Magic", "Sunset Glow", "When the Day Dims",
            "Warm Amber Skies", "Watching the Day Fade", "Evening Falling",
            "Soft Peach Sky", "Dusk Beside You", "Sun Sinking Low"
        )

        val TIME_TITLES_DAY: List<String> = listOf(
            "Morning Sun on Us", "Gentle Daylight", "Bright and Sweet",
            "Lazy Sunday Morning", "Sunbeams on the Floor", "Clear Skies Ahead",
            "Basking in Daylight", "Small Morning Joy", "A Fresh Little Day"
        )

        /** Universal titles usable in any scene */
        val UNIVERSAL_TITLES: List<String> = listOf(
            "Just Us",
            "A Little Moment",
            "Our Little World",
            "Little Things",
            "Stay A Little Longer",
            "Home With You",
            "A Place for Two",
            "Quietly Yours",
            "Soft and Warm",
            "Right Here",
            "No Rush",
            "Us Again",
            "Two of Us",
            "This Exact Moment",
            "Slow Sunday",
            "Always This",
            "Our Tiny Life",
            "Near You",
            "Gentle Afternoon",
            "Safe and Warm",
            "The Sweetest Thing",
            "While The World Slept",
            "Somewhere Cozy",
            "Another Day With You",
            "Small and Perfect",
            "This Is Enough",
            "A Quiet Kind of Happy",
            "Made For This",
            "Unhurried",
            "Tucked In Together",
            "Still Here",
            "Wherever You Are",
            "Our Corner",
            "Warmly",
            "Not Wanting to Leave",
            "Let's Stay",
            "The Best Part",
            "Always the Best Day",
            "Just the Two of Us",
            "A Memory Already",
            "Treasured Forever",
            "My Favorite View",
            "Everyday Magic",
            "Peaceful Hours",
            "Where I Belong",
            "Pure Comfort",
            "Just Like This",
            "Cherished Hour",
            "Forever Small",
            "With You, Always"
        )
    }
}
