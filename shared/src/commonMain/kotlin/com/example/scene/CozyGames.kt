package com.example.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.CoupleDates
import com.example.engine.CharacterEmotion
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.EmoteType
import com.example.engine.GameText
import com.example.games.CookingGame
import com.example.games.FishingCatch
import com.example.games.FishingConditions
import com.example.games.FishingGame
import com.example.games.GardenPlots
import com.example.games.Ingredient
import com.example.games.PlotOwner
import com.example.games.PlotStage
import com.example.games.Recipe
import com.example.games.Recipes
import com.example.games.Seed
import com.example.games.SeedKind
import com.example.games.Seeds
import com.example.progress.ProgressEvent
import com.example.resources.*
import org.jetbrains.compose.resources.StringResource
import kotlin.random.Random

/**
 * Cooking, fishing and garden care inside the world (plan 07, C3-C5). The rules live in
 * [com.example.games]; this runs them in their scenes, reacts with the couple, sounds and sparkles,
 * and reports what happened through the engine's onProgress. Games only start when the player
 * starts them (the garden is always there, but only grows when it's looked after).
 */
class CozyGames(private val engine: SceneEngine) {

    val cooking = CookingGame()
    val fishing = FishingGame()

    // Compose state for the screen's recipe card and fishing counter.
    var cookingActive by mutableStateOf(false)
        private set
    /** The ingredients on the card: the recipe's, plus a couple of others, shuffled. */
    var cookingChoices by mutableStateOf<List<Ingredient>>(emptyList())
        private set
    /** Bumped on every change to the cooking game, so the card redraws. */
    var cookingTick by mutableIntStateOf(0)
        private set
    var fishingActive by mutableStateOf(false)
        private set
    var fishingPhase by mutableStateOf(FishingGame.Phase.IDLE)
        private set

    /** What the progress knows, kept up to date by the screen. */
    var garden: GardenPlots = GardenPlots()
    var cookedRecipes: Set<String> = emptySet()
    var hasCaughtFish = false
    /** The season (WINTER, SPRING, SUMMER, AUTUMN) and the pantry's produce, also kept up to date by the screen. */
    var season: String = "SPRING"
    var pantry: Map<Ingredient, Int> = emptyMap()

    /** The recipe picker is open (plan 09, E3). */
    var cookingPicking by mutableStateOf(false)
        private set
    /** The garden spot whose seed is being chosen (plan 09, E1), or null. */
    var seedPickerSpot by mutableStateOf<Int?>(null)
        private set
    /** The shared plot asking who is watering (plan 09, E2), or null. */
    var whoWatersSpot by mutableStateOf<Int?>(null)
        private set
    /** Seconds until the dinner-talk question after a meal (plan 09, E4). */
    private var dinnerTalkIn = 0f

    private var steamTimer = 0f
    private var lastRainDay = -1L
    private var tooSoonHold = 0f

    private fun today() = CoupleDates.today().toEpochDays().toLong()
    private fun report(event: ProgressEvent) = engine.onProgress?.invoke(event)

    val canCook: Boolean
        get() = engine.currentScene == SceneType.COOKING && !engine.isDreamMode

    val canFish: Boolean
        get() = engine.currentScene == SceneType.SEASIDE_PIER && !engine.isDreamMode

    // ── Cooking ──

    /** "Cook together": the recipe picker opens (the starters, plus what the pantry allows). */
    fun openRecipePicker() {
        if (!canCook || cookingActive) return
        cookingPicking = true
        engine.audio.playPaperFlip()
    }

    fun closeRecipePicker() {
        cookingPicking = false
    }

    /** Starts a suggested recipe straight away (previews and tests). */
    fun startCooking(random: Random = Random.Default) {
        if (!canCook || cookingActive) return
        chooseRecipe(Recipes.suggest(cookedRecipes, random, pantry), random)
    }

    /** Starts [recipe], if the pantry has what it needs. */
    fun chooseRecipe(recipe: Recipe, random: Random = Random.Default) {
        if (!canCook || cookingActive || !recipe.canCook(pantry)) return
        cookingPicking = false
        val others = Ingredient.entries.filter { it !in recipe.ingredients }.shuffled(random)
        cookingChoices = (recipe.ingredients.distinct() + others.take(6 - recipe.ingredients.distinct().size)).shuffled(random)
        cooking.start(recipe)
        cookingActive = true
        cookingTick++
        engine.girl.emotion = CharacterEmotion.HAPPY
        engine.boy.emotion = CharacterEmotion.HAPPY
        engine.audio.playPaperFlip()
        engine.showMessage(GameText.get(Res.string.cooking_hint), duration = 3.5f)
    }

