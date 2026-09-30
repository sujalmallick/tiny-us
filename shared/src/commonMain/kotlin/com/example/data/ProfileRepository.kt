package com.example.data

/**
 * Platform-agnostic contract for retrieving and persisting the personal profile.
 */
interface ProfileRepository {
    fun getProfile(): PersonalProfile
    fun setProfile(profile: PersonalProfile)
}
