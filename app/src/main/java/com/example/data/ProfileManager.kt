package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Data layer for couple personalization.
 *
 * In public/template builds, this provides warm, generic couple defaults.
 * For personal builds, real names, relationship dates, custom letters, and memories
 * can be loaded locally from a non-committed JSON file ("personal_profile.json")
 * or configured through the onboarding / settings flow.
 */
data class PersonalProfile(
    val boyName: String = "Him",
    val girlName: String = "Her",
    val anniversaryDate: LocalDate? = null,
    val secretLetter: String = "I built this little digital home so we can always share cozy moments together, no matter where we are. Every single pixel, every melody, and every little secret was crafted with all my love, just for you.",
    val secretCodeTitle: String = "A Secret Note",
    val secretCodeSubtitle: String = "A keepsake from the heart",
    val secretCodeBody: String = "Rich not in paper money or gold, but in endless love, devotion, and warm cuddles!",
    val boyTapWhispers: List<String> = listOf(
        "Forever by your side.",
        "You make me smile every day.",
        "So lucky to have you in my life.",
        "Always thinking of you."
    ),
    val girlTapWhispers: List<String> = listOf(
        "My whole heart.",
        "Warmest cuddles only with you.",
        "Love you forever and always."
    ),
    val defaultMemories: List<MemoryItem> = listOf(
        MemoryItem("m_stargazing", "Midnight Stargazing", "Quiet Night", "Sitting side-by-side in the quiet dark, talking about everything and nothing at all under a peaceful sky full of glowing stars.", "stars"),
        MemoryItem("m_under_tree", "Under Our Tree", "Sunny Afternoon", "Resting gently in the cool shade, watching the leaves flutter in the breeze as time stood completely still.", "tree"),
        MemoryItem("m_cooking", "Kitchen Treats", "Cozy Morning", "Cooking together and stealing little warm bites from the stove when no one was looking.", "cooking"),
        MemoryItem("m_rainy_day", "Rainy Day Warmth", "Monsoon Magic", "Listening to the soft rhythm of raindrops falling outside while sharing a warm cup of tea, safe and sound together.", "flower"),
        MemoryItem("m_walk", "Lantern Evening Walk", "Evening Stroll", "Holding hands along the quiet path under the warm amber glow of lantern lights.", "couch"),
        MemoryItem("m_sweet_kiss", "Sweet Whispers", "Special Moment", "A magical spark in time that made the whole world stand completely still.", "heart")
    ),
    val defaultNotes: List<LoveNoteItem> = listOf(
        LoveNoteItem("n1", "Good morning! I hope your day is as sweet and wonderful as you are.", "From Me", "Today"),
        LoveNoteItem("n2", "Just wanted to remind you how much you mean to me.", "From Me", "Yesterday"),
        LoveNoteItem("n3", "You are my favorite person in the whole universe.", "From You", "2d ago"),
        LoveNoteItem("n4", "Even when things get busy, my heart is always holding yours tight.", "From Me", "3d ago"),
        LoveNoteItem("n5", "Can't wait to curl up and cuddle under the warm blanket with you tonight.", "From You", "4d ago"),
        LoveNoteItem("n6", "Thank you for bringing so much warmth and light into my life.", "From Me", "5d ago")
    ),
    val stallSignboardText: String = "WARM BITES",
    val milestoneText: String = "TINY US 0 KM"
)

object ProfileManager {
    private var activeProfile: PersonalProfile = PersonalProfile()

    fun getProfile(): PersonalProfile = activeProfile

    fun setProfile(profile: PersonalProfile) {
        activeProfile = profile
    }

    /**
     * Attempts to load a local non-committed profile JSON from external/internal storage
     * or custom configuration path if present.
     */
    fun loadFromLocalFile(context: Context): Boolean {
        return runCatching {
            val candidateFiles = listOf(
                File(context.filesDir, "personal_profile.json"),
                File(context.getExternalFilesDir(null), "personal_profile.json")
            )
            val target = candidateFiles.firstOrNull { it.exists() && it.length() > 0 } ?: return false
            val jsonStr = target.readText()
            val obj = JSONObject(jsonStr)

            // Safeguard: explicit profile flag prevents accidental auto-load from generic backups
            if (!obj.optBoolean("isPersonalProfile", false) && obj.optInt("schemaVersion", 0) != 1) {
                return false
            }

            val parsedProfile = parseJsonToProfile(obj)
            activeProfile = parsedProfile
            true
        }.getOrDefault(false)
    }

    fun parseJsonToProfile(obj: JSONObject): PersonalProfile {
        val bName = obj.optString("boyName", "Him")
        val gName = obj.optString("girlName", "Her")
        val annDateStr = obj.optString("anniversaryDate", "")
        val annDate = if (annDateStr.isNotBlank()) {
            runCatching { LocalDate.parse(annDateStr) }.getOrNull()
        } else null

        val secretLetter = obj.optString("secretLetter", activeProfile.secretLetter)
        val secretTitle = obj.optString("secretCodeTitle", activeProfile.secretCodeTitle)
        val secretSubtitle = obj.optString("secretCodeSubtitle", activeProfile.secretCodeSubtitle)
        val secretBody = obj.optString("secretCodeBody", activeProfile.secretCodeBody)

        val boyWhispers = mutableListOf<String>()
        obj.optJSONArray("boyTapWhispers")?.let { arr ->
            for (i in 0 until arr.length()) boyWhispers.add(arr.getString(i))
        }

        val girlWhispers = mutableListOf<String>()
        obj.optJSONArray("girlTapWhispers")?.let { arr ->
            for (i in 0 until arr.length()) girlWhispers.add(arr.getString(i))
        }

        val memories = mutableListOf<MemoryItem>()
        obj.optJSONArray("memories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                memories.add(
                    MemoryItem(
                        id = m.optString("id", "m_$i"),
                        title = m.optString("title", "Memory"),
                        date = m.optString("date", ""),
                        note = m.optString("note", ""),
                        iconType = m.optString("iconType", "heart")
                    )
                )
            }
        }

        val notes = mutableListOf<LoveNoteItem>()
        obj.optJSONArray("loveNotes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val n = arr.getJSONObject(i)
                notes.add(
                    LoveNoteItem(
                        id = n.optString("id", "n_$i"),
                        text = n.optString("text", ""),
                        author = n.optString("author", "From Me"),
                        date = n.optString("date", "Today"),
                        isCustom = n.optBoolean("isCustom", false)
                    )
                )
            }
        }

        val stallText = obj.optString("stallSignboardText", "WARM BITES")
        val mileText = obj.optString("milestoneText", "TINY US 0 KM")

        return PersonalProfile(
            boyName = bName,
            girlName = gName,
            anniversaryDate = annDate,
            secretLetter = secretLetter,
            secretCodeTitle = secretTitle,
            secretCodeSubtitle = secretSubtitle,
            secretCodeBody = secretBody,
            boyTapWhispers = if (boyWhispers.isNotEmpty()) boyWhispers else activeProfile.boyTapWhispers,
            girlTapWhispers = if (girlWhispers.isNotEmpty()) girlWhispers else activeProfile.girlTapWhispers,
            defaultMemories = if (memories.isNotEmpty()) memories else activeProfile.defaultMemories,
            defaultNotes = if (notes.isNotEmpty()) notes else activeProfile.defaultNotes,
            stallSignboardText = stallText,
            milestoneText = mileText
        )
    }
}
