package com.example.data

import kotlinx.datetime.LocalDate

/**
 * Data layer for couple personalization.
 * Platform-independent data class in commonMain.
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
