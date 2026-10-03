package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class PreferencesManager(
    private val prefs: SharedPreferences,
    val storage: KeyValueStorage = SharedPreferencesStorage(prefs)
) {
    constructor(context: Context) : this(
        context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE)
    )

    private var cachedMemories: List<MemoryItem>? = null
    private var cachedLoveNotes: List<LoveNoteItem>? = null
    private var cachedDreamEntries: List<DreamEntry>? = null

    var boyfriendName: String
        get() {
            val defaultName = ProfileManager.getProfile().boyName
            return prefs.getString("bf_name", defaultName) ?: defaultName
        }
        set(value) = prefs.edit().putString("bf_name", value.trim().ifEmpty { ProfileManager.getProfile().boyName }).apply()

    var girlfriendName: String
        get() {
            val defaultName = ProfileManager.getProfile().girlName
            return prefs.getString("gf_name", defaultName) ?: defaultName
        }
        set(value) = prefs.edit().putString("gf_name", value.trim().ifEmpty { ProfileManager.getProfile().girlName }).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var atmosphereMode: String
        get() = prefs.getString("atmosphere_mode", "AUTO") ?: "AUTO"
        set(value) = prefs.edit().putString("atmosphere_mode", value).apply()

    var lastSceneId: String
        get() = prefs.getString("last_scene_id", "") ?: ""
        set(value) = prefs.edit().putString("last_scene_id", value).apply()

    var anniversaryDate: String
        get() {
            val defaultDate = ProfileManager.getProfile().anniversaryDate?.toString() ?: "2024-01-01"
            return prefs.getString("anniversary_date", defaultDate) ?: defaultDate
        }
        set(value) = prefs.edit().putString("anniversary_date", value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    var secretCode: String
        get() = prefs.getString("secret_code", "LOVE") ?: "LOVE"
        set(value) = prefs.edit().putString("secret_code", value.trim().uppercase()).apply()

    var secretCodeBody: String
        get() = prefs.getString("secret_code_body", ProfileManager.getProfile().secretCodeBody) ?: ProfileManager.getProfile().secretCodeBody
        set(value) = prefs.edit().putString("secret_code_body", value.trim()).apply()

    var boyfriendBirthday: String
        get() = prefs.getString("bf_birthday", "") ?: ""
        set(value) = prefs.edit().putString("bf_birthday", value.trim()).apply()

    var girlfriendBirthday: String
        get() = prefs.getString("gf_birthday", "") ?: ""
        set(value) = prefs.edit().putString("gf_birthday", value.trim()).apply()

    var firstOpenDate: String
        get() = prefs.getString("first_open_date", "") ?: ""
        set(value) = prefs.edit().putString("first_open_date", value).apply()

    var lastOpenedDate: String
        get() = prefs.getString("last_opened_date", "") ?: ""
        set(value) = prefs.edit().putString("last_opened_date", value).apply()

    var uniqueDaysOpened: Int
        get() = prefs.getInt("unique_days_opened", 1)
        set(value) = prefs.edit().putInt("unique_days_opened", value.coerceAtLeast(1)).apply()

    var debugDayOffset: Int
        get() = prefs.getInt("debug_day_offset", 0)
        set(value) = prefs.edit().putInt("debug_day_offset", value).apply()

    var gardenStage: Int
        get() = prefs.getInt("garden_stage", computeGardenStage(uniqueDaysOpened))
        set(value) = prefs.edit().putInt("garden_stage", value.coerceIn(0, 6)).apply()

    var catName: String
        get() = prefs.getString("cat_name", "Mochi") ?: "Mochi"
        set(value) = prefs.edit().putString("cat_name", value.trim().ifEmpty { "Mochi" }).apply()

    var catPositionState: String
        get() = prefs.getString("cat_pos_state", "MEADOW") ?: "MEADOW"
        set(value) = prefs.edit().putString("cat_pos_state", value).apply()

    var cottageRoomState: String
        get() = prefs.getString("cottage_room_state", "LIVING_ROOM") ?: "LIVING_ROOM"
        set(value) = prefs.edit().putString("cottage_room_state", value).apply()

    var roomThemeId: String
        get() = prefs.getString("room_theme_id", "WARM_AUTUMN_COTTAGE") ?: "WARM_AUTUMN_COTTAGE"
        set(value) = prefs.edit().putString("room_theme_id", value).apply()

    var characterMoodState: String
        get() = prefs.getString("character_mood_state", "COZY") ?: "COZY"
        set(value) = prefs.edit().putString("character_mood_state", value).apply()

    var girlOutfitIndex: Int
        get() = prefs.getInt("girl_outfit_index", 0)
        set(value) = prefs.edit().putInt("girl_outfit_index", value.coerceIn(0, 20)).apply()

    var girlAccessoryIndex: Int
        get() = prefs.getInt("girl_accessory_index", 0)
        set(value) = prefs.edit().putInt("girl_accessory_index", value.coerceIn(0, 10)).apply()

    var boyOutfitIndex: Int
        get() = prefs.getInt("boy_outfit_index", 0)
        set(value) = prefs.edit().putInt("boy_outfit_index", value.coerceIn(0, 20)).apply()

    var boyAccessoryIndex: Int
        get() = prefs.getInt("boy_accessory_index", 0)
        set(value) = prefs.edit().putInt("boy_accessory_index", value.coerceIn(0, 10)).apply()

    var buttonGlassIntensity: Float
        get() = prefs.getFloat("button_glass_intensity", 0.65f)
        set(value) = prefs.edit().putFloat("button_glass_intensity", value.coerceIn(0.10f, 1.0f)).apply()

    var tinyCareEnabled: Boolean
        get() = prefs.getBoolean("tiny_care_enabled", false)
        set(value) = prefs.edit().putBoolean("tiny_care_enabled", value).apply()

    var tinyCareCategories: Set<String>
        get() = prefs.getStringSet("tiny_care_categories", com.example.care.TinyCareCategory.allIds())
            ?: com.example.care.TinyCareCategory.allIds()
        set(value) = prefs.edit().putStringSet("tiny_care_categories", value).apply()

    var tinyCareQuietStartHour: Int
        get() = prefs.getInt("tiny_care_quiet_start", 23)
        set(value) = prefs.edit().putInt("tiny_care_quiet_start", value).apply()

    var tinyCareQuietEndHour: Int
        get() = prefs.getInt("tiny_care_quiet_end", 7)
        set(value) = prefs.edit().putInt("tiny_care_quiet_end", value).apply()

    var tinyCareNextTriggerMillis: Long
        get() = prefs.getLong("tiny_care_next_trigger", 0L)
        set(value) = prefs.edit().putLong("tiny_care_next_trigger", value).apply()

    fun getTinyCareRecentMessageIds(): List<String> {
        val raw = prefs.getString("tiny_care_recent_ids", "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split(",")
    }

    fun addTinyCareRecentMessageId(id: String) {
        val current = getTinyCareRecentMessageIds().toMutableList()
        current.remove(id)
        current.add(0, id)
        val trimmed = current.take(10)
        prefs.edit().putString("tiny_care_recent_ids", trimmed.joinToString(",")).apply()
    }

    fun computeGardenStage(days: Int): Int {
        return when {
            days <= 1 -> 0 // Bare soil with tiny sprout specks
            days == 2 -> 1 // Clover patches & green sprouts
            days in 3..4 -> 2 // Small floral buds appearing
            days in 5..6 -> 3 // Blooming wildflowers
            days in 7..9 -> 4 // Lush flower bushes & butterflies
            days in 10..13 -> 5 // Blossom tree sapling
            else -> 6 // Paradise blooming tree, golden sparkles, ladybugs
        }
    }

    private fun getEffectiveDateString(): String {
        val calendar = Calendar.getInstance()
        if (debugDayOffset != 0) {
            calendar.add(Calendar.DAY_OF_YEAR, debugDayOffset)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    fun markAppOpenedToday(): Boolean {
        val today = getEffectiveDateString()
        if (firstOpenDate.isEmpty()) {
            firstOpenDate = today
        }
        val isFirst = (lastOpenedDate != today)
        if (isFirst) {
            if (lastOpenedDate.isNotEmpty()) {
                uniqueDaysOpened += 1
            }
            lastOpenedDate = today
            gardenStage = computeGardenStage(uniqueDaysOpened)
        }
        return isFirst
    }

    fun fastForwardDayForDebug(): Int {
        debugDayOffset += 1
        markAppOpenedToday()
        return uniqueDaysOpened
    }

    // Recently shown scenes history to prevent repetition
    fun getRecentScenes(): List<String> {
        val raw = prefs.getString("recent_scenes", "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split(",")
    }

    fun addRecentScene(sceneId: String) {
        val current = getRecentScenes().toMutableList()
        current.remove(sceneId)
        current.add(0, sceneId)
        val trimmed = current.take(3)
        prefs.edit().putString("recent_scenes", trimmed.joinToString(",")).apply()
        lastSceneId = sceneId
    }

    fun getDaysTogether(): Long {
        return RelationshipTimeManager.calculateTinyUsDay()
    }

    /**
     * Organic Home Evolution state based on real journey milestones and days together.
     * Gradually introduces home artifacts (plants, photos, decor) without XP or levels.
     */
    fun getHomeEvolutionState(weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY): HomeEvolutionState {
        val days = uniqueDaysOpened
        val memoriesCount = getMemories().size
        val notesCount = getLoveNotes().size
        val completedAdventures = getCompletedAdventuresCount()
        val completedMoments = getCompletedMomentsCount()
        val completedMiniGames = getCompletedMiniGamesCount()
        val hasSignals = getLongDistanceSignals().isNotEmpty()

        val seasonal = when (weather) {
            com.example.scene.WeatherType.SAKURA -> SeasonalArtifact.SPRING_BLOSSOM_VASE
            com.example.scene.WeatherType.AUTUMN -> SeasonalArtifact.AUTUMN_HARVEST_PUMPKIN
            com.example.scene.WeatherType.SNOW -> SeasonalArtifact.WINTER_WARM_COCOA
            else -> SeasonalArtifact.SUMMER_ICED_CARAFE
        }

        return HomeEvolutionState(
            hasWindowsillPlant = days >= 3,
            hasCozyKnitThrow = days >= 7,
            hasCopperTeakettle = days >= 14,
            hasCornerMonstera = days >= 21,
            hasFairyStringLights = days >= 30,
            hasHangingMacrame = days >= 45,
            hasFramedKeepsake = memoriesCount >= 1,
            hasFridgePolaroid = memoriesCount >= 2 || days >= 5,
            hasFridgeLoveNote = notesCount >= 1,
            hasAdventurePicnicBasket = completedAdventures >= 1,
            hasBedsideNotepad = completedMoments >= 1,
            hasMiniGameBoard = completedMiniGames >= 1,
            hasOrigamiHeart = hasSignals,
            seasonalArtifact = seasonal
        )
    }

    // Memories (Offline Keepsakes)
    fun getMemories(): List<MemoryItem> {
        cachedMemories?.let { return it }

        val version = prefs.getInt("memories_version", 1)
        if (version < 2) {
            val defaults = getDefaultMemories()
            val existing = try {
                val raw = prefs.getString("memories_json", null)
                if (raw != null) {
                    val arr = JSONArray(raw)
                    val list = mutableListOf<MemoryItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "")
                        // Retain custom user-added memories
                        if (obj.optBoolean("isCustom", false) || id.length > 5 && !id.startsWith("m_")) {
                            list.add(
                                MemoryItem(
                                    id = id,
                                    title = obj.getString("title"),
                                    date = obj.getString("date"),
                                    note = obj.getString("note"),
                                    iconType = obj.optString("iconType", "heart")
                                )
                            )
                        }
                    }
                    list
                } else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val merged = defaults + existing
            saveMemories(merged)
            prefs.edit().putInt("memories_version", 2).apply()
            cachedMemories = merged
            return merged
        }

        val raw = prefs.getString("memories_json", null)
        if (raw.isNullOrEmpty()) {
            val defaults = getDefaultMemories()
            cachedMemories = defaults
            return defaults
        }
        val loaded = try {
            val arr = JSONArray(raw)
            val list = mutableListOf<MemoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MemoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.getString("title"),
                        date = obj.getString("date"),
                        note = obj.getString("note"),
                        iconType = obj.optString("iconType", "heart")
                    )
                )
            }
            list
        } catch (_: Exception) {
            getDefaultMemories()
        }
        cachedMemories = loaded
        return loaded
    }

    fun addMemory(title: String, note: String, date: String, iconType: String) {
        val current = getMemories().toMutableList()
        val newItem = MemoryItem(
            id = UUID.randomUUID().toString(),
            title = title,
            date = date.ifEmpty { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()) },
            note = note,
            iconType = iconType
        )
        current.add(0, newItem)
        saveMemories(current)
    }

    private fun saveMemories(list: List<MemoryItem>) {
        cachedMemories = list
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("date", item.date)
                put("note", item.note)
                put("iconType", item.iconType)
            }
            arr.put(obj)
        }
        prefs.edit().putString("memories_json", arr.toString()).apply()
    }

    private fun getDefaultMemories(): List<MemoryItem> = ProfileManager.getProfile().defaultMemories

    // Love Notes (Offline Mailbox Letters)
    fun getLoveNotes(): List<LoveNoteItem> {
        cachedLoveNotes?.let { return it }

        val version = prefs.getInt("notes_version", 1)
        if (version < 2) {
            val defaults = getDefaultNotes()
            val existing = try {
                val raw = prefs.getString("notes_json", null)
                if (raw != null) {
                    val arr = JSONArray(raw)
                    val list = mutableListOf<LoveNoteItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "")
                        if (obj.optBoolean("isCustom", false) || id.length > 5 && !id.startsWith("n_")) {
                            list.add(
                                LoveNoteItem(
                                    id = id,
                                    text = obj.getString("text"),
                                    author = obj.getString("author"),
                                    date = obj.getString("date"),
                                    isCustom = true
                                )
                            )
                        }
                    }
                    list
                } else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val merged = defaults + existing
            saveLoveNotes(merged)
            prefs.edit().putInt("notes_version", 2).apply()
            cachedLoveNotes = merged
            return merged
        }

        val raw = prefs.getString("notes_json", null)
        if (raw.isNullOrEmpty()) {
            val defaults = getDefaultNotes()
            cachedLoveNotes = defaults
            return defaults
        }
        val loaded = try {
            val arr = JSONArray(raw)
            val list = mutableListOf<LoveNoteItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    LoveNoteItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        text = obj.getString("text"),
                        author = obj.getString("author"),
                        date = obj.getString("date"),
                        isCustom = obj.optBoolean("isCustom", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            getDefaultNotes()
        }
        cachedLoveNotes = loaded
        return loaded
    }

    fun addLoveNote(text: String, author: String) {
        val current = getLoveNotes().toMutableList()
        val newItem = LoveNoteItem(
            id = UUID.randomUUID().toString(),
            text = text,
            author = author,
            date = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date()),
            isCustom = true
        )
        current.add(0, newItem)
        saveLoveNotes(current)
    }

    private fun saveLoveNotes(list: List<LoveNoteItem>) {
        cachedLoveNotes = list
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("author", item.author)
                put("date", item.date)
                put("isCustom", item.isCustom)
            }
            arr.put(obj)
        }
        prefs.edit().putString("notes_json", arr.toString()).apply()
    }

    private fun getDefaultNotes(): List<LoveNoteItem> = ProfileManager.getProfile().defaultNotes
 
    // All Tiny Moments across the journey
    fun getAllMoments(): List<TinyMoment> = listOf(
        TinyMoment(
            dayIndex = 0,
            title = "A Tiny Blossom",
            description = "He picked a fresh flower just to make you smile.",
            sceneHint = "Scene: Flower",
            quote = "Small gestures speak the softest truths."
        ),
        TinyMoment(
            dayIndex = 1,
            title = "Shade and Whispers",
            description = "Sitting beneath the ancient branches, sharing comfortable silence.",
            sceneHint = "Scene: Under the Tree",
            quote = "The world moves fast, but here we can just breathe."
        ),
        TinyMoment(
            dayIndex = 2,
            title = "Secret Kitchen Thief",
            description = "Someone stole a warm treat from the pot when no one was looking!",
            sceneHint = "Scene: Kitchen Cooking",
            quote = "The sweetest seasoning is playful laughter."
        ),
        TinyMoment(
            dayIndex = 3,
            title = "Sleepy Slumber",
            description = "Dozing off together on the soft cushions as the room dims.",
            sceneHint = "Scene: Couch Sleep",
            quote = "Home is wherever you fall asleep beside me."
        ),
        TinyMoment(
            dayIndex = 4,
            title = "A Stroll Under the Stars",
            description = "Side by side along the quiet lantern path.",
            sceneHint = "Scene: Scenic Walk",
            quote = "Any path is an adventure when holding your hand."
        ),
        TinyMoment(
            dayIndex = 5,
            title = "Just Looking at You",
            description = "Pausing to look into each other's eyes and sharing a gentle head pat.",
            sceneHint = "Scene: Just Looking",
            quote = "Sometimes no words are needed at all."
        ),
        TinyMoment(
            dayIndex = 6,
            title = "Breeze & Evening Ride",
            description = "Riding our scooter through the gentle breezes, holding you tight.",
            sceneHint = "Scene: Evening Ride",
            quote = "Every journey feels like home with your arms around me."
        ),
        TinyMoment(
            dayIndex = 7,
            title = "Street Food Date",
            description = "Sharing steaming dumplings and sweet smiles at our cozy stall.",
            sceneHint = "Scene: Street Food Date",
            quote = "The warmest flavor is laughing together over hot treats."
        ),
        TinyMoment(
            dayIndex = 8,
            title = "Cozy Midnight Loft",
            description = "Wrapped in a warm blanket on the sofa, listening to vinyl records as the glowing city skyline rests outside.",
            sceneHint = "Scene: Midnight Loft",
            quote = "Right here with you is the coziest place in the world."
        )
    )

    // Daily Tiny Moment pool calculated offline based on day of year
    fun getTodayTinyMoment(offset: Int = 0): TinyMoment {
        val moments = getAllMoments()
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR) + offset
        val normalized = ((dayOfYear % moments.size) + moments.size) % moments.size
        return moments[normalized]
    }

    // ── Shared Dream Journal ──────────────────────────────────────────────────

    fun getDreamEntries(): List<DreamEntry> {
        cachedDreamEntries?.let { return it }
        val raw = prefs.getString("dreams_json", null) ?: return emptyList<DreamEntry>().also { cachedDreamEntries = it }
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<DreamEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val kws = obj.optJSONArray("matchedKeywords")
                val kwList = mutableListOf<String>()
                if (kws != null) for (k in 0 until kws.length()) kwList.add(kws.getString(k))
                list.add(
                    DreamEntry(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        matchedKeywords = kwList,
                        dreamTheme = obj.optString("dreamTheme", "FALLBACK"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list.also { cachedDreamEntries = it }
        } catch (e: Exception) {
            emptyList<DreamEntry>().also { cachedDreamEntries = it }
        }
    }

    fun addDreamEntry(entry: DreamEntry) {
        val current = getDreamEntries().toMutableList()
        current.add(0, entry)
        if (current.size > 50) current.subList(50, current.size).clear()
        saveDreamEntries(current)
    }

    fun deleteDreamEntry(id: String) {
        val current = getDreamEntries().toMutableList()
        current.removeAll { it.id == id }
        saveDreamEntries(current)
    }

    private fun saveDreamEntries(list: List<DreamEntry>) {
        cachedDreamEntries = list
        val arr = org.json.JSONArray()
        list.forEach { entry ->
            val obj = org.json.JSONObject().apply {
                put("id", entry.id)
                put("text", entry.text)
                put("dreamTheme", entry.dreamTheme)
                put("timestamp", entry.timestamp)
                val kws = org.json.JSONArray()
                entry.matchedKeywords.forEach { kws.put(it) }
                put("matchedKeywords", kws)
            }
            arr.put(obj)
        }
        prefs.edit().putString("dreams_json", arr.toString()).apply()
    }

    // ── Tiny Date Adventures ──
    private var cachedAdventures: List<DateAdventure>? = null

    fun getDateAdventures(): List<DateAdventure> {
        cachedAdventures?.let { return it }
        val raw = prefs.getString("date_adventures_json", null) ?: return DateAdventureCatalog.defaultAdventures.also { cachedAdventures = it }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<DateAdventure>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DateAdventure(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        category = obj.optString("category", "COZY_HOME"),
                        sceneHint = if (obj.has("sceneHint") && !obj.isNull("sceneHint")) obj.getString("sceneHint").takeIf { it.isNotEmpty() } else null,
                        status = try { AdventureStatus.valueOf(obj.optString("status", "AVAILABLE")) } catch (_: Exception) { AdventureStatus.AVAILABLE },
                        completedByBoy = obj.optBoolean("completedByBoy", false),
                        completedByGirl = obj.optBoolean("completedByGirl", false),
                        completedTimestamp = if (obj.has("completedTimestamp")) obj.getLong("completedTimestamp") else null,
                        unlockedArtifact = if (obj.has("unlockedArtifact") && !obj.isNull("unlockedArtifact")) obj.getString("unlockedArtifact").takeIf { it.isNotEmpty() } else null
                    )
                )
            }
            list.ifEmpty { DateAdventureCatalog.defaultAdventures }.also { cachedAdventures = it }
        } catch (_: Exception) {
            DateAdventureCatalog.defaultAdventures.also { cachedAdventures = it }
        }
    }

    fun saveDateAdventures(list: List<DateAdventure>) {
        cachedAdventures = list
        val arr = JSONArray()
        list.forEach { adv ->
            val obj = JSONObject().apply {
                put("id", adv.id)
                put("title", adv.title)
                put("description", adv.description)
                put("category", adv.category)
                adv.sceneHint?.let { put("sceneHint", it) }
                put("status", adv.status.name)
                put("completedByBoy", adv.completedByBoy)
                put("completedByGirl", adv.completedByGirl)
                adv.completedTimestamp?.let { put("completedTimestamp", it) }
                adv.unlockedArtifact?.let { put("unlockedArtifact", it) }
            }
            arr.put(obj)
        }
        prefs.edit().putString("date_adventures_json", arr.toString()).apply()
    }

    fun updateAdventureStatus(
        id: String,
        status: AdventureStatus,
        completedByBoy: Boolean = false,
        completedByGirl: Boolean = false
    ) {
        val current = getDateAdventures().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = current[index]
            val boyDone = old.completedByBoy || completedByBoy
            val girlDone = old.completedByGirl || completedByGirl
            val finalStatus = if (boyDone && girlDone) AdventureStatus.COMPLETED else status
            val updated = old.copy(
                status = finalStatus,
                completedByBoy = boyDone,
                completedByGirl = girlDone,
                completedTimestamp = if (finalStatus == AdventureStatus.COMPLETED) System.currentTimeMillis() else old.completedTimestamp
            )
            current[index] = updated
            saveDateAdventures(current)

            if (finalStatus == AdventureStatus.COMPLETED) {
                WorldEventBus.post(
                    WorldEvent.DateAdventureCompleted(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        adventureId = updated.id,
                        title = updated.title,
                        completedBy = if (boyDone && girlDone) "both" else if (boyDone) "boy" else "girl"
                    )
                )
            }
        }
    }

    fun getCompletedAdventuresCount(): Int {
        return getDateAdventures().count { it.status == AdventureStatus.COMPLETED || (it.completedByBoy && it.completedByGirl) }
    }

    // ── Daily Tiny Moment ──
    private var cachedMomentResponses: MutableMap<String, DailyMomentResponse>? = null

    fun getDailyMomentResponses(): Map<String, DailyMomentResponse> {
        cachedMomentResponses?.let { return it }
        val raw = prefs.getString("daily_moment_responses_json", null) ?: return emptyMap<String, DailyMomentResponse>().also { cachedMomentResponses = it.toMutableMap() }
        return try {
            val arr = JSONArray(raw)
            val map = mutableMapOf<String, DailyMomentResponse>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val resp = DailyMomentResponse(
                    promptId = obj.getString("promptId"),
                    dateString = obj.getString("dateString"),
                    boyAnswer = if (obj.has("boyAnswer") && !obj.isNull("boyAnswer")) obj.getString("boyAnswer").takeIf { it.isNotEmpty() } else null,
                    girlAnswer = if (obj.has("girlAnswer") && !obj.isNull("girlAnswer")) obj.getString("girlAnswer").takeIf { it.isNotEmpty() } else null,
                    isRevealed = obj.optBoolean("isRevealed", false),
                    completedTimestamp = if (obj.has("completedTimestamp")) obj.getLong("completedTimestamp") else null
                )
                map[resp.dateString] = resp
            }
            map.also { cachedMomentResponses = it }
        } catch (_: Exception) {
            emptyMap<String, DailyMomentResponse>().also { cachedMomentResponses = it.toMutableMap() }
        }
    }

    fun getDailyMomentResponseForDate(dateString: String, promptId: String): DailyMomentResponse {
        val map = getDailyMomentResponses()
        return map[dateString] ?: DailyMomentResponse(promptId = promptId, dateString = dateString)
    }

    fun saveDailyMomentResponse(response: DailyMomentResponse) {
        val map = getDailyMomentResponses().toMutableMap()
        map[response.dateString] = response
        cachedMomentResponses = map

        val arr = JSONArray()
        map.values.forEach { resp ->
            val obj = JSONObject().apply {
                put("promptId", resp.promptId)
                put("dateString", resp.dateString)
                resp.boyAnswer?.let { put("boyAnswer", it) }
                resp.girlAnswer?.let { put("girlAnswer", it) }
                put("isRevealed", resp.isRevealed)
                resp.completedTimestamp?.let { put("completedTimestamp", it) }
            }
            arr.put(obj)
        }
        prefs.edit().putString("daily_moment_responses_json", arr.toString()).apply()

        if (response.isBothAnswered || response.isRevealed) {
            val prompt = DailyPromptCatalog.defaultPrompts.find { it.id == response.promptId }
            WorldEventBus.post(
                WorldEvent.TinyMomentCompleted(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    promptId = response.promptId,
                    promptText = prompt?.question ?: "Daily Tiny Moment",
                    isBothAnswered = response.isBothAnswered
                )
            )
        }
    }

    fun getCompletedMomentsCount(): Int {
        return getDailyMomentResponses().values.count { it.isBothAnswered || it.isRevealed }
    }

    // ── Two-Person Mini-Games ──
    private var cachedMiniGames: List<MiniGameRound>? = null

    fun getMiniGameRounds(): List<MiniGameRound> {
        cachedMiniGames?.let { return it }
        val raw = prefs.getString("mini_games_json", null) ?: return emptyList<MiniGameRound>().also { cachedMiniGames = it }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<MiniGameRound>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optsArr = obj.getJSONArray("options")
                val opts = mutableListOf<String>()
                for (j in 0 until optsArr.length()) opts.add(optsArr.getString(j))

                list.add(
                    MiniGameRound(
                        id = obj.getString("id"),
                        questionId = obj.getString("questionId"),
                        type = try { MiniGameType.valueOf(obj.optString("type", "WOULD_YOU_RATHER")) } catch (_: Exception) { MiniGameType.WOULD_YOU_RATHER },
                        prompt = obj.getString("prompt"),
                        options = opts,
                        boyChosenIndex = if (obj.has("boyChosenIndex") && !obj.isNull("boyChosenIndex")) obj.getInt("boyChosenIndex") else null,
                        girlChosenIndex = if (obj.has("girlChosenIndex") && !obj.isNull("girlChosenIndex")) obj.getInt("girlChosenIndex") else null,
                        isRevealed = obj.optBoolean("isRevealed", false),
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
            list.also { cachedMiniGames = it }
        } catch (_: Exception) {
            emptyList<MiniGameRound>().also { cachedMiniGames = it }
        }
    }

    fun saveMiniGameRound(round: MiniGameRound) {
        val current = getMiniGameRounds().toMutableList()
        val index = current.indexOfFirst { it.id == round.id }
        if (index != -1) {
            current[index] = round
        } else {
            current.add(0, round)
        }
        if (current.size > 50) current.subList(50, current.size).clear()
        cachedMiniGames = current

        val arr = JSONArray()
        current.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("questionId", r.questionId)
                put("type", r.type.name)
                put("prompt", r.prompt)
                val opts = JSONArray()
                r.options.forEach { opts.put(it) }
                put("options", opts)
                r.boyChosenIndex?.let { put("boyChosenIndex", it) }
                r.girlChosenIndex?.let { put("girlChosenIndex", it) }
                put("isRevealed", r.isRevealed)
                put("timestamp", r.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("mini_games_json", arr.toString()).apply()

        if (round.isBothAnswered || round.isRevealed) {
            WorldEventBus.post(
                WorldEvent.MiniGameCompleted(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    gameId = round.id,
                    gameTypeName = round.type.title,
                    summary = if (round.isMatch) "Sweet Match!" else "Shared Perspective"
                )
            )
        }
    }

    fun getCompletedMiniGamesCount(): Int {
        return getMiniGameRounds().count { it.isBothAnswered || it.isRevealed }
    }

    // ── Shared Mood ──
    fun getPartnerMoodState(): PartnerMoodState {
        val boyMoodId = prefs.getString("boy_mood", "good") ?: "good"
        val boyShared = prefs.getBoolean("boy_mood_shared", true)
        val girlMoodId = prefs.getString("girl_mood", "good") ?: "good"
        val girlShared = prefs.getBoolean("girl_mood_shared", true)
        val lastUpdated = prefs.getLong("mood_last_updated", 0L)
        return PartnerMoodState(
            boyMood = SharedMoodType.fromId(boyMoodId),
            boyMoodShared = boyShared,
            girlMood = SharedMoodType.fromId(girlMoodId),
            girlMoodShared = girlShared,
            lastUpdated = lastUpdated
        )
    }

    fun setPartnerMood(partner: String, mood: SharedMoodType, isShared: Boolean) {
        val editor = prefs.edit()
        val now = System.currentTimeMillis()
        if (partner.equals("boy", ignoreCase = true)) {
            editor.putString("boy_mood", mood.id)
            editor.putBoolean("boy_mood_shared", isShared)
        } else {
            editor.putString("girl_mood", mood.id)
            editor.putBoolean("girl_mood_shared", isShared)
        }
        editor.putLong("mood_last_updated", now)
        editor.apply()

        WorldEventBus.post(
            WorldEvent.SharedMoodChanged(
                id = UUID.randomUUID().toString(),
                timestamp = now,
                partner = partner,
                moodName = mood.displayName,
                isShared = isShared
            )
        )
    }

    // ── Long-Distance Mode ──
    private var cachedSignals: List<LongDistanceSignal>? = null

    fun getLongDistanceSignals(): List<LongDistanceSignal> {
        cachedSignals?.let { return it }
        val raw = prefs.getString("ld_signals_json", null) ?: return emptyList<LongDistanceSignal>().also { cachedSignals = it }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<LongDistanceSignal>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    LongDistanceSignal(
                        id = obj.getString("id"),
                        sender = obj.getString("sender"),
                        type = LongDistanceSignalType.fromId(obj.getString("type")),
                        note = if (obj.has("note") && !obj.isNull("note")) obj.getString("note").takeIf { it.isNotEmpty() } else null,
                        timestamp = obj.getLong("timestamp"),
                        isViewed = obj.optBoolean("isViewed", false)
                    )
                )
            }
            list.also { cachedSignals = it }
        } catch (_: Exception) {
            emptyList<LongDistanceSignal>().also { cachedSignals = it }
        }
    }

    fun sendLongDistanceSignal(signal: LongDistanceSignal) {
        val current = getLongDistanceSignals().toMutableList()
        current.add(0, signal)
        if (current.size > 50) current.subList(50, current.size).clear()
        saveLongDistanceSignals(current)

        WorldEventBus.post(
            WorldEvent.PartnerSignalReceived(
                id = signal.id,
                timestamp = signal.timestamp,
                sender = signal.sender,
                signalTypeName = signal.type.title,
                note = signal.note
            )
        )
    }

    fun markSignalViewed(id: String) {
        val current = getLongDistanceSignals().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isViewed = true)
            saveLongDistanceSignals(current)
        }
    }

    private fun saveLongDistanceSignals(list: List<LongDistanceSignal>) {
        cachedSignals = list
        val arr = JSONArray()
        list.forEach { sig ->
            val obj = JSONObject().apply {
                put("id", sig.id)
                put("sender", sig.sender)
                put("type", sig.type.id)
                sig.note?.let { put("note", it) }
                put("timestamp", sig.timestamp)
                put("isViewed", sig.isViewed)
            }
            arr.put(obj)
        }
        prefs.edit().putString("ld_signals_json", arr.toString()).apply()
    }

    // ── Widget Data Payload ──
    fun getWidgetData(currentWeather: String = "Sunny", timePhase: String = "Day", sceneName: String = "Living Room"): TinyUsWidgetData {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dayIndex = RelationshipTimeManager.calculateTinyUsDay().toInt()
        val prompt = DailyPromptCatalog.getPromptForDay(dayIndex)
        val momentResp = getDailyMomentResponseForDate(todayStr, prompt.id)
        val mood = getPartnerMoodState()
        val latestSig = getLongDistanceSignals().firstOrNull()

        return TinyUsWidgetData(
            coupleNames = "$boyfriendName & $girlfriendName",
            daysTogether = RelationshipTimeManager.calculateTinyUsDay(),
            sceneName = sceneName,
            weatherName = currentWeather,
            timePhase = timePhase,
            dailyMomentPrompt = prompt.question,
            dailyMomentAnswered = momentResp.isBothAnswered || momentResp.isRevealed,
            latestSignalText = latestSig?.let { "${it.type.emoji} ${it.type.title}" },
            sharedMoodEmoji = mood.boyMood.emoji,
            sharedMoodText = mood.boyMood.displayName,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }
}
