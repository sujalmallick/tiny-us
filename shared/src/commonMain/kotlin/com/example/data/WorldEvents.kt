package com.example.data

/**
 * Centralized World Event abstraction for Tiny Us.
 * All major relationship milestones, date adventure completions, daily moment reflections,
 * mini-game finishes, and partner signals dispatch a [WorldEvent].
 *
 * This allows existing systems (SceneEngine, HomeEvolution, Particles, Audio, Notifications)
 * to react cleanly without tightly coupling disparate systems.
 */
sealed class WorldEvent {
    abstract val id: String
    abstract val timestamp: Long

    data class DateAdventureCompleted(
        override val id: String,
        override val timestamp: Long,
        val adventureId: String,
        val title: String,
        val completedBy: String // "boy", "girl", or "both"
    ) : WorldEvent()

    data class TinyMomentCompleted(
        override val id: String,
        override val timestamp: Long,
        val promptId: String,
        val promptText: String,
        val isBothAnswered: Boolean
    ) : WorldEvent()

    data class MiniGameCompleted(
        override val id: String,
        override val timestamp: Long,
        val gameId: String,
        val gameTypeName: String,
        val summary: String
    ) : WorldEvent()

    data class SharedMoodChanged(
        override val id: String,
        override val timestamp: Long,
        val partner: String, // "boy" or "girl"
        val moodName: String,
        val isShared: Boolean
    ) : WorldEvent()

    data class PartnerSignalReceived(
        override val id: String,
        override val timestamp: Long,
        val sender: String, // "boy" or "girl"
        val signalTypeName: String,
        val note: String? = null
    ) : WorldEvent()

    data class MemoryCreated(
        override val id: String,
        override val timestamp: Long,
        val memoryId: String,
        val title: String
    ) : WorldEvent()

    data class RelationshipMilestoneReached(
        override val id: String,
        override val timestamp: Long,
        val dayCount: Long,
        val milestoneTitle: String
    ) : WorldEvent()
}

/**
 * Thread-safe, lightweight listener interface for world events.
 */
fun interface WorldEventListener {
    fun onWorldEvent(event: WorldEvent)
}

/**
 * Centralized, singleton event bus for dispatching and observing [WorldEvent]s.
 * Safe for multiplatform use.
 */
object WorldEventBus {
    @kotlin.concurrent.Volatile
    private var listeners = emptyList<WorldEventListener>()

    fun subscribe(listener: WorldEventListener) {
        if (!listeners.contains(listener)) {
            listeners = listeners + listener
        }
    }

    fun unsubscribe(listener: WorldEventListener) {
        listeners = listeners - listener
    }

    fun post(event: WorldEvent) {
        val targets = listeners
        for (target in targets) {
            target.onWorldEvent(event)
        }
    }

    fun clearListeners() {
        listeners = emptyList()
    }
}