    fun tapIngredient(ingredient: Ingredient) {
        val w = engine.lastWorldW
        val h = engine.lastWorldH
        when (cooking.tap(ingredient)) {
            CookingGame.TapResult.ADDED -> {
                engine.audio.playBubblePop()
                engine.particles.spawnSparkles(w * POT_X, h * POT_Y, 5)
            }
            CookingGame.TapResult.COOKING -> {
                engine.audio.playCookingBubbles()
                repeat(6) { engine.particles.spawnSteam(w * POT_X, h * POT_Y) }
            }
            CookingGame.TapResult.WRONG -> {
                engine.audio.playSoftThud()
                cooking.next?.let { engine.showMessage(GameText.get(Res.string.cooking_wrong, GameText.get(ingredientName(it))), duration = 2.5f) }
            }
            CookingGame.TapResult.IGNORED -> Unit
        }
        cookingTick++
    }

    fun stopCooking() {
        cooking.stop()
        cookingActive = false
        cookingTick++
    }

    /** The dish on the kitchen table while it's being shown. */
    val servedDish: Recipe?
        get() = if (cooking.phase == CookingGame.Phase.SERVED) cooking.recipe else null

    private fun updateCooking(dt: Float, cw: Float, ch: Float) {
        if (!cookingActive) return
        if (engine.currentScene != SceneType.COOKING) {
            stopCooking()
            return
        }
        val wiggling = cooking.wiggling
        val served = cooking.update(dt)
        if (cooking.phase == CookingGame.Phase.COOKING) {
            steamTimer -= dt
            if (steamTimer <= 0f) {
                engine.particles.spawnSteam(cw * POT_X, ch * POT_Y)
                steamTimer = 0.25f
            }
        }
        if (served != null) {
            engine.audio.playHeartChime()
            engine.particles.spawnHeart(cw * 0.5f, ch * 0.78f)
            engine.particles.spawnSparkles(cw * 0.5f, ch * 0.80f, 10, Color(0xFFFFD166))
            cheer()
            engine.showMessage(GameText.get(Res.string.cooking_served, GameText.get(recipeName(served.id))), duration = 4f)
            cookedRecipes = cookedRecipes + served.id
            // What it took from the pantry is used up (the progress does the same).
            for (i in served.pantry) pantry = pantry + (i to ((pantry[i] ?: 1) - 1).coerceAtLeast(0))
            report(ProgressEvent.DishCooked(served.id))
            sitDownToEat()
            cookingTick++
        }
        if (cooking.phase == CookingGame.Phase.IDLE) {
            cookingActive = false
            cookingTick++
        } else if (wiggling != cooking.wiggling) {
            cookingTick++
        }
    }

    /** Sharing the meal (plan 09, E4): they sit together at the table, then talk over dinner. */
    private fun sitDownToEat() {
        val boy = engine.boy
        val girl = engine.girl
        // They walk over and sit down when they get there, facing each other.
        boy.moveTo(TABLE_BOY_X, arrivePose = CharacterPose.SIT)
        girl.moveTo(TABLE_GIRL_X, arrivePose = CharacterPose.SIT)
        boy.direction = Direction.RIGHT
        girl.direction = Direction.LEFT
        boy.reactionTimer = MEAL_SECONDS
        girl.reactionTimer = MEAL_SECONDS
        dinnerTalkIn = DINNER_TALK_AFTER
    }

    private fun updateDinnerTalk(dt: Float) {
        if (dinnerTalkIn <= 0f) return
        dinnerTalkIn -= dt
        if (dinnerTalkIn > 0f) return
        if (engine.currentScene != SceneType.COOKING) return
        val prompt = com.example.data.DailyPromptCatalog.getPromptForDay(Random.nextInt(0, 10_000))
        engine.showMessage(GameText.get(Res.string.dinner_talk, prompt.question), duration = 8f)
    }

    // ── Fishing ──

