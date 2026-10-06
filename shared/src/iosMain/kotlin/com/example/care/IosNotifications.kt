package com.example.care

import com.example.data.BirthdayStore
import com.example.data.CoupleLifeStore
import com.example.data.FestivalStore
import com.example.data.Festivals
import com.example.data.PreferencesManager
import com.example.engine.GameText
import com.example.resources.Res
import com.example.resources.discreet_notification_body
import com.example.resources.discreet_notification_title
import com.example.security.IosDiscreetIcon
import com.example.security.IosLock
import com.example.resources.ui_tiny_us
import com.example.ui.Reminders
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.UserNotifications.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Tiny Care and the couple's mornings (birthdays, festivals, the anniversary, the month-iversary)
 * on iOS, through UNUserNotificationCenter. iOS runs no app code in the background, so the
 * reminders are planned ahead ([TinyCarePlan.upcomingThinning]: a few days as usual, then one a
 * day) and planned again each time the app opens or a setting changes.
 */
object IosNotifications {
    private const val CARE_PREFIX = "tinycare."
    // iOS keeps 64 waiting notifications: Tiny Care, the morning reminder, the preview, and room to spare.
    private const val CARE_SLOTS = 58
    private const val PREVIEW_ID = "tinycare.preview"
    private const val BIRTHDAY_ID = "tinyus.birthday"
    private const val PLAN_DAYS = 3
    private const val THIN_DAYS = 21

    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    /** The notification permission as last read, or null before the first read. */
    var status: Long? = null
        private set

    private fun onMain(block: () -> Unit) = dispatch_async(dispatch_get_main_queue(), block)

    /** Reads the notification permission, then runs [then] on the main thread. */
    fun refreshStatus(then: () -> Unit = {}) {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val read = settings?.authorizationStatus
            onMain {
                status = read
                then()
            }
        }
    }

    fun allowed(): Boolean = when (status) {
        null, UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral -> true
        else -> false
    }

    fun needsPermission(): Boolean = status == UNAuthorizationStatusNotDetermined

    fun requestPermission(onResult: (Boolean) -> Unit) {
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            onMain {
                status = if (granted) UNAuthorizationStatusAuthorized else UNAuthorizationStatusDenied
                onResult(granted)
            }
        }
    }

    /** What the current plan was made for: on or off, the categories and the quiet hours. */
    private var plannedFor: String? = null

    private fun planKey(prefs: PreferencesManager) =
        "${prefs.tinyCareEnabled}|${prefs.tinyCareCategories.sorted()}|${prefs.tinyCareQuietStartHour}-${prefs.tinyCareQuietEndHour}|${isPrivate()}"

    /** Plans again only when the reminders' settings changed since the last plan. */
    fun planTinyCareIfChanged(prefs: PreferencesManager) {
        if (planKey(prefs) != plannedFor) planTinyCare(prefs)
    }

    /** Plans Tiny Care for the next few days (or clears it when it's off). [firstSoon]: the quick first check-in. */
    fun planTinyCare(prefs: PreferencesManager, firstSoon: Boolean = false) {
        plannedFor = planKey(prefs)
        center.removePendingNotificationRequestsWithIdentifiers(List(CARE_SLOTS) { "$CARE_PREFIX$it" })
        if (!prefs.tinyCareEnabled || !allowed()) return
        val now = Clock.System.now().toEpochMilliseconds()
        val plan = TinyCarePlan.upcomingThinning(
            fromMillis = now,
            denseDays = PLAN_DAYS,
            sparseDays = THIN_DAYS,
            categoryIds = prefs.tinyCareCategories,
            recentIds = prefs.getTinyCareRecentMessageIds(),
            quietStartHour = prefs.tinyCareQuietStartHour,
            quietEndHour = prefs.tinyCareQuietEndHour,
            firstSoon = firstSoon,
            max = CARE_SLOTS
        )
        plan.forEachIndexed { i, reminder ->
            schedule("$CARE_PREFIX$i", reminder.message.title, reminder.message.body, reminder.atMillis, now)
        }
        // The first one counts as shown, so the next plan starts with fresh messages.
        plan.firstOrNull()?.let { prefs.addTinyCareRecentMessageId(it.message.id) }
    }

    /** One reminder now, to show what they look like. */
    fun sendPreview(prefs: PreferencesManager) {
        val sample = TinyCareMessagePool.pickMessage(prefs.tinyCareCategories, emptyList()) ?: TinyCareMessagePool.testMessage
        val now = Clock.System.now().toEpochMilliseconds()
        schedule(PREVIEW_ID, sample.title, sample.body, now + 1_000, now)
    }

    /**
     * Sets (or clears) the next morning reminder: a birthday, a festival, the anniversary or the
     * month-iversary, whichever is switched on and comes first. Android works out the text on the
     * day; iOS has to write it now, so it is the text for that day.
     */
    fun planMornings(prefs: PreferencesManager) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(BIRTHDAY_ID))
        if (!allowed()) return
        val now = Clock.System.now().toEpochMilliseconds()
        val birthdays = BirthdayStore(prefs.storage)
        val life = CoupleLifeStore(prefs.storage)
        val start = runCatching { LocalDate.parse(prefs.anniversaryDate) }.getOrNull()
        val festivals = FestivalStore(prefs.storage)
        val southern = Festivals.isSouthern(androidx.compose.ui.text.intl.Locale.current.region)
        val at = CoupleMornings.nextMillis(birthdays, life, start, now, festivals = festivals, southern = southern) ?: return
        val day = Instant.fromEpochMilliseconds(at).toLocalDateTime(TimeZone.currentSystemDefault()).date
        val message = CoupleMornings.messageFor(day, birthdays, life, start, festivals, southern) ?: return
        schedule(BIRTHDAY_ID, GameText.get(Res.string.ui_tiny_us), GameText.get(message), at, now)
    }

    /** Both plans, after the permission has been read (the app's start). */
    fun planAll(prefs: PreferencesManager) {
        refreshStatus {
            planTinyCare(prefs)
            planMornings(prefs)
        }
    }

    /**
     * With the app lock or the discreet icon on, reminders only say something neutral. Android
     * hides the text on the lock screen; iOS writes it ahead of time, so it is neutral everywhere.
     */
    private fun isPrivate(): Boolean = IosLock.store.isEnabled || IosDiscreetIcon.isEnabled()

    private fun schedule(id: String, title: String, body: String, atMillis: Long, nowMillis: Long) {
        val seconds = ((atMillis - nowMillis) / 1000.0).coerceAtLeast(1.0)
        val content = UNMutableNotificationContent()
        val neutral = isPrivate()
        content.setTitle(if (neutral) GameText.get(Res.string.discreet_notification_title) else title)
        content.setBody(if (neutral) GameText.get(Res.string.discreet_notification_body) else body)
        content.setSound(UNNotificationSound.defaultSound)
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(id, content, trigger), withCompletionHandler = null)
    }
}

/** The settings sheet's reminders on iOS. */
class IosReminders(private val prefs: PreferencesManager) : Reminders {
    override fun allowed(): Boolean = IosNotifications.allowed()

    override fun needsPermission(): Boolean = IosNotifications.needsPermission()

    override fun requestPermission(onResult: (granted: Boolean) -> Unit) = IosNotifications.requestPermission(onResult)

    override fun enable() {
        prefs.tinyCareEnabled = true
        IosNotifications.planTinyCare(prefs, firstSoon = true)
    }

    override fun disable() {
        prefs.tinyCareEnabled = false
        IosNotifications.planTinyCare(prefs)
    }

    override fun sendPreview() = IosNotifications.sendPreview(prefs)
}
