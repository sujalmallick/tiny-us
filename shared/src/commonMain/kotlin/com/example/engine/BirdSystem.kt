package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.scene.SceneType
import com.example.scene.WeatherType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * All bird randomness flows through here so previews can seed it (see [WorldRandom]). Shadows the
 * simple name `Random` in this file; production still uses [kotlin.random.Random.Default].
 */
private val Random: kotlin.random.Random get() = WorldRandom.rng

/** Prints bird decisions to the console when true (was Android's Log.d). */
private const val DEBUG_BIRDS = false

private fun logBird(msg: String) {
    try {
        if (DEBUG_BIRDS) println("BirdSystem: $msg")
    } catch (_: Throwable) {
        // Ignored in unit test environment
    }
}

enum class BirdSpecies {
    SPARROW,
    BLUEBIRD,
    WHITE_DOVE
}

enum class BirdState {
    INACTIVE,
    FLYING_IN,
    GLIDING,
    LANDING,
    PERCHED,
    PECKING,
    LOOKING_AT_CAT,
    STARTLED,
    FLYING_AWAY,
    SKY_CROSSING
}

enum class PerchSurface {
    MEADOW_ROOF,
    MEADOW_GROUND,
    TREE_BRANCH_RIGHT,
    TREE_BRANCH_LEFT,
    TREE_GROUND,
    TWILIGHT_GROUND,
    TWILIGHT_JAR,
    SKY_TRANSIT
}

class BirdEntity {
    var isActive: Boolean = false
    var species: BirdSpecies = BirdSpecies.SPARROW
    var state: BirdState = BirdState.INACTIVE
    var surface: PerchSurface = PerchSurface.SKY_TRANSIT

    var x: Float = -100f
    var y: Float = -100f
    var vx: Float = 0f
    var vy: Float = 0f
    var targetX: Float = 0f
    var targetY: Float = 0f

    var facingLeft: Boolean = false
    var animPhase: Float = 0f
    var stateTimer: Float = 0f
    var peckTimer: Float = 0f
    var isPeckingDown: Boolean = false
    var alpha: Float = 1f

    fun reset() {
        isActive = false
        species = BirdSpecies.SPARROW
        state = BirdState.INACTIVE
        surface = PerchSurface.SKY_TRANSIT
        x = -100f
        y = -100f
        vx = 0f
        vy = 0f
        targetX = 0f
        targetY = 0f
        facingLeft = false
        animPhase = 0f
        stateTimer = 0f
        peckTimer = 0f
        isPeckingDown = false
        alpha = 1f
    }

    fun initSpawn(
        chosenSpecies: BirdSpecies,
        chosenSurface: PerchSurface,
        startX: Float,
        startY: Float,
        destX: Float,
        destY: Float,
        flyLeft: Boolean
    ) {
        reset()
        isActive = true
        species = chosenSpecies
        surface = chosenSurface
        x = startX
        y = startY
        targetX = destX
        targetY = destY
        facingLeft = flyLeft
        animPhase = Random.nextFloat() * 3.14f
        stateTimer = 0f
        peckTimer = 0f
        isPeckingDown = false
        alpha = 1f

        if (chosenSurface == PerchSurface.SKY_TRANSIT) {
            state = BirdState.SKY_CROSSING
            val speed = 70f + Random.nextFloat() * 30f
            vx = if (facingLeft) -speed else speed
            vy = (Random.nextFloat() - 0.5f) * 15f
        } else {
            state = BirdState.FLYING_IN
            val dx = destX - startX
            val dy = destY - startY
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val speed = 80f + Random.nextFloat() * 25f
            vx = (dx / dist) * speed
            vy = (dy / dist) * speed
        }
    }

    fun startle() {
        if (!isActive) return
        state = BirdState.STARTLED
        stateTimer = 0f
        isPeckingDown = false
        vy = -160f - Random.nextFloat() * 40f
        vx = if (facingLeft) -110f - Random.nextFloat() * 30f else 110f + Random.nextFloat() * 30f
        logBird("Bird species=$species STARTLED! vy=$vy, vx=$vx")
    }