    /**
     * A message in a bottle that has washed up and waits on the line (plan 09, F), kept up to
     * date by the screen from the sealed letters. The next bite brings it.
     */
    var dueBottle: com.example.data.SealedLetter? = null
    /** The bottle just reeled in, for the screen to open, or null. */
    var bottleLanded by mutableStateOf<com.example.data.SealedLetter?>(null)
        private set
    /** The bottle-note writer is open. */
    var writingBottle by mutableStateOf(false)
        private set

    fun openBottleWriter() {
        if (canFish && !fishingActive) writingBottle = true
    }

    fun closeBottleWriter() {
        writingBottle = false
    }

    /** The note is sealed: the bottle bobs off over the waves, to wash up another day. */
    fun tossBottle(forName: String) {
        writingBottle = false
        val w = engine.lastWorldW
        val h = engine.lastWorldH
        engine.audio.playWaterDrip()
        engine.particles.spawnSparkles(w * 0.30f, h * 0.52f, 10, Color(0xFFBDE0FE))
        engine.particles.spawnHeart(w * 0.30f, h * 0.50f, Color(0xFFFF85A1))
        engine.showMessage(GameText.get(Res.string.bottle_tossed, forName), duration = 4f)
    }

    /** The bottle has been read (or put away): it won't come up again. */
    fun bottleRead() {
        bottleLanded = null
    }

    /** Casts the line, bringing a waiting bottle on the next bite. */
    private fun castLine(practice: Boolean = false) {
        fishing.cast(firstEver = !hasCaughtFish, conditions = fishingConditions, practice = practice)
        if (!practice && dueBottle != null) fishing.forceNext(FishingCatch.BOTTLE)
    }

    /** Whether they've ever fished: the first time, Grandpa Bao teaches first (plan 09, F2). */
    var hasFished = false

    /** Seconds into Bao's lesson, or a negative number when there's none. */
    private var lessonTime = -1f
    private var lessonLine = -1

    val inBaoLesson: Boolean get() = lessonTime >= 0f

    /** What's biting depends on this moment (plan 09, F1). */
    val fishingConditions: FishingConditions
        get() = FishingConditions(
            season = season,
            weather = engine.weather.name,
            isNight = engine.timeOfDayPhase.isNight,
            isSunset = engine.timeOfDayPhase.isSunset
        )

    fun startFishing(random: Random = Random.Default) {
        if (!canFish || fishingActive) return
        fishingActive = true
        engine.audio.playReelClick()
        if (!hasFished) {
            // The first time, Bao shows them how, then a slow practice bite (plan 09, F2).
            lessonTime = 0f
            lessonLine = -1
            fishingPhase = fishing.phase
            return
        }
        castLine()
        fishingPhase = fishing.phase
        // Now and then Bao says what's biting right now.
        val hint = baoHint(fishingConditions)
        engine.showMessage(GameText.get(if (hint != null && random.nextBoolean()) hint else Res.string.fishing_hint), duration = 3.5f)
    }

    private fun updateBaoLesson(dt: Float) {
        lessonTime += dt
        val line = (lessonTime / LESSON_LINE_SECONDS).toInt()
        if (line != lessonLine && line < BAO_LESSON.size) {
            lessonLine = line
            engine.showMessage(GameText.get(BAO_LESSON[line]), duration = LESSON_LINE_SECONDS)
        }
        if (line >= BAO_LESSON.size) {
            lessonTime = -1f
            hasFished = true
            castLine(practice = true)
        }
    }

    fun tapFishing() {
        when (fishing.tap()) {
            FishingGame.TapResult.HOOKED -> {
                engine.audio.playReelClick()
                engine.particles.spawnSparkles(engine.lastWorldW * BOBBER_X, engine.lastWorldH * BOBBER_Y, 8, Color(0xFFBDE0FE))
            }
            FishingGame.TapResult.TOO_SOON -> if (tooSoonHold <= 0f) {
                engine.showMessage(GameText.get(Res.string.fishing_too_soon), duration = 1.6f)
                tooSoonHold = 2f
            }
            FishingGame.TapResult.IGNORED -> Unit
        }
        fishingPhase = fishing.phase
    }

    fun stopFishing() {
        fishing.stop()
        fishingActive = false
        // He puts the rod down and turns back to her.
        if (engine.boy.heldItem == com.example.engine.HeldItem.ROD) {
            engine.boy.putAwayHeldItem()
            engine.boy.direction = Direction.RIGHT
        }
        lessonTime = -1f
        fishingPhase = fishing.phase
    }

