package com.example.data

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * The couple's own dates, for shared code (the scene engine, special days). On Android the
 * RelationshipTimeManager and SpecialCalendarManager keep these up to date whenever they change;
 * elsewhere they start from the saved profile.
 */
object CoupleDates {
    /** The day they got together. */
    var anniversary: LocalDate = ProfileManager.getProfile().anniversaryDate ?: today()

    var boyBirthday: LocalDate? = null
    var girlBirthday: LocalDate? = null

    fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
}