    fun update(
        dt: Float,
        cw: Float,
        ch: Float,
        p: Float,
        catX: Float,
        catY: Float,
        isCatClose: Boolean
    ): Boolean {
        if (!isActive) return false

        animPhase += dt * 9f
        stateTimer += dt

        when (state) {
            BirdState.INACTIVE -> return false

            BirdState.SKY_CROSSING -> {
                val flapCycle = (animPhase % 2.5f)
                val isGliding = flapCycle > 1.6f
                val waveY = if (isGliding) 0f else sin(animPhase * 5f) * 6f * p * dt
                x += vx * dt * (p / 3.5f)
                y += (vy * dt + waveY) * (p / 3.5f)

                if (x < -60f * p || x > cw + 60f * p || y < -60f * p) {
                    reset()
                    return false
                }
            }

            BirdState.FLYING_IN -> {
                val dx = targetX - x
                val dy = targetY - y
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < 12f * p) {
                    state = BirdState.LANDING
                    stateTimer = 0f
                    x = targetX
                    y = targetY
                } else {
                    val flapCycle = (animPhase % 2.2f)
                    val isGliding = flapCycle > 1.4f
                    val waveY = if (isGliding) 0f else sin(animPhase * 4.5f) * 5f * p * dt
                    val targetVx = (dx / dist) * (75f + Random.nextFloat() * 15f)
                    val targetVy = (dy / dist) * (75f + Random.nextFloat() * 15f)
                    vx += (targetVx - vx) * (dt * 3.5f)
                    vy += (targetVy - vy) * (dt * 3.5f)
                    x += vx * dt * (p / 3.5f)
                    y += (vy * dt + waveY) * (p / 3.5f)
                    facingLeft = vx < 0f
                }
            }

            BirdState.GLIDING -> {
                x += vx * dt * (p / 3.5f)
                y += vy * dt * (p / 3.5f)
                val dx = targetX - x
                val dy = targetY - y
                if (sqrt(dx * dx + dy * dy) < 8f * p) {
                    state = BirdState.LANDING
                    stateTimer = 0f
                    x = targetX
                    y = targetY
                }
            }

            BirdState.LANDING -> {
                if (stateTimer > 0.35f) {
                    state = when (surface) {
                        PerchSurface.MEADOW_GROUND,
                        PerchSurface.TREE_GROUND,
                        PerchSurface.TWILIGHT_GROUND -> BirdState.PECKING
                        else -> BirdState.PERCHED
                    }
                    stateTimer = 0f
                    peckTimer = 0f
                    isPeckingDown = false
                }
            }

            BirdState.PERCHED -> {
                if (isCatClose) {
                    val distCat = sqrt((catX - x) * (catX - x) + (catY - y) * (catY - y))
                    if (distCat < 26f * p) {
                        startle()
                        return true
                    } else if (distCat < 55f * p) {
                        state = BirdState.LOOKING_AT_CAT
                        facingLeft = catX < x
                        stateTimer = 0f
                    }
                }

                if (stateTimer >= 5.0f + (x.toInt() % 3)) {
                    state = BirdState.FLYING_AWAY
                    stateTimer = 0f
                    vy = -110f - Random.nextFloat() * 25f
                    vx = if (facingLeft) -80f - Random.nextFloat() * 25f else 80f + Random.nextFloat() * 25f
                }
            }

            BirdState.PECKING -> {
                peckTimer += dt
                if (peckTimer >= 0.85f) {
                    isPeckingDown = true
                    if (peckTimer >= 1.15f) {
                        peckTimer = 0f
                        isPeckingDown = false
                    }
                }

                if (isCatClose) {
                    val distCat = sqrt((catX - x) * (catX - x) + (catY - y) * (catY - y))
                    if (distCat < 26f * p) {
                        startle()
                        return true
                    } else if (distCat < 55f * p) {
                        state = BirdState.LOOKING_AT_CAT
                        facingLeft = catX < x
                        stateTimer = 0f
                    }
                }

                if (stateTimer >= 5.5f + (y.toInt() % 4)) {
                    state = BirdState.FLYING_AWAY
                    stateTimer = 0f
                    isPeckingDown = false
                    vy = -120f - Random.nextFloat() * 25f
                    vx = if (facingLeft) -85f - Random.nextFloat() * 25f else 85f + Random.nextFloat() * 25f
                }
            }

            BirdState.LOOKING_AT_CAT -> {
                if (isCatClose) {
                    val distCat = sqrt((catX - x) * (catX - x) + (catY - y) * (catY - y))
                    if (distCat < 28f * p) {
                        startle()
                        return true
                    }
                }
                if (stateTimer >= 2.5f) {
                    state = BirdState.PERCHED
                    stateTimer = 0f
                }
            }

            BirdState.STARTLED -> {
                x += vx * dt * (p / 3.5f)
                y += vy * dt * (p / 3.5f)
                vy -= 35f * dt * (p / 3.5f)

                if (stateTimer >= 0.4f) {
                    state = BirdState.FLYING_AWAY
                }
            }

            BirdState.FLYING_AWAY -> {
                val waveY = sin(animPhase * 5.5f) * 6f * p * dt
                x += vx * dt * (p / 3.5f)
                y += (vy * dt + waveY) * (p / 3.5f)

                if (x < -60f * p || x > cw + 60f * p || y < -60f * p) {
                    reset()
                    return false
                }
            }
        }