    /**
     * While they fish, he holds the rod in both hands facing the water (plan 10, A), and the reel
     * turns while a catch comes in.
     */
    private fun holdRod() {
        val boy = engine.boy
        if (boy.heldItem != com.example.engine.HeldItem.ROD) boy.hold(com.example.engine.HeldItem.ROD, 5f)
        boy.heldItemTimeLeft = 5f
        boy.heldItemUse = if (fishing.phase == FishingGame.Phase.REELING) 0.5f else 0f
        boy.direction = if (BOBBER_X < boy.worldX) Direction.LEFT else Direction.RIGHT
    }

    /** A treasure for her: he gives it to her right there, and it goes on the shelf. */
    private fun giveTreasure(t: FishingCatch) {
        engine.giveGift(fromBoy = true, itemName = GameText.get(catchName(t)))
        report(ProgressEvent.GiftGiven("catch:${t.name}", fromBoy = true))
    }

    private fun updateFishing(dt: Float, cw: Float, ch: Float) {
        tooSoonHold = (tooSoonHold - dt).coerceAtLeast(0f)
        if (!fishingActive) return
        if (engine.currentScene != SceneType.SEASIDE_PIER) {
            stopFishing()
            return
        }
        holdRod()
        if (inBaoLesson) {
            updateBaoLesson(dt)
            fishingPhase = fishing.phase
            return
        }
        val before = fishing.phase
        val landed = fishing.update(dt)
        if (before == FishingGame.Phase.WAITING && fishing.phase == FishingGame.Phase.BITE) {
            engine.audio.playWaterDrip()
            engine.particles.spawnSparkles(cw * BOBBER_X, ch * BOBBER_Y, 4, Color(0xFFBDE0FE))
        }
        if (before == FishingGame.Phase.BITE && fishing.phase == FishingGame.Phase.WAITING) {
            engine.showMessage(GameText.get(Res.string.fishing_got_away), duration = 2.2f)
        }
        if (landed == FishingCatch.BOTTLE && dueBottle != null) {
            // A message in a bottle, for one of them: the line comes in and the screen opens it.
            val bottle = dueBottle
            dueBottle = null
            report(ProgressEvent.FishCaught(landed.name))
            engine.audio.playStarArpeggio()
            engine.particles.spawnSparkles(cw * BOBBER_X, ch * BOBBER_Y, 12, Color(0xFFBDE0FE))
            stopFishing()
            bottleLanded = bottle
            return
        }
        if (landed != null && landed.isTreasure) {
            report(ProgressEvent.FishCaught(landed.name))
            giveTreasure(landed)
        } else if (landed != null) {
            engine.audio.playHeartChime()
            if (landed == FishingCatch.GOLDEN_FISH) engine.audio.playStarArpeggio()
            engine.particles.spawnSparkles(cw * BOBBER_X, ch * BOBBER_Y, 10, if (landed == FishingCatch.GOLDEN_FISH) Color(0xFFFFD166) else null)
            cheer()
            engine.showMessage(GameText.get(Res.string.fishing_caught, GameText.get(catchName(landed))), duration = 3.5f)
            if (landed.isFish) hasCaughtFish = true
            report(ProgressEvent.FishCaught(landed.name))
        }
        // After a catch is shown, the line goes straight back in until they stop.
        if (fishing.phase == FishingGame.Phase.IDLE) castLine()
        fishingPhase = fishing.phase
    }

    // ── Garden ──

    /** The centre of the base of meadow plot [index] (0-2), or of sunroom pot [index] (3-4). */
    fun plotBase(index: Int, cw: Float, ch: Float) =
        if (GardenPlots.isPot(index)) Offset(cw * POT_XS[index - GardenPlots.PLOTS], ch * POTS_Y)
        else Offset(cw * PLOT_XS[index], ch * PLOT_Y)

    /** Handles a tap in the meadow; true if it was on a plot. */
    fun onGardenTap(x: Float, y: Float, cw: Float, ch: Float, p: Float, random: Random = Random.Default): Boolean {
        if (engine.currentScene != SceneType.FLOWER || engine.isDreamMode) return false
        return tapSpots(0 until GardenPlots.PLOTS, x, y, cw, ch, p, random)
    }

