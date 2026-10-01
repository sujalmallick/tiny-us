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
}
