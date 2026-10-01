package com.example.data

/**
 * Common platform-independent contract for ProfileManager singleton.
 */
expect object ProfileManager : ProfileRepository {
    override fun getProfile(): PersonalProfile
    override fun setProfile(profile: PersonalProfile)
}