    /** Handles a tap in the sunroom; true if it was on one of the herb pots. */
    fun onPotTap(x: Float, y: Float, cw: Float, ch: Float, p: Float, random: Random = Random.Default): Boolean {
        if (engine.currentScene != SceneType.SUNROOM || engine.isDreamMode) return false
        return tapSpots(GardenPlots.PLOTS until GardenPlots.SPOTS, x, y, cw, ch, p, random)
    }

    private fun tapSpots(spots: IntRange, x: Float, y: Float, cw: Float, ch: Float, p: Float, random: Random): Boolean {
        val q = p * PLOT_SCALE
        val index = spots.firstOrNull { i ->
            val base = plotBase(i, cw, ch)
            kotlin.math.abs(x - base.x) < 6f * q && y > base.y - 11f * q && y < base.y + 3f * q
        } ?: return false
        lastTapP = p
        onSpot(index, plotBase(index, cw, ch), p)
        return true
    }

    /** The game-pixel size from the last garden tap, for the sparkles of a later choice. */
    private var lastTapP = 5f

    private fun onSpot(index: Int, base: Offset, p: Float) {
        val plot = garden.plots[index]
        val day = today()
        val owner = GardenPlots.ownerOf(index)
        when {
            plot.stage == PlotStage.EMPTY -> {
                val seeds = if (GardenPlots.isPot(index)) Seeds.forPots() else Seeds.forMeadow(season)
                if (seeds.isEmpty()) {
                    engine.showMessage(GameText.get(Res.string.garden_winter), duration = 3.5f)
                } else {
                    whoWatersSpot = null
                    seedPickerSpot = index
                }
            }
            plot.stage == PlotStage.BLOOM && plot.seed?.kind == SeedKind.FLOWER -> {
                val held = garden.stems.size + 1
                garden = garden.pick(index).first
                report(ProgressEvent.FlowerPicked(index))
                engine.audio.playHeartChime()
                engine.particles.spawnPetals(base.x, base.y - 6f * p, 5)
                if (held >= GardenPlots.BOUQUET_SIZE) {
                    cheer()
                    engine.showMessage(GameText.get(Res.string.garden_bouquet), duration = 3.5f)
                } else {
                    engine.showMessage(GameText.get(Res.string.garden_picked, GardenPlots.BOUQUET_SIZE - held), duration = 2.5f)
                }
            }
            plot.stage == PlotStage.BLOOM -> {
                val (after, produce) = garden.harvest(index)
                if (produce != null) {
                    garden = after
                    pantry = pantry + (produce to (pantry[produce] ?: 0) + 1)
                    report(ProgressEvent.CropHarvested(index))
                    engine.audio.playLeafRustle()
                    engine.particles.spawnSparkles(base.x, base.y - 6f * p, 8, Color(0xFFFFD166))
                    engine.showMessage(GameText.get(Res.string.garden_harvested, GameText.get(ingredientName(produce))), duration = 3f)
                }
            }
            owner == PlotOwner.SHARED && (garden.needsWater(index, day, PlotOwner.BOY) || garden.needsWater(index, day, PlotOwner.GIRL)) -> {
                // The shared plot wants both of them today: ask who this is.
                seedPickerSpot = null
                whoWatersSpot = index
            }
            garden.needsWater(index, day) -> waterSpot(index, day, owner.takeIf { it == PlotOwner.BOY || it == PlotOwner.GIRL }, base, p)
            else -> engine.showMessage(GameText.get(Res.string.garden_already_watered), duration = 2f)
        }
    }

    /** Plants [seed] in the spot the picker is open for. */
    fun chooseSeed(seed: Seed, random: Random = Random.Default) {
        val index = seedPickerSpot ?: return
        seedPickerSpot = null
        val before = garden
        garden = garden.plant(index, seed.id)
        if (garden == before) return
        report(ProgressEvent.GardenPlanted(index, seed.id))
        engine.audio.playLeafRustle()
        val base = plotBase(index, engine.lastWorldW, engine.lastWorldH)
        engine.particles.spawnGrassPuff(base.x, base.y, 4)
        val gardener = when (GardenPlots.ownerOf(index)) {
            PlotOwner.BOY -> engine.boy.name
            PlotOwner.GIRL -> engine.girl.name
            else -> if (random.nextBoolean()) engine.boy.name else engine.girl.name
        }
        engine.showMessage(GameText.get(Res.string.garden_planted, gardener, GameText.get(seedName(seed.id))), duration = 3f)
    }