        return true
    }

    fun draw(scope: DrawScope, p: Float) {
        if (!isActive) return

        val (bodyColor, breastColor, wingColor, beakColor, eyeColor) = when (species) {
            BirdSpecies.SPARROW -> Quintuple(
                Color(0xFF8D5B4C),
                Color(0xFFD7C4B7),
                Color(0xFF5C382C),
                Color(0xFFE09F3E),
                Color(0xFF222222)
            )
            BirdSpecies.BLUEBIRD -> Quintuple(
                Color(0xFF3A86FF),
                Color(0xFFE8F1F5),
                Color(0xFF1D4ED8),
                Color(0xFFFFB703),
                Color(0xFF1E293B)
            )
            BirdSpecies.WHITE_DOVE -> Quintuple(
                Color(0xFFF8F9FA),
                Color(0xFFFFECEB),
                Color(0xFFCBD5E1),
                Color(0xFFFFB5A7),
                Color(0xFF333333)
            )
        }

        val inFlight = state == BirdState.FLYING_IN ||
            state == BirdState.GLIDING ||
            state == BirdState.STARTLED ||
            state == BirdState.FLYING_AWAY ||
            state == BirdState.SKY_CROSSING

        val dir = if (facingLeft) -1f else 1f
        val bobY = if (!inFlight && !isPeckingDown) sin(animPhase * 0.8f) * 0.5f * p else 0f
        val curX = x
        val curY = y + bobY

        // Shadow on perching surface
        if (!inFlight) {
            scope.drawRect(
                Color(0x28000000),
                Offset(curX - 4f * p, curY + 3.5f * p),
                Size(8f * p, 1.5f * p)
            )
        }

        // Tail feathers
        scope.drawRect(
            wingColor,
            Offset(curX - dir * 4.5f * p - if (facingLeft) 2.5f * p else 0f, curY - 0.5f * p),
            Size(2.5f * p, 1.8f * p)
        )

        // Main body
        scope.drawRect(
            bodyColor,
            Offset(curX - 3f * p, curY - 2.5f * p),
            Size(6f * p, 4.5f * p)
        )

        // Breast highlight
        scope.drawRect(
            breastColor,
            Offset(curX + dir * 0.8f * p - if (facingLeft) 2f * p else 0f, curY - 0.5f * p),
            Size(2f * p, 2.5f * p)
        )

        // Head and beak position (dipped during pecking)
        val headOffY = if (isPeckingDown) 2f * p else -2f * p
        val headOffX = if (isPeckingDown) dir * 1f * p else 0f

        // Head
        scope.drawRect(
            bodyColor,
            Offset(curX + dir * 2f * p - if (facingLeft) 3f * p else 0f + headOffX, curY - 3.5f * p + headOffY),
            Size(3f * p, 3f * p)
        )

        // Eye
        val eyeX = curX + dir * 2.8f * p + headOffX
        val eyeY = curY - 2.6f * p + headOffY
        scope.drawRect(
            eyeColor,
            Offset(eyeX, eyeY),
            Size(1f * p, 1f * p)
        )

        // Beak
        val beakX = if (facingLeft) curX - 4.5f * p + headOffX else curX + 4.5f * p + headOffX
        val beakY = curY - 1.8f * p + headOffY + if (isPeckingDown) p else 0f
        scope.drawRect(
            beakColor,
            Offset(beakX, beakY),
            Size(1.8f * p, 1.4f * p)
        )

        // Legs/Feet when perched or pecking
        if (!inFlight) {
            val footColor = Color(0xFF4A4A4A)
            scope.drawRect(footColor, Offset(curX - 1.2f * p, curY + 2f * p), Size(0.9f * p, 1.8f * p))
            scope.drawRect(footColor, Offset(curX + 1.2f * p, curY + 2f * p), Size(0.9f * p, 1.8f * p))
        }

        // Wing rendering: resting fold vs flapping wing
        if (inFlight) {
            val flapPhase = (animPhase % 1.6f)
            val wingFlapUp = flapPhase < 0.8f
            if (wingFlapUp) {
                // Raised wing
                scope.drawRect(
                    wingColor,
                    Offset(curX - 2f * p, curY - 6.5f * p),
                    Size(4.2f * p, 4.5f * p)
                )
                scope.drawRect(
                    breastColor,
                    Offset(curX - 1.5f * p, curY - 5.5f * p),
                    Size(2f * p, 2f * p)
                )
            } else {
                // Downward wing
                scope.drawRect(
                    wingColor,
                    Offset(curX - 2f * p, curY + 0.5f * p),
                    Size(4.2f * p, 3.8f * p)
                )
            }
        } else {
            // Folded wing
            scope.drawRect(
                wingColor,
                Offset(curX - dir * 1f * p - if (facingLeft) 3.5f * p else 0f, curY - 2.2f * p),
                Size(3.6f * p, 3f * p)
            )
        }
    }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}

