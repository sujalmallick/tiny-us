package com.example.data

import kotlinx.datetime.LocalDate

/**
 * Apple iOS implementation of ProfileManager singleton.
 */
actual object ProfileManager : ProfileRepository {
    private val storage = IosUserDefaultsStorage.defaultStorage()
    private var activeProfile: PersonalProfile = loadProfile()

    actual override fun getProfile(): PersonalProfile {
        val persisted = loadProfile()
        activeProfile = activeProfile.copy(
            boyName = persisted.boyName,
            girlName = persisted.girlName,
            anniversaryDate = persisted.anniversaryDate
        )
        return activeProfile
    }

    actual override fun setProfile(profile: PersonalProfile) {
        activeProfile = profile
        storage.putString("bf_name", profile.boyName)
        storage.putString("gf_name", profile.girlName)
        profile.anniversaryDate?.let { storage.putString("anniversary_date", it.toString()) }
            ?: storage.remove("anniversary_date")
    }

    private fun loadProfile(): PersonalProfile {
        val defaults = PersonalProfile()
        val anniversary = storage.getString("anniversary_date", null)
            ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
        return defaults.copy(
            boyName = storage.getString("bf_name", defaults.boyName) ?: defaults.boyName,
            girlName = storage.getString("gf_name", defaults.girlName) ?: defaults.girlName,
            anniversaryDate = anniversary
        )
    }
}
