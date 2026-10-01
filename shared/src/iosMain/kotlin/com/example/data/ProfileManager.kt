package com.example.data

/**
 * Apple iOS implementation of ProfileManager singleton.
 */
actual object ProfileManager : ProfileRepository {
    private var activeProfile: PersonalProfile = PersonalProfile()

    actual override fun getProfile(): PersonalProfile = activeProfile

    actual override fun setProfile(profile: PersonalProfile) {
        activeProfile = profile
    }
}