class BirdSystem {
    /** The weather now, for where on the tree a bird can sit. */
    private var weatherNow = WeatherType.SUNNY

    companion object {
        const val MAX_POOL_SIZE = 3
        const val MAX_ACTIVE_BIRDS = 2
        const val TAP_HIT_RADIUS_FACTOR = 14f
    }

    private val pool = ArrayList<BirdEntity>(MAX_POOL_SIZE).apply {
        repeat(MAX_POOL_SIZE) {
            add(BirdEntity())
        }
    }

    val activeBirds = ArrayList<BirdEntity>(MAX_POOL_SIZE)

    private var spawnTimer: Float = 0f
    // The first bird of a visit comes within seconds, so people actually see one; later ones are rarer.
    private var nextSpawnInterval: Float = firstSpawnInterval()

    private fun firstSpawnInterval() = 4f + Random.nextFloat() * 6f

    private val speciesList = listOf(BirdSpecies.SPARROW, BirdSpecies.BLUEBIRD, BirdSpecies.WHITE_DOVE)
    private var speciesIndex = 0

    fun getPoolAvailableCount(): Int = pool.size
    fun getActiveCount(): Int = activeBirds.size

    fun clear() {
        for (i in activeBirds.indices) {
            val b = activeBirds[i]
            b.reset()
            if (pool.size < MAX_POOL_SIZE) {
                pool.add(b)
            }
        }
        activeBirds.clear()
        spawnTimer = 0f
        nextSpawnInterval = firstSpawnInterval()
    }

    fun isOutdoorScene(scene: SceneType): Boolean {
        return scene == SceneType.FLOWER ||
            scene == SceneType.UNDER_TREE ||
            scene == SceneType.LOOKING
    }

    fun isWeatherEligible(weather: WeatherType, isNight: Boolean): Boolean {
        if (isNight) return false
        return when (weather) {
            WeatherType.SNOW -> false
            WeatherType.RAIN -> false // High barrier for rain in normal cycle
            WeatherType.SUNNY,
            WeatherType.SAKURA,
            WeatherType.AUTUMN -> true
        }
    }