    /** "Who is watering?" on the shared plot: [who] waters it. */
    fun waterAs(who: PlotOwner) {
        val index = whoWatersSpot ?: return
        whoWatersSpot = null
        val day = today()
        if (!garden.needsWater(index, day, who)) {
            engine.showMessage(GameText.get(Res.string.garden_already_watered), duration = 2f)
            return
        }
        waterSpot(index, day, who, plotBase(index, engine.lastWorldW, engine.lastWorldH), lastTapP)
    }

    fun closeGardenCards() {
        seedPickerSpot = null
        whoWatersSpot = null
    }

    private fun waterSpot(index: Int, day: Long, who: PlotOwner?, base: Offset, p: Float) {
        garden = garden.water(index, day, who)
        report(ProgressEvent.GardenWatered(index, day, who?.name))
        engine.audio.playWaterDrip()
        engine.particles.spawnSparkles(base.x, base.y - 4f * p, 6, Color(0xFF9AD1F5))
        val waiting = garden.waitingOnOther(index, day)
        val line = when {
            waiting != null -> GameText.get(
                Res.string.garden_waiting_for,
                nameOf(waiting),
                nameOf(if (waiting == PlotOwner.BOY) PlotOwner.GIRL else PlotOwner.BOY)
            )
            GardenPlots.ownerOf(index) == PlotOwner.SHARED -> GameText.get(Res.string.garden_watered_together)
            else -> GameText.get(Res.string.garden_watered)
        }
        engine.showMessage(line, duration = 3f)
    }

    private fun nameOf(owner: PlotOwner) = if (owner == PlotOwner.GIRL) engine.girl.name else engine.boy.name

    private fun updateGarden() {
        // Rain waters the garden, once a day, wherever the two of them are.
        if (engine.weather != WeatherType.RAIN) return
        val day = today()
        if (day == lastRainDay) return
        lastRainDay = day
        if ((0 until GardenPlots.PLOTS).none { garden.needsWater(it, day) }) return
        garden = garden.rain(day)
        report(ProgressEvent.GardenRained(day))
        if (engine.currentScene == SceneType.FLOWER) engine.showMessage(GameText.get(Res.string.garden_rained), duration = 3f)
    }

    // ── Shared ──

    fun update(dt: Float, cw: Float, ch: Float) {
        updateCooking(dt, cw, ch)
        updateFishing(dt, cw, ch)
        updateGarden()
        updateDinnerTalk(dt)
    }

    /** Puts everything away (a new scene, a dream). */
    fun stopAll() {
        if (cookingActive) stopCooking()
        if (fishingActive) stopFishing()
        cookingPicking = false
        closeGardenCards()
    }

    private fun cheer() {
        engine.boy.emote = EmoteType.HEART
        engine.girl.emote = EmoteType.HEART
        engine.boy.emoteTimer = 2.4f
        engine.girl.emoteTimer = 2.4f
    }

