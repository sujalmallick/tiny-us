package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

import kotlinx.datetime.LocalDate as KxLocalDate
import kotlinx.datetime.toJavaLocalDate

val PersonalProfile.anniversaryDateJava: LocalDate?
    get() = anniversaryDate?.toJavaLocalDate()

actual object ProfileManager : ProfileRepository {
    private var activeProfile: PersonalProfile = PersonalProfile()

    actual override fun getProfile(): PersonalProfile = activeProfile

    actual override fun setProfile(profile: PersonalProfile) {
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
        val bName = obj.optString("boyName", PersonalProfile.DEFAULT_NAME_A)
        val gName = obj.optString("girlName", PersonalProfile.DEFAULT_NAME_B)
        val annDateStr = obj.optString("anniversaryDate", "")
        val annDate = if (annDateStr.isNotBlank()) {
            runCatching { KxLocalDate.parse(annDateStr) }.getOrNull()
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
        val mileText = obj.optString("milestoneText", "TINY Us 0 KM")

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