    fun spawnBird(
        cw: Float,
        ch: Float,
        p: Float,
        scene: SceneType,
        forcedSurface: PerchSurface? = null,
        forcedSpecies: BirdSpecies? = null
    ): BirdEntity? {
        if (activeBirds.size >= MAX_ACTIVE_BIRDS || pool.isEmpty()) {
            return null
        }

        val bird = pool.removeAt(pool.size - 1)
        val species = forcedSpecies ?: run {
            val sp = speciesList[speciesIndex % speciesList.size]
            speciesIndex++
            sp
        }

        val surface = forcedSurface ?: pickSurfaceForScene(scene)
        val flyFromLeft = Random.nextBoolean()
        val startX = if (flyFromLeft) -35f * p else cw + 35f * p
        val startY = ch * (0.08f + Random.nextFloat() * 0.22f)

        val (destX, destY) = getSurfaceCoordinates(surface, cw, ch, p)

        bird.initSpawn(
            chosenSpecies = species,
            chosenSurface = surface,
            startX = startX,
            startY = startY,
            destX = destX,
            destY = destY,
            flyLeft = if (surface == PerchSurface.SKY_TRANSIT) !flyFromLeft else (destX < startX)
        )

        activeBirds.add(bird)
        logBird("Spawned bird species=$species surface=$surface at ($startX, $startY) -> target ($destX, $destY)")
        return bird
    }

    private fun pickSurfaceForScene(scene: SceneType): PerchSurface {
        return when (scene) {
            SceneType.FLOWER -> {
                when (Random.nextInt(3)) {
                    0 -> PerchSurface.MEADOW_ROOF
                    1 -> PerchSurface.MEADOW_GROUND
                    else -> PerchSurface.SKY_TRANSIT
                }
            }
            SceneType.UNDER_TREE -> {
                // In the snow the bare branches are the place to be
                if (weatherNow == WeatherType.SNOW) return PerchSurface.TREE_BRANCH_RIGHT
                when (Random.nextInt(4)) {
                    0 -> PerchSurface.TREE_BRANCH_RIGHT
                    1 -> PerchSurface.TREE_BRANCH_LEFT
                    2 -> PerchSurface.TREE_GROUND
                    else -> PerchSurface.SKY_TRANSIT
                }
            }
            SceneType.LOOKING -> {
                when (Random.nextInt(3)) {
                    0 -> PerchSurface.TWILIGHT_GROUND
                    1 -> PerchSurface.TWILIGHT_JAR
                    else -> PerchSurface.SKY_TRANSIT
                }
            }
            else -> PerchSurface.SKY_TRANSIT
        }
    }

    fun getSurfaceCoordinates(surface: PerchSurface, cw: Float, ch: Float, p: Float): Pair<Float, Float> {
        return when (surface) {
            PerchSurface.MEADOW_ROOF -> {
                // On the cottage roof, right of the ridge and clear of the chimney smoke
                val cp = p * com.example.scene.MeadowLayout.COTTAGE_SCALE
                Pair(com.example.scene.MeadowLayout.cottageX(p) + 6f * cp, com.example.scene.MeadowLayout.groundY(ch) - 50f * cp)
            }
            PerchSurface.MEADOW_GROUND -> {
                // Open lawn between flowers and characters
                Pair(cw * (0.64f + Random.nextFloat() * 0.12f), ch * 0.70f)
            }
            PerchSurface.TREE_BRANCH_RIGHT, PerchSurface.TREE_BRANCH_LEFT -> {
                // On a branch of the tree on the hill, where there's open sky above it: the swing
                // branch under the leaves, anywhere along the bare branches in winter
                val perches = SeasonalTree.perches(weatherNow)
                val perch = if (surface == PerchSurface.TREE_BRANCH_LEFT) perches.firstOrNull() else perches.randomOrNull()
                if (perch != null) {
                    SeasonalTree.perchAt(perch, cw * SeasonalTree.SCENE_X, ch * SeasonalTree.SCENE_GROUND, p)
                } else {
                    Pair(cw * 0.50f - 24f * p, ch * 0.69f - 76f * p)
                }
            }
            PerchSurface.TREE_GROUND -> {
                // Tree hill left meadow clearing
                Pair(cw * (0.16f + Random.nextFloat() * 0.12f), ch * 0.71f)
            }
            PerchSurface.TWILIGHT_GROUND -> {
                // Twilight open meadow grass strictly on far left, safely clear of Boy at 0.38f-0.44f
                Pair(cw * (0.12f + Random.nextFloat() * 0.12f), ch * 0.71f)
            }
            PerchSurface.TWILIGHT_JAR -> {
                // Wooden lid on top of the fairy light jar
                Pair(cw * 0.76f, ch * 0.70f - 14f * p)
            }
            PerchSurface.SKY_TRANSIT -> {
                Pair(if (Random.nextBoolean()) -40f * p else cw + 40f * p, ch * 0.18f)
            }
        }
    }

