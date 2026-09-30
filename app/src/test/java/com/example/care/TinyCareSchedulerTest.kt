package com.example.care

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TinyCareSchedulerTest {

    @Test
    fun testNoEmojiInAnyMessageText() {
        // Strict verification: zero emoji in any title or body
        // Emoji characters are in surrogate pairs \uD83C-\uDBFF followed by \uDC00-\uDFFF or misc symbols \u2600-\u27BF
        val emojiRegex = Regex("[\\uD83C-\\uDBFF][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]")
        for (msg in TinyCareMessagePool.allMessages) {
            assertFalse("Message title contains emoji: ${msg.title}", emojiRegex.containsMatchIn(msg.title))
            assertFalse("Message body contains emoji: ${msg.body}", emojiRegex.containsMatchIn(msg.body))
            // Also assert none of the characters are surrogates
            for (c in msg.title) assertFalse(c.isSurrogate())
            for (c in msg.body) assertFalse(c.isSurrogate())
        }
        assertFalse(emojiRegex.containsMatchIn(TinyCareMessagePool.testMessage.title))
        assertFalse(emojiRegex.containsMatchIn(TinyCareMessagePool.testMessage.body))
        for (c in TinyCareMessagePool.testMessage.title) assertFalse(c.isSurrogate())
        for (c in TinyCareMessagePool.testMessage.body) assertFalse(c.isSurrogate())
    }

    @Test
    fun testQuietHoursDetection() {
        // Standard window: 23 (11 PM) to 7 (7 AM)
        assertTrue(TinyCareScheduler.isTimeInQuietWindow(23, 23, 7))
        assertTrue(TinyCareScheduler.isTimeInQuietWindow(0, 23, 7))
        assertTrue(TinyCareScheduler.isTimeInQuietWindow(3, 23, 7))
        assertTrue(TinyCareScheduler.isTimeInQuietWindow(6, 23, 7))
        assertFalse(TinyCareScheduler.isTimeInQuietWindow(7, 23, 7))
        assertFalse(TinyCareScheduler.isTimeInQuietWindow(12, 23, 7))
        assertFalse(TinyCareScheduler.isTimeInQuietWindow(22, 23, 7))
    }

    @Test
    fun testSchedulerPushesToMorningWhenLandingInQuietHoursOrNearBoundary() {
        // Simulate evening at 23:00 (11:00 PM) which is inside quiet hours
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerTime = TinyCareScheduler.computeNextTriggerTime(
            quietStartHour = 23,
            quietEndHour = 7,
            currentTimeMillis = cal.timeInMillis
        )
        val resultCal = Calendar.getInstance().apply { timeInMillis = triggerTime }

        // Must be pushed to tomorrow morning between 7:15 and 7:45 AM
        assertTrue(resultCal.get(Calendar.HOUR_OF_DAY) in 7..8)
        val totalMinutesPastSeven = (resultCal.get(Calendar.HOUR_OF_DAY) - 7) * 60 + resultCal.get(Calendar.MINUTE)
        assertTrue("Expected 15..45 min past 7 AM, was $totalMinutesPastSeven", totalMinutesPastSeven in 15..45)
    }

    @Test
    fun testAntiRepeatPickerExcludesRecentIds() {
        val enabledCategories = setOf("hydration")
        val hydrationMessages = TinyCareMessagePool.allMessages.filter { it.category == TinyCareCategory.HYDRATION }

        // Put all hydration messages except the last one into recentIds
        val recentIds = hydrationMessages.dropLast(1).map { it.id }
        val expected = hydrationMessages.last()

        val picked = TinyCareMessagePool.pickMessage(enabledCategories, recentIds)
        assertNotNull(picked)
        assertEquals(expected.id, picked?.id)
    }

    @Test
    fun testRecentIdsTrimmingToStrictlyTen() {
        var recent = emptyList<String>()
        // Simulate adding 15 unique message IDs
        for (i in 1..15) {
            val id = "msg_$i"
            val mutable = recent.toMutableList()
            mutable.remove(id)
            mutable.add(0, id)
            recent = mutable.take(10)
        }

        assertEquals(10, recent.size)
        assertEquals("msg_15", recent.first())
        assertEquals("msg_6", recent.last())
        assertFalse(recent.contains("msg_5"))
        assertFalse(recent.contains("msg_1"))
    }

    @Test
    fun testCategoryFiltering() {
        val onlyHydration = setOf(TinyCareCategory.HYDRATION.id)
        for (i in 0 until 20) {
            val picked = TinyCareMessagePool.pickMessage(onlyHydration, emptyList())
            assertNotNull(picked)
            assertEquals(TinyCareCategory.HYDRATION, picked?.category)
        }
    }
}