    companion object {
        /** The stove pot in the kitchen, as shares of the stage. */
        const val POT_X = 0.58f
        const val POT_Y = 0.59f
        /** Where the couple's bobber floats at the pier (Grandpa Bao fishes further right). */
        const val BOBBER_X = 0.23f
        const val BOBBER_Y = 0.55f
        /** The three garden plots along the front of the meadow. */
        val PLOT_XS = listOf(0.22f, 0.40f, 0.58f)
        const val PLOT_Y = 0.80f
        /** Plots are drawn at this many game pixels per sprite pixel. */
        const val PLOT_SCALE = 2f
        /** The two herb pots on the sunroom floor (the winter greenhouse). */
        val POT_XS = listOf(0.34f, 0.46f)
        const val POTS_Y = 0.74f
        /** Where they sit at the kitchen table to eat (plan 09, E4). */
        const val TABLE_BOY_X = 0.42f
        const val TABLE_GIRL_X = 0.58f
        const val MEAL_SECONDS = 9f
        const val DINNER_TALK_AFTER = 4.3f
        /** Grandpa Bao's lesson: three lines, then the practice bite (plan 09, F2). */
        val BAO_LESSON = listOf(Res.string.bao_lesson_1, Res.string.bao_lesson_2, Res.string.bao_lesson_3)
        const val LESSON_LINE_SECONDS = 3.2f

        /** Bao's hint about what's biting in [c], or null when it's an ordinary moment. */
        fun baoHint(c: FishingConditions): StringResource? = when {
            c.weather == "RAIN" -> Res.string.bao_hint_rain
            c.isNight -> Res.string.bao_hint_night
            c.season == "WINTER" -> Res.string.bao_hint_winter
            c.season == "SPRING" -> Res.string.bao_hint_spring
            c.isSunset -> Res.string.bao_hint_sunset
            else -> null
        }

        fun recipeName(id: String): StringResource = when (id) {
            "pancakes" -> Res.string.recipe_pancakes
            "soup" -> Res.string.recipe_soup
            "dumplings" -> Res.string.recipe_dumplings
            "cookies" -> Res.string.recipe_cookies
            "tomato_soup" -> Res.string.recipe_tomato_soup
            "strawberry_pancakes" -> Res.string.recipe_strawberry_pancakes
            "pumpkin_pie" -> Res.string.recipe_pumpkin_pie
            "herb_tea" -> Res.string.recipe_herb_tea
            "apple_crumble" -> Res.string.recipe_apple_crumble
            "pea_soup" -> Res.string.recipe_pea_soup
            else -> Res.string.recipe_tea
        }

        fun ingredientName(i: Ingredient): StringResource = when (i) {
            Ingredient.FLOUR -> Res.string.ingredient_flour
            Ingredient.EGG -> Res.string.ingredient_egg
            Ingredient.MILK -> Res.string.ingredient_milk
            Ingredient.BUTTER -> Res.string.ingredient_butter
            Ingredient.SUGAR -> Res.string.ingredient_sugar
            Ingredient.WATER -> Res.string.ingredient_water
            Ingredient.CARROT -> Res.string.ingredient_carrot
            Ingredient.POTATO -> Res.string.ingredient_potato
            Ingredient.ONION -> Res.string.ingredient_onion
            Ingredient.CABBAGE -> Res.string.ingredient_cabbage
            Ingredient.CHOCOLATE -> Res.string.ingredient_chocolate
            Ingredient.TEA_LEAVES -> Res.string.ingredient_tea_leaves
            Ingredient.HONEY -> Res.string.ingredient_honey
            Ingredient.STRAWBERRY -> Res.string.ingredient_strawberry
            Ingredient.PEAS -> Res.string.ingredient_peas
            Ingredient.TOMATO -> Res.string.ingredient_tomato
            Ingredient.BASIL -> Res.string.ingredient_basil
            Ingredient.PUMPKIN -> Res.string.ingredient_pumpkin
            Ingredient.APPLE -> Res.string.ingredient_apple
            Ingredient.MINT -> Res.string.ingredient_mint
        }

        /** A seed's name: a flower, or the crop it grows. */
        fun seedName(id: String): StringResource =
            Seeds.byId(id)?.produce?.let { ingredientName(it) } ?: flowerName(id)

        fun catchName(c: FishingCatch): StringResource = when (c) {
            FishingCatch.MINNOW -> Res.string.catch_minnow
            FishingCatch.CARP -> Res.string.catch_carp
            FishingCatch.SEASHELL -> Res.string.catch_seashell
            FishingCatch.OLD_BOOT -> Res.string.catch_old_boot
            FishingCatch.BOTTLE -> Res.string.catch_bottle
            FishingCatch.GOLDEN_FISH -> Res.string.catch_golden_fish
            FishingCatch.MOON_JELLY -> Res.string.catch_moon_jelly
            FishingCatch.GLOW_SQUID -> Res.string.catch_glow_squid
            FishingCatch.RAIN_TROUT -> Res.string.catch_rain_trout
            FishingCatch.ICE_COD -> Res.string.catch_ice_cod
            FishingCatch.BLOSSOM_KOI -> Res.string.catch_blossom_koi
            FishingCatch.PEARL -> Res.string.catch_pearl
            FishingCatch.HEART_SHELL -> Res.string.catch_heart_shell
            FishingCatch.SEA_GLASS_HEART -> Res.string.catch_sea_glass_heart
        }

        fun flowerName(id: String): StringResource = when (id) {
            "sunflower" -> Res.string.flower_sunflower
            "tulip" -> Res.string.flower_tulip
            "daisy" -> Res.string.flower_daisy
            "lavender" -> Res.string.flower_lavender
            else -> Res.string.flower_forget_me_not
        }
    }
}