    fun update(
        dt: Float,
        cw: Float,
        ch: Float,
        p: Float,
        scene: SceneType,
        weather: WeatherType,
        isNight: Boolean,
        catX: Float,
        catY: Float
    ) {
        if (!isOutdoorScene(scene)) {
            if (activeBirds.isNotEmpty()) {
                clear()
            }
            return
        }

        weatherNow = weather
        // Spawn timer (a little bird comes to sit in the bare branches of the tree in the snow, too)
        if (isWeatherEligible(weather, isNight) || (weather == WeatherType.SNOW && !isNight && scene == SceneType.UNDER_TREE)) {
            spawnTimer += dt
            if (spawnTimer >= nextSpawnInterval) {
                spawnTimer = 0f
                nextSpawnInterval = 28f + Random.nextFloat() * 16f
                if (activeBirds.size < MAX_ACTIVE_BIRDS) {
                    spawnBird(cw, ch, p, scene)
                }
            }
        } else if (weather == WeatherType.RAIN && !isNight) {
            // Rare rain transit
            spawnTimer += dt
            if (spawnTimer >= 140f) {
                spawnTimer = 0f
                if (activeBirds.size < 1 && Random.nextFloat() < 0.25f) {
                    spawnBird(cw, ch, p, scene, forcedSurface = PerchSurface.SKY_TRANSIT)
                }
            }
        }

        // Update active birds with in-place compaction
        var writeIndex = 0
        for (i in activeBirds.indices) {
            val bird = activeBirds[i]
            val stillActive = bird.update(dt, cw, ch, p, catX, catY, isCatClose = true)
            if (stillActive) {
                activeBirds[writeIndex++] = bird
            } else {
                val despawnedSpecies = bird.species
                bird.reset()
                if (pool.size < MAX_POOL_SIZE) {
                    pool.add(bird)
                }
                logBird("Bird species=$despawnedSpecies DESPAWNED. Recycled to pool. Pool size: ${pool.size}")
            }
        }
        while (activeBirds.size > writeIndex) {
            activeBirds.removeAt(activeBirds.size - 1)
        }
    }

    fun drawBirds(scope: DrawScope, p: Float) {
        for (i in activeBirds.indices) {
            activeBirds[i].draw(scope, p)
        }
    }

    fun onTouchBird(tapX: Float, tapY: Float, p: Float): Boolean {
        val hitRadius = TAP_HIT_RADIUS_FACTOR * p
        val hitRadiusSq = hitRadius * hitRadius

        for (i in activeBirds.indices) {
            val bird = activeBirds[i]
            if (bird.isActive && (
                bird.state == BirdState.PERCHED ||
                bird.state == BirdState.PECKING ||
                bird.state == BirdState.LOOKING_AT_CAT
            )) {
                val dx = tapX - bird.x
                val dy = tapY - bird.y
                if (dx * dx + dy * dy <= hitRadiusSq) {
                    logBird("onTouchBird DIRECT HIT on bird species=${bird.species} at (${bird.x}, ${bird.y})")
                    bird.startle()
                    return true
                }
            }
        }
        return false
    }
}
