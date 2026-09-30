package com.example.care

data class TinyCareMessage(
    val id: String,
    val category: TinyCareCategory,
    val title: String,
    val body: String
)

object TinyCareMessagePool {

    val allMessages: List<TinyCareMessage> = listOf(
        // Hydration
        TinyCareMessage(
            id = "hyd_01",
            category = TinyCareCategory.HYDRATION,
            title = "Time for a sip",
            body = "A cool glass of water will make you feel refreshed."
        ),
        TinyCareMessage(
            id = "hyd_02",
            category = TinyCareCategory.HYDRATION,
            title = "Water check",
            body = "Take a moment to pause and drink some water."
        ),
        TinyCareMessage(
            id = "hyd_03",
            category = TinyCareCategory.HYDRATION,
            title = "Stay refreshed",
            body = "Your body will thank you for a fresh cup of water right now."
        ),
        TinyCareMessage(
            id = "hyd_04",
            category = TinyCareCategory.HYDRATION,
            title = "Gentle sip",
            body = "Here is a gentle reminder to have a sip of water."
        ),
        TinyCareMessage(
            id = "hyd_05",
            category = TinyCareCategory.HYDRATION,
            title = "Refreshing pause",
            body = "Grab your water bottle and take a slow, refreshing drink."
        ),
        TinyCareMessage(
            id = "hyd_06",
            category = TinyCareCategory.HYDRATION,
            title = "Hydration moment",
            body = "A few sips of water can brighten the rest of your day."
        ),
        TinyCareMessage(
            id = "hyd_07",
            category = TinyCareCategory.HYDRATION,
            title = "Glass of water",
            body = "Pause whatever you are doing and fill up your glass."
        ),

        // Breaks
        TinyCareMessage(
            id = "brk_01",
            category = TinyCareCategory.BREAKS,
            title = "Gentle pause",
            body = "Take a deep breath and give your mind a quiet minute."
        ),
        TinyCareMessage(
            id = "brk_02",
            category = TinyCareCategory.BREAKS,
            title = "Rest your eyes",
            body = "Look away from the screen and let your eyes soften for a moment."
        ),
        TinyCareMessage(
            id = "brk_03",
            category = TinyCareCategory.BREAKS,
            title = "Small breather",
            body = "You have been doing so well. Pause and unclench your jaw."
        ),
        TinyCareMessage(
            id = "brk_04",
            category = TinyCareCategory.BREAKS,
            title = "Quiet breath",
            body = "Close your eyes for three slow, peaceful breaths."
        ),
        TinyCareMessage(
            id = "brk_05",
            category = TinyCareCategory.BREAKS,
            title = "Step back",
            body = "Step away for a minute and enjoy a quiet, calm moment."
        ),
        TinyCareMessage(
            id = "brk_06",
            category = TinyCareCategory.BREAKS,
            title = "Midday rest",
            body = "Give your thoughts a brief vacation. You deserve a pause."
        ),
        TinyCareMessage(
            id = "brk_07",
            category = TinyCareCategory.BREAKS,
            title = "Window gaze",
            body = "Look out a window into the distance to let your mind wander."
        ),

        // Nourishment / Food
        TinyCareMessage(
            id = "food_01",
            category = TinyCareCategory.FOOD,
            title = "Warm snack",
            body = "Have you had something nourishing to eat lately?"
        ),
        TinyCareMessage(
            id = "food_02",
            category = TinyCareCategory.FOOD,
            title = "Nourish yourself",
            body = "A warm bite or a little snack might be just what you need."
        ),
        TinyCareMessage(
            id = "food_03",
            category = TinyCareCategory.FOOD,
            title = "Fuel your day",
            body = "Do not forget to eat something delicious and wholesome today."
        ),
        TinyCareMessage(
            id = "food_04",
            category = TinyCareCategory.FOOD,
            title = "Treat yourself",
            body = "Take time to enjoy a quiet meal or a healthy treat."
        ),
        TinyCareMessage(
            id = "food_05",
            category = TinyCareCategory.FOOD,
            title = "Afternoon fuel",
            body = "Listen to your body and grab a light, healthy snack."
        ),
        TinyCareMessage(
            id = "food_06",
            category = TinyCareCategory.FOOD,
            title = "Meal reminder",
            body = "Pause your task for a bit and savor something good to eat."
        ),

        // Movement / Posture / Stretch
        TinyCareMessage(
            id = "mov_01",
            category = TinyCareCategory.MOVEMENT,
            title = "Posture check",
            body = "Gently roll your shoulders back and relax your neck."
        ),
        TinyCareMessage(
            id = "mov_02",
            category = TinyCareCategory.MOVEMENT,
            title = "Gentle stretch",
            body = "Reach your arms up high and let the tension melt away."
        ),
        TinyCareMessage(
            id = "mov_03",
            category = TinyCareCategory.MOVEMENT,
            title = "Shoulder roll",
            body = "Drop your shoulders down from your ears and loosen up."
        ),
        TinyCareMessage(
            id = "mov_04",
            category = TinyCareCategory.MOVEMENT,
            title = "Shake it out",
            body = "Stand up, stretch your legs, and take a few easy steps."
        ),
        TinyCareMessage(
            id = "mov_05",
            category = TinyCareCategory.MOVEMENT,
            title = "Ease the tension",
            body = "Tilt your head gently from side to side to relax your neck."
        ),
        TinyCareMessage(
            id = "mov_06",
            category = TinyCareCategory.MOVEMENT,
            title = "Quick stretch",
            body = "A tiny stretch right now will help you feel much lighter."
        ),
        TinyCareMessage(
            id = "mov_07",
            category = TinyCareCategory.MOVEMENT,
            title = "Wrist and hands",
            body = "Gently shake out your wrists and stretch your fingers wide."
        ),

        // Sleep
        TinyCareMessage(
            id = "slp_01",
            category = TinyCareCategory.SLEEP,
            title = "Winding down",
            body = "The night is getting late. Dim the lights and get comfortable."
        ),
        TinyCareMessage(
            id = "slp_02",
            category = TinyCareCategory.SLEEP,
            title = "Soft rest",
            body = "Put away your screen soon and let sleep find you peacefully."
        ),
        TinyCareMessage(
            id = "slp_03",
            category = TinyCareCategory.SLEEP,
            title = "Time for dreams",
            body = "Your pillow is waiting. Rest your weary thoughts tonight."
        ),
        TinyCareMessage(
            id = "slp_04",
            category = TinyCareCategory.SLEEP,
            title = "Quiet evening",
            body = "The stars are out. It is a good time to get ready for sleep."
        ),
        TinyCareMessage(
            id = "slp_05",
            category = TinyCareCategory.SLEEP,
            title = "Peaceful night",
            body = "Wrap up in something warm and drift into a restful slumber."
        ),
        TinyCareMessage(
            id = "slp_06",
            category = TinyCareCategory.SLEEP,
            title = "Gentle bedtime",
            body = "You did enough today. Let tonight bring deep, restful sleep."
        ),

        // General Care
        TinyCareMessage(
            id = "gen_01",
            category = TinyCareCategory.GENERAL,
            title = "Kind thought",
            body = "You are doing your best, and that is more than enough."
        ),
        TinyCareMessage(
            id = "gen_02",
            category = TinyCareCategory.GENERAL,
            title = "Soft check-in",
            body = "Just a quiet reminder that you are cared for and appreciated."
        ),
        TinyCareMessage(
            id = "gen_03",
            category = TinyCareCategory.GENERAL,
            title = "Warm feeling",
            body = "Take things one step at a time. There is no rush."
        ),
        TinyCareMessage(
            id = "gen_04",
            category = TinyCareCategory.GENERAL,
            title = "Little smile",
            body = "Sending a little warmth to keep you company right now."
        ),
        TinyCareMessage(
            id = "gen_05",
            category = TinyCareCategory.GENERAL,
            title = "Comfort moment",
            body = "May your day feel light, peaceful, and gentle."
        ),
        TinyCareMessage(
            id = "gen_06",
            category = TinyCareCategory.GENERAL,
            title = "Quiet friend",
            body = "Checking in quietly to make sure you are feeling comfortable."
        )
    )

    val testMessage = TinyCareMessage(
        id = "test_01",
        category = TinyCareCategory.GENERAL,
        title = "Tiny Care",
        body = "This is a preview reminder. Tiny Care is watching over you quietly."
    )

    fun pickMessage(
        enabledCategoryIds: Set<String>,
        recentIds: List<String>
    ): TinyCareMessage? {
        val eligible = allMessages.filter { it.category.id in enabledCategoryIds }
        if (eligible.isEmpty()) return null

        val unrecentlyDelivered = eligible.filter { it.id !in recentIds }
        return if (unrecentlyDelivered.isNotEmpty()) {
            unrecentlyDelivered.random()
        } else {
            // When all messages in selected categories have been seen in the recent window,
            // pick the one least recently seen (farthest back in the recentIds list)
            eligible.maxByOrNull { recentIds.indexOf(it.id) } ?: eligible.random()
        }
    }
}
