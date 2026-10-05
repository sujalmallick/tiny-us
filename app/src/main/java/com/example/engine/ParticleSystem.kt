package com.example.engine

import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.math.exp
import kotlin.random.Random

data class PixelParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var color: Color,
    var alpha: Float = 1f,
    var maxLife: Float = 60f,
    var currentLife: Float = 0f,
    var type: ParticleType = ParticleType.LEAF,
    var text: String = "",
    var phase: Float = 0f,
    var targetGroundY: Float = 0f,
    var depth: Float = 1f,
    var weatherFadeRemaining: Float = -1f,
    var weatherFadeStartAlpha: Float = 1f
)

data class FallenParticle(
    var normX: Float,
    var normY: Float,
    val type: ParticleType,
    val color: Color,
    val size: Float,
    val styleVariant: Int = 0,
    var alpha: Float = 0.95f,
    var isSwept: Boolean = false,
    var sweptVx: Float = 0f,
    var sweptVy: Float = 0f,
    var sweptLife: Float = 0f,
    var fadeRemaining: Float = -1f,
    /** Where the particle lies on the ground; a tossed particle lands back on this line. */
    var restNormY: Float = normY
)

/** A footprint, pawprint or finger trace left in fresh snow. */
data class SnowPrint(
    val normX: Float,
    val normY: Float,
    val kind: SnowPrintKind,
    val facingLeft: Boolean,
    var age: Float = 0f
)

enum class SnowPrintKind { FOOT, PAW, TRACE }

/** A rain puddle on the ground; [size] grows 0..1 while it rains and shrinks as it dries. */
/** A puddle spot on the ground (fractions of the scene); it fills in the rain and dries after. */
data class Puddle(var normX: Float, var normY: Float, var size: Float = 0f)

enum class ParticleType {
    LEAF,
    HEART,
    STAR,
    SLEEP_Z,
    STEAM,
    SPARKLE,
    PETAL,
    FIREFLY,
    SHOOTING_STAR,
    CHIMNEY_SMOKE,
    WATER_RIPPLE,
    MUSIC_NOTE,
    RAIN_DROP,
    RAIN_SPLASH,
    SAKURA_PETAL,
    AUTUMN_LEAF,
    SNOWFLAKE,
    DANDELION_FLUFF,
    WIND_BREEZE
}

private val LEAF_COLORS = arrayOf(
    Color(0xFF55A630),
    Color(0xFF80B918),
    Color(0xFFE76F51),
    Color(0xFFF4A261),
    Color(0xFF2D6A4F)
)

private val PETAL_COLORS = arrayOf(
    Color(0xFFFFB5C2),
    Color(0xFFFFC6D9),
    Color(0xFFFF7597),
    Color(0xFFFFF0F3)
)

private val MUSIC_NOTE_COLORS = arrayOf(
    Color(0xFFFF5D8F),
    Color(0xFF4361EE),
    Color(0xFFFFD166),
    Color(0xFF70E000)
)

private val GRASS_COLORS = arrayOf(
    Color(0xFF96D678),
    Color(0xFF7DC26A),
    Color(0xFF68AE55),
    Color(0xFFD4A373)
)

private val SAKURA_PETAL_COLORS = arrayOf(
    Color(0xFFFF758F),
    Color(0xFFFF85A1),
    Color(0xFFFFB3C1),
    Color(0xFFFFC2D1),
    Color(0xFFFFF0F5)
)

private val AUTUMN_LEAF_COLORS = arrayOf(
    Color(0xFFE76F51),
    Color(0xFFF4A261),
    Color(0xFFE9C46A),
    Color(0xFFD62828),
    Color(0xFFBC4749),
    Color(0xFF9A031E)
)

private val SNOW_COLORS = arrayOf(
    Color(0xFFFFFFFF),
    Color(0xFFF0F8FF),
    Color(0xFFEDF2F7),
    Color(0xFFE2E8F0)
)

private val SUN_SPARKLE_COLORS = arrayOf(
    Color(0xFFFFF3B0),
    Color(0xFFFFD166),
    Color(0xFFFFE66D),
    Color(0xFFFFFFFF)
)

private fun eventChance(ratePerSecond: Float, deltaSeconds: Float): Boolean =
    Random.nextFloat() < (1f - exp(-ratePerSecond * deltaSeconds.coerceAtLeast(0f)))

class ParticleSystem {
    companion object {
        private const val WEATHER_FADE_SECONDS = 0.42f

        /** Seconds a full-depth particle takes to fall the height of the scene: lively, but slow
         * enough to watch (and to catch a snowflake or petal). */
        const val RAIN_TRAVERSAL_SECONDS = 1.3f
        const val SAKURA_TRAVERSAL_SECONDS = 9f
        const val AUTUMN_TRAVERSAL_SECONDS = 8f
        const val SNOW_TRAVERSAL_SECONDS = 11f

        /** Where a falling particle enters: just above the top of the scene. */
        private fun entryY(ch: Float) = -ch * (0.03f + Random.nextFloat() * 0.08f)

        /** How long prints stay in the snow before the fresh fall covers them. */
        const val SNOW_PRINT_SECONDS = 35f
        private const val MAX_SNOW_PRINTS = 220
        /** Seconds of rain for a puddle to fill, and of dry weather for it to vanish. */
        const val PUDDLE_FILL_SECONDS = 20f
        const val PUDDLE_DRY_SECONDS = 40f
        /** Puddle spots on the ground band, shared by every outdoor scene. */
        val PUDDLE_SPOTS = listOf(0.24f to 0.84f, 0.60f to 0.80f, 0.82f to 0.86f)

        /** Falling weather the couple can catch with a tap. Rain is too dense to single out. */
        val CATCHABLE_WEATHER = setOf(
            ParticleType.SNOWFLAKE, ParticleType.SAKURA_PETAL,
            ParticleType.AUTUMN_LEAF, ParticleType.DANDELION_FLUFF
        )
    }

    val particles = mutableListOf<PixelParticle>()
    private val pendingParticles = mutableListOf<PixelParticle>()
    val fallenParticles = mutableListOf<FallenParticle>()
    val snowPrints = ArrayList<SnowPrint>(MAX_SNOW_PRINTS)
    val puddles: List<Puddle> = PUDDLE_SPOTS.map { (x, y) -> Puddle(x, y) }

    /** Moves the puddle spots to fit a scene's ground; puddles that move start dry. */
    fun placePuddles(spots: List<Pair<Float, Float>>) {
        for ((i, puddle) in puddles.withIndex()) {
            val (x, y) = spots.getOrNull(i) ?: PUDDLE_SPOTS[i]
            if (puddle.normX != x || puddle.normY != y) {
                puddle.normX = x
                puddle.normY = y
                puddle.size = 0f
            }
        }
    }
    var groundSeededWeather: com.example.scene.WeatherType? = null

    private val particlePool = ArrayList<PixelParticle>(256)
    private var lastChimneySmokeNanos = 0L
    private var rainSpawnAccumulator = 0f
    private var seasonalSpawnAccumulator = 0f
    private var activeWeather: com.example.scene.WeatherType? = null

    fun obtainParticle(
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
        size: Float,
        color: Color,
        maxLife: Float,
        type: ParticleType,
        text: String = "",
        phase: Float = 0f,
        targetGroundY: Float = 0f,
        alpha: Float = 1f,
        depth: Float = 1f
    ): PixelParticle {
        if (particlePool.isNotEmpty()) {
            val p = particlePool.removeAt(particlePool.size - 1)
            p.x = x
            p.y = y
            p.vx = vx
            p.vy = vy
            p.size = size
            p.color = color
            p.alpha = alpha
            p.maxLife = maxLife
            p.currentLife = 0f
            p.type = type
            p.text = text
            p.phase = phase
            p.targetGroundY = targetGroundY
            p.depth = depth
            p.weatherFadeRemaining = -1f
            p.weatherFadeStartAlpha = alpha
            return p
        }
        return PixelParticle(
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            size = size,
            color = color,
            alpha = alpha,
            maxLife = maxLife,
            currentLife = 0f,
            type = type,
            text = text,
            phase = phase,
            targetGroundY = targetGroundY,
            depth = depth,
            weatherFadeStartAlpha = alpha
        )
    }

    fun recycleParticle(p: PixelParticle) {
        if (particlePool.size < 256) {
            particlePool.add(p)
        }
    }

    fun clearSeasonalParticles() {
        var write = 0
        for (i in 0 until particles.size) {
            val p = particles[i]
            val isSeasonal = p.type == ParticleType.SAKURA_PETAL ||
                p.type == ParticleType.AUTUMN_LEAF ||
                p.type == ParticleType.SNOWFLAKE ||
                p.type == ParticleType.RAIN_DROP ||
                p.type == ParticleType.RAIN_SPLASH ||
                p.type == ParticleType.WATER_RIPPLE ||
                p.type == ParticleType.DANDELION_FLUFF ||
                p.type == ParticleType.WIND_BREEZE
            if (isSeasonal) {
                if (p.weatherFadeRemaining < 0f) {
                    p.weatherFadeRemaining = WEATHER_FADE_SECONDS
                    p.weatherFadeStartAlpha = p.alpha
                }
                particles[write++] = p
            } else {
                particles[write++] = p
            }
        }
        while (particles.size > write) {
            particles.removeAt(particles.size - 1)
        }
        // Pending particles are deferred rain splash details; discard them on weather change.
        for (p in pendingParticles) recycleParticle(p)
        pendingParticles.clear()
        for (fp in fallenParticles) {
            if (fp.fadeRemaining < 0f) fp.fadeRemaining = WEATHER_FADE_SECONDS
        }
        snowPrints.clear()
        groundSeededWeather = null
    }

    fun seedInitialGroundFoliage(
        weather: com.example.scene.WeatherType,
        cw: Float,
        ch: Float
    ) {
        fallenParticles.clear()
        when (weather) {
            com.example.scene.WeatherType.AUTUMN -> {
                // Natural cozy scatter of fallen autumn leaves across meadow grass
                val count = 22 + Random.nextInt(7)
                repeat(count) {
                    val nx = 0.04f + Random.nextFloat() * 0.92f
                    // Range 0.70-0.90 — keep particles in the visible grass zone,
                    // not all at the same horizon line (was 0.68-0.95, too close to edge).
                    val ny = 0.70f + Random.nextFloat() * 0.20f
                    fallenParticles.add(
                        FallenParticle(
                            normX = nx,
                            normY = ny,
                            type = ParticleType.AUTUMN_LEAF,
                            color = AUTUMN_LEAF_COLORS.random(),
                            size = 3.2f + Random.nextFloat() * 1.5f,
                            styleVariant = Random.nextInt(3),
                            alpha = 0.95f
                        )
                    )
                }
            }
            com.example.scene.WeatherType.SAKURA -> {
                // Soft delicate pink sakura petals resting on the lawn
                val count = 24 + Random.nextInt(8)
                repeat(count) {
                    val nx = 0.04f + Random.nextFloat() * 0.92f
                    val ny = 0.70f + Random.nextFloat() * 0.20f
                    fallenParticles.add(
                        FallenParticle(
                            normX = nx,
                            normY = ny,
                            type = ParticleType.SAKURA_PETAL,
                            color = SAKURA_PETAL_COLORS.random(),
                            size = 3.0f + Random.nextFloat() * 1.4f,
                            styleVariant = Random.nextInt(3),
                            alpha = 0.95f
                        )
                    )
                }
            }
            com.example.scene.WeatherType.SNOW -> {
                // Quiet winter snow patches dusted across the grass
                val count = 28 + Random.nextInt(10)
                repeat(count) {
                    val nx = 0.04f + Random.nextFloat() * 0.92f
                    val ny = 0.70f + Random.nextFloat() * 0.18f
                    fallenParticles.add(
                        FallenParticle(
                            normX = nx,
                            normY = ny,
                            type = ParticleType.SNOWFLAKE,
                            color = SNOW_COLORS.random(),
                            size = 2.4f + Random.nextFloat() * 1.4f,
                            styleVariant = Random.nextInt(3),
                            alpha = 0.95f
                        )
                    )
                }
            }
            else -> {
                // SUNNY / RAIN: no persistent ground leaves/petals
            }
        }
    }


    fun landSeasonalParticle(p: PixelParticle, cw: Float, ch: Float) {
        val maxGround = when (p.type) {
            ParticleType.SNOWFLAKE -> 45
            else -> 35
        }
        val currentCount = fallenParticles.count { it.type == p.type && !it.isSwept }
        if (currentCount < maxGround) {
            val normX = (p.x / cw).coerceIn(0.04f, 0.96f)
            // Add a small deterministic Y jitter per particle so they do NOT all clamp
            // to exactly normY=0.67 (which caused a continuous horizontal line at the
            // grass horizon). The jitter is derived from the particle's X position so
            // no extra allocation occurs per frame.
            val yJitter = (Random.nextFloat() - 0.5f) * 0.035f
            val normY = (p.y / ch + yJitter).coerceIn(0.68f, 0.96f)

            // Avoid spawning directly on top of an already-landed particle of the same
            // type. Neighbourhood radius ≈ 1.5 particle widths in normalised coords.
            val tooClose = fallenParticles.any { fp ->
                !fp.isSwept && fp.type == p.type &&
                    kotlin.math.abs(fp.normX - normX) < 0.035f &&
                    kotlin.math.abs(fp.normY - normY) < 0.025f
            }
            if (!tooClose) {
                fallenParticles.add(
                    FallenParticle(
                        normX = normX,
                        normY = normY,
                        type = p.type,
                        color = p.color,
                        size = p.size,
                        styleVariant = Random.nextInt(3),
                        alpha = 0.95f
                    )
                )
            }
        }
    }

    fun sweepGroundParticles(
        touchX: Float,
        touchY: Float,
        cw: Float,
        ch: Float,
        radiusPx: Float,
        dragDeltaX: Float = 0f,
        dragDeltaY: Float = 0f
    ): Int {
        var sweptCount = 0
        val radiusSq = radiusPx * radiusPx
        for (i in fallenParticles.indices) {
            val fp = fallenParticles[i]
            if (fp.isSwept) continue
            // strictly cherry blossoms and leaves only! "not in case of rain and snow"
            if (fp.type != ParticleType.SAKURA_PETAL && fp.type != ParticleType.AUTUMN_LEAF) {
                continue
            }
            val px = fp.normX * cw
            val py = fp.normY * ch
            val dx = px - touchX
            val dy = py - touchY
            if (dx * dx + dy * dy <= radiusSq) {
                // Velocities are px/sec. A drag event is roughly one frame, so its delta x60 is
                // the finger's speed; a hard flick throws leaves clean off the screen.
                val fingerVx = (dragDeltaX * 60f * 0.35f).coerceIn(-cw * 1.4f, cw * 1.4f)
                tossFallenParticle(
                    fp, cw, ch,
                    vx = fingerVx + (Random.nextFloat() - 0.5f) * cw * 0.16f,
                    vy = -ch * (0.18f + Random.nextFloat() * 0.12f) + (dragDeltaY * 60f * 0.1f).coerceIn(-ch * 0.1f, ch * 0.1f)
                )
                sweptCount++
            }
        }
        return sweptCount
    }

    /** A gentle hop for leaves and petals near walking feet. Returns how many were kicked. */
    fun kickGroundParticles(footX: Float, footY: Float, cw: Float, ch: Float, radiusPx: Float, towardLeft: Boolean): Int {
        var kicked = 0
        for (i in fallenParticles.indices) {
            val fp = fallenParticles[i]
            if (fp.isSwept || fp.fadeRemaining >= 0f) continue
            if (fp.type != ParticleType.SAKURA_PETAL && fp.type != ParticleType.AUTUMN_LEAF) continue
            val dx = fp.normX * cw - footX
            val dy = (fp.normY * ch - footY) * 2.5f // a flat oval around the foot
            if (dx * dx + dy * dy > radiusPx * radiusPx) continue
            tossFallenParticle(
                fp, cw, ch,
                vx = (if (towardLeft) -1f else 1f) * cw * (0.03f + Random.nextFloat() * 0.05f),
                vy = -ch * (0.06f + Random.nextFloat() * 0.06f)
            )
            kicked++
        }
        return kicked
    }

    private fun tossFallenParticle(fp: FallenParticle, cw: Float, ch: Float, vx: Float, vy: Float) {
        fp.isSwept = true
        fp.sweptLife = 0f
        fp.sweptVx = vx
        fp.sweptVy = vy
        // It settles a little nearer or further on the ground band than where it rested.
        fp.restNormY = (fp.restNormY + (Random.nextFloat() - 0.5f) * 0.03f).coerceIn(0.68f, 0.96f)
    }

    /** Leaves a footprint, pawprint or finger trace in the snow, dropping the oldest when full. */
    fun addSnowPrint(normX: Float, normY: Float, kind: SnowPrintKind, facingLeft: Boolean) {
        if (snowPrints.size >= MAX_SNOW_PRINTS) snowPrints.removeAt(0)
        snowPrints.add(SnowPrint(normX, normY, kind, facingLeft))
    }

    /** The puddle under (x, y), if it holds any water. */
    fun puddleAt(normX: Float, normY: Float, cw: Float, ch: Float, unit: Float): Puddle? {
        for (puddle in puddles) {
            if (puddle.size < 0.2f) continue
            val halfW = puddleHalfWidth(unit, puddle.size)
            val dx = (normX - puddle.normX) * cw / halfW
            val dy = (normY - puddle.normY) * ch / (halfW * 0.38f)
            if (dx * dx + dy * dy <= 1f) return puddle
        }
        return null
    }

    fun puddleHalfWidth(unit: Float, size: Float): Float = unit * (5f + 9f * size)

    private fun updatePuddles(raining: Boolean, dt: Float) {
        for (puddle in puddles) {
            puddle.size = if (raining) {
                (puddle.size + dt / PUDDLE_FILL_SECONDS).coerceAtMost(1f)
            } else {
                (puddle.size - dt / PUDDLE_DRY_SECONDS).coerceAtLeast(0f)
            }
        }
    }

    fun update(deltaSeconds: Float, cw: Float = 1000f, ch: Float = 2000f) {
        val dt = deltaSeconds.coerceIn(0f, 0.05f)
        val dtFactor = dt * 60f
        var writeIndex = 0
        val size = particles.size
        for (i in 0 until size) {
            val p = particles[i]
            p.currentLife += dtFactor
            if (p.currentLife >= p.maxLife) {
                recycleParticle(p)
                continue
            }

            if (p.weatherFadeRemaining >= 0f) {
                p.weatherFadeRemaining = (p.weatherFadeRemaining - dt).coerceAtLeast(0f)
                if (p.weatherFadeRemaining <= 0f) {
                    recycleParticle(p)
                    continue
                }
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.alpha = p.weatherFadeStartAlpha * (p.weatherFadeRemaining / WEATHER_FADE_SECONDS)
                particles[writeIndex++] = p
                continue
            }

            val phaseRate = when (p.type) {
                ParticleType.SAKURA_PETAL -> 0.80f
                ParticleType.AUTUMN_LEAF -> 0.65f
                ParticleType.SNOWFLAKE -> 0.42f
                else -> 4f
            }
            p.phase += dt * phaseRate
            val lifeRatio = p.currentLife / p.maxLife

            when (p.type) {
                ParticleType.LEAF -> {
                    p.x += (sin(p.phase) * 1.6f + p.vx) * dtFactor
                    p.y += p.vy * dtFactor
                    p.alpha = (1f - (lifeRatio * 0.4f)).coerceIn(0f, 1f)
                }
                ParticleType.PETAL -> {
                    p.x += (sin(p.phase * 1.3f) * 2.2f + p.vx) * dtFactor
                    p.y += p.vy * dtFactor
                    p.alpha = (1f - (lifeRatio * 0.3f)).coerceIn(0f, 1f)
                }
                ParticleType.HEART -> {
                    p.y -= p.vy * dtFactor
                    p.x += sin(p.phase * 2f) * 0.9f * dtFactor
                    p.alpha = if (lifeRatio < 0.2f) {
                        lifeRatio / 0.2f
                    } else {
                        (1f - lifeRatio) * 1.25f
                    }.coerceIn(0f, 1f)
                }
                ParticleType.SLEEP_Z -> {
                    p.y -= p.vy * dtFactor
                    p.x += sin(p.phase * 1.2f) * 1.1f * dtFactor
                    p.alpha = (1f - lifeRatio).coerceIn(0f, 1f)
                }
                ParticleType.STEAM -> {
                    p.y -= p.vy * dtFactor
                    p.x += sin(p.phase * 1.5f) * 0.8f * dtFactor
                    p.alpha = (1f - lifeRatio).coerceIn(0f, 0.85f)
                    p.size += 0.06f * dtFactor
                }
                ParticleType.CHIMNEY_SMOKE -> {
                    p.y -= p.vy * dtFactor
                    p.x += (sin(p.phase) * 1.2f + p.vx) * dtFactor
                    p.alpha = (1f - lifeRatio).coerceIn(0f, 0.65f)
                    p.size += 0.12f * dtFactor
                }
                ParticleType.FIREFLY -> {
                    p.x += (sin(p.phase * 0.8f) * 1.2f + p.vx * 0.3f) * dtFactor
                    p.y += (sin(p.phase * 1.1f) * 0.9f + p.vy * 0.3f) * dtFactor
                    val glow = (sin(p.phase * 2.5f) * 0.45f + 0.55f).coerceIn(0.2f, 1f)
                    p.alpha = (glow * (1f - lifeRatio * 0.2f)).coerceIn(0f, 1f)
                }
                ParticleType.SHOOTING_STAR -> {
                    p.x += p.vx * dtFactor
                    p.y += p.vy * dtFactor
                    p.alpha = (1f - lifeRatio).coerceIn(0f, 1f)
                }
                ParticleType.STAR, ParticleType.SPARKLE -> {
                    p.x += p.vx * dtFactor
                    p.y += p.vy * dtFactor
                    p.alpha = (sin(p.phase * 3f) * 0.5f + 0.5f).coerceIn(0.2f, 1f)
                }
                ParticleType.WATER_RIPPLE, ParticleType.RAIN_SPLASH -> {
                    p.size += 0.4f * dtFactor
                    p.alpha = (1f - lifeRatio).coerceIn(0f, 0.8f)
                }
                ParticleType.MUSIC_NOTE -> {
                    p.y -= p.vy * dtFactor
                    p.x += sin(p.phase * 1.5f) * 1.0f * dtFactor
                    p.alpha = (1f - lifeRatio).coerceIn(0f, 1f)
                }
                ParticleType.RAIN_DROP -> {
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.alpha = if (p.depth < 0.88f) 0.56f else 0.86f
                    if (p.targetGroundY > 0f && p.y >= p.targetGroundY) {
                        spawnRainSplash(p.x, p.targetGroundY)
                        recycleParticle(p)
                        continue
                    }
                }
                ParticleType.SAKURA_PETAL -> {
                    // Steady vertical descent at explicit px/sec (no sinusoidal modulation on p.y)
                    p.y += p.vy * dt
                    // Romantic fluttering side-to-side sway strictly decoupled to p.x
                    val swayX = (sin(p.phase * 0.55f) * 0.012f + kotlin.math.cos(p.phase * 0.25f) * 0.005f) * cw * p.depth
                    p.x += (swayX + p.vx) * dt
                    p.alpha = (0.48f + p.depth * 0.34f).coerceIn(0f, 1f)
                    if (p.x < -30f) p.x = cw + 20f else if (p.x > cw + 30f) p.x = -20f
                    if (p.targetGroundY > 0f && p.y >= p.targetGroundY) {
                        landSeasonalParticle(p, cw, ch)
                        recycleParticle(p)
                        continue
                    }
                }
                ParticleType.AUTUMN_LEAF -> {
                    // Steady vertical descent at explicit px/sec (no sinusoidal modulation on p.y)
                    p.y += p.vy * dt
                    // Romantic tumbling autumn drift strictly decoupled to p.x
                    val swayX = (sin(p.phase * 0.45f) * 0.014f + kotlin.math.cos(p.phase * 0.20f) * 0.006f) * cw * p.depth
                    p.x += (swayX + p.vx) * dt
                    p.alpha = (0.48f + p.depth * 0.34f).coerceIn(0f, 1f)
                    if (p.x < -30f) p.x = cw + 20f else if (p.x > cw + 30f) p.x = -20f
                    if (p.targetGroundY > 0f && p.y >= p.targetGroundY) {
                        landSeasonalParticle(p, cw, ch)
                        recycleParticle(p)
                        continue
                    }
                }
                ParticleType.SNOWFLAKE -> {
                    // Steady vertical descent at explicit px/sec (no sinusoidal modulation on p.y)
                    p.y += p.vy * dt
                    // Peaceful lazy winter snow drift strictly decoupled to p.x
                    val swayX = (sin(p.phase * 0.35f) * 0.006f + kotlin.math.cos(p.phase * 0.18f) * 0.003f) * cw * p.depth
                    p.x += (swayX + p.vx) * dt
                    p.alpha = (0.48f + p.depth * 0.34f).coerceIn(0f, 1f)
                    if (p.x < -30f) p.x = cw + 20f else if (p.x > cw + 30f) p.x = -20f
                    if (p.targetGroundY > 0f && p.y >= p.targetGroundY) {
                        landSeasonalParticle(p, cw, ch)
                        recycleParticle(p)
                        continue
                    }
                }
                ParticleType.DANDELION_FLUFF -> {
                    p.x += (p.vx + sin(p.phase * 0.8f) * 1.0f) * dtFactor
                    p.y += (p.vy + sin(p.phase * 0.5f) * 0.6f) * dtFactor
                    p.alpha = (1f - (lifeRatio * 0.3f)).coerceIn(0f, 0.9f)
                }
                ParticleType.WIND_BREEZE -> {
                    p.x += p.vx * dtFactor
                    p.y += sin(p.phase * 0.4f) * 0.5f * dtFactor
                    p.alpha = (sin(lifeRatio * 3.1415f) * 0.45f).coerceIn(0f, 0.55f)
                }
            }
            particles[writeIndex++] = p
        }

        while (particles.size > writeIndex) {
            recycleParticle(particles.removeAt(particles.size - 1))
        }

        // Advance swept ground particles and fade them out (swipe = permanently remove)
        var fpWrite = 0
        for (i in fallenParticles.indices) {
            val fp = fallenParticles[i]
            if (fp.fadeRemaining >= 0f) {
                fp.fadeRemaining = (fp.fadeRemaining - dt).coerceAtLeast(0f)
                fp.alpha = minOf(fp.alpha, fp.fadeRemaining / WEATHER_FADE_SECONDS)
                if (fp.fadeRemaining <= 0f) continue
            }
            if (fp.isSwept) {
                fp.sweptLife += dt
                fp.sweptVy += ch * 0.55f * dt
                // Leaves and petals float down rather than drop.
                if (fp.sweptVy > ch * 0.10f) fp.sweptVy = ch * 0.10f
                fp.sweptVx *= (1f - 1.1f * dt).coerceAtLeast(0f)
                val flutter = sin(fp.sweptLife * 7f + fp.styleVariant * 2f) * cw * 0.05f
                fp.normX += (fp.sweptVx + flutter) * dt / cw
                fp.normY += fp.sweptVy * dt / ch
                if (fp.normX < -0.05f || fp.normX > 1.05f) {
                    continue // flicked clean off the screen
                }
                if (fp.sweptVy > 0f && fp.normY >= fp.restNormY) {
                    fp.normY = fp.restNormY
                    fp.isSwept = false
                    fp.sweptLife = 0f
                }
            }
            fallenParticles[fpWrite++] = fp
        }
        while (fallenParticles.size > fpWrite) {
            fallenParticles.removeAt(fallenParticles.size - 1)
        }

        var printWrite = 0
        for (i in snowPrints.indices) {
            val print = snowPrints[i]
            print.age += dt
            if (print.age < SNOW_PRINT_SECONDS) snowPrints[printWrite++] = print
        }
        while (snowPrints.size > printWrite) snowPrints.removeAt(snowPrints.size - 1)

        if (pendingParticles.isNotEmpty()) {
            particles.addAll(pendingParticles)
            pendingParticles.clear()
        }
    }

    /** A falling weather particle caught by a tap. */
    data class CaughtWeather(val type: ParticleType, val x: Float, val y: Float, val color: Color)

    /**
     * Catches the nearest falling snowflake, petal, leaf or dandelion puff within [radius]
     * of the tap; it is removed on the next update. Returns null if nothing was close enough.
     */
    fun catchWeatherParticleAt(x: Float, y: Float, radius: Float): CaughtWeather? {
        var best: PixelParticle? = null
        var bestDist = radius
        for (i in particles.indices) {
            val pt = particles[i]
            if (pt.type !in CATCHABLE_WEATHER || pt.currentLife >= pt.maxLife || pt.weatherFadeRemaining >= 0f) continue
            val d = kotlin.math.hypot(pt.x - x, pt.y - y)
            if (d < bestDist) {
                best = pt
                bestDist = d
            }
        }
        val caught = best ?: return null
        caught.currentLife = caught.maxLife
        return CaughtWeather(caught.type, caught.x, caught.y, caught.color)
    }

    fun spawnHeart(x: Float, y: Float, color: Color = Color(0xFFFF3366)) {
        particles.add(
            obtainParticle(
                x = x + Random.nextFloat() * 16f - 8f,
                y = y,
                vx = 0f,
                vy = 1.3f + Random.nextFloat() * 0.7f,
                size = 11f + Random.nextFloat() * 5f,
                color = color,
                maxLife = 75f + Random.nextFloat() * 25f,
                type = ParticleType.HEART,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnLeaf(x: Float, y: Float) {
        particles.add(
            obtainParticle(
                x = x + Random.nextFloat() * 40f - 20f,
                y = y,
                vx = 0.4f + Random.nextFloat() * 0.8f,
                vy = 0.9f + Random.nextFloat() * 0.8f,
                size = 6f + Random.nextFloat() * 3f,
                color = LEAF_COLORS[Random.nextInt(LEAF_COLORS.size)],
                maxLife = 130f + Random.nextFloat() * 50f,
                type = ParticleType.LEAF,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnPetals(x: Float, y: Float, count: Int = 5) {
        repeat(count) {
            particles.add(
                obtainParticle(
                    x = x + Random.nextFloat() * 30f - 15f,
                    y = y + Random.nextFloat() * 20f - 10f,
                    vx = (Random.nextFloat() - 0.5f) * 1.4f,
                    vy = 0.8f + Random.nextFloat() * 0.9f,
                    size = 5f + Random.nextFloat() * 3f,
                    color = PETAL_COLORS[Random.nextInt(PETAL_COLORS.size)],
                    maxLife = 90f + Random.nextFloat() * 40f,
                    type = ParticleType.PETAL,
                    phase = Random.nextFloat() * 6.28f
                )
            )
        }
    }

    fun spawnSleepZ(x: Float, y: Float) {
        particles.add(
            obtainParticle(
                x = x + Random.nextFloat() * 10f - 5f,
                y = y,
                vx = 0.3f,
                vy = 0.8f + Random.nextFloat() * 0.4f,
                size = 8f + Random.nextFloat() * 4f,
                color = Color(0xFFD6E2E9),
                maxLife = 95f,
                type = ParticleType.SLEEP_Z,
                text = if (Random.nextBoolean()) "Z" else "z",
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnSteam(x: Float, y: Float) {
        particles.add(
            obtainParticle(
                x = x + Random.nextFloat() * 12f - 6f,
                y = y,
                vx = 0f,
                vy = 0.9f + Random.nextFloat() * 0.4f,
                size = 6f + Random.nextFloat() * 3f,
                color = Color(0xD8FFFFFF),
                maxLife = 65f + Random.nextFloat() * 20f,
                type = ParticleType.STEAM,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnChimneySmoke(x: Float, y: Float) {
        val now = System.nanoTime()
        if (now - lastChimneySmokeNanos < 1_050_000_000L) return
        lastChimneySmokeNanos = now
        particles.add(
            obtainParticle(
                x = x + Random.nextFloat() * 6f - 3f,
                y = y,
                vx = 0.3f + Random.nextFloat() * 0.3f,
                vy = 0.7f + Random.nextFloat() * 0.3f,
                size = 7f + Random.nextFloat() * 4f,
                color = Color(0x99ECEFE6),
                maxLife = 110f,
                type = ParticleType.CHIMNEY_SMOKE,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnFirefly(x: Float, y: Float) {
        particles.add(
            obtainParticle(
                x = x + Random.nextFloat() * 30f - 15f,
                y = y + Random.nextFloat() * 30f - 15f,
                vx = (Random.nextFloat() - 0.5f) * 0.6f,
                vy = (Random.nextFloat() - 0.5f) * 0.5f,
                size = 3.5f + Random.nextFloat() * 2f,
                color = Color(0xFFD8F3DC),
                maxLife = 220f + Random.nextFloat() * 80f,
                type = ParticleType.FIREFLY,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnShootingStar(startX: Float, startY: Float) {
        particles.add(
            obtainParticle(
                x = startX,
                y = startY,
                vx = 8f + Random.nextFloat() * 4f,
                vy = 4f + Random.nextFloat() * 2f,
                size = 4f,
                color = Color(0xFFFFF9DB),
                maxLife = 35f,
                type = ParticleType.SHOOTING_STAR,
                phase = 0f
            )
        )
    }

    fun spawnSparkles(x: Float, y: Float, count: Int = 6, color: Color? = null) {
        repeat(count) {
            particles.add(
                obtainParticle(
                    x = x + Random.nextFloat() * 24f - 12f,
                    y = y + Random.nextFloat() * 24f - 12f,
                    vx = (Random.nextFloat() - 0.5f) * 1.2f,
                    vy = (Random.nextFloat() - 0.5f) * 1.2f,
                    size = 4f + Random.nextFloat() * 3f,
                    color = color ?: if (Random.nextBoolean()) Color(0xFFFFE66D) else Color(0xFFFFF0F3),
                    maxLife = 45f + Random.nextFloat() * 25f,
                    type = ParticleType.SPARKLE,
                    phase = Random.nextFloat() * 6.28f
                )
            )
        }
    }

    fun spawnMusicNote(x: Float, y: Float) {
        particles.add(
            obtainParticle(
                x = x + Random.nextFloat() * 16f - 8f,
                y = y,
                vx = (Random.nextFloat() - 0.5f) * 0.4f,
                vy = 0.8f + Random.nextFloat() * 0.4f,
                size = 5f + Random.nextFloat() * 2f,
                color = MUSIC_NOTE_COLORS.random(),
                maxLife = 70f + Random.nextFloat() * 20f,
                type = ParticleType.MUSIC_NOTE,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnGrassPuff(x: Float, y: Float, count: Int = 4) {
        repeat(count) {
            particles.add(
                obtainParticle(
                    x = x + Random.nextFloat() * 16f - 8f,
                    y = y + Random.nextFloat() * 8f - 4f,
                    vx = (Random.nextFloat() - 0.5f) * 1.5f,
                    vy = -(0.8f + Random.nextFloat() * 1.2f),
                    size = 3f + Random.nextFloat() * 2.5f,
                    color = GRASS_COLORS.random(),
                    maxLife = 35f + Random.nextFloat() * 15f,
                    type = ParticleType.SPARKLE,
                    phase = Random.nextFloat() * 6.28f
                )
            )
        }
    }

    fun spawnDandelionFluff(x: Float, y: Float) {
        particles.add(
            obtainParticle(
                x = x,
                y = y,
                vx = 0.8f + Random.nextFloat() * 0.6f,
                vy = (Random.nextFloat() - 0.45f) * 0.25f,
                size = 3.5f + Random.nextFloat() * 1.5f,
                color = Color(0xFFF8F9FA),
                maxLife = 300f + Random.nextFloat() * 120f,
                type = ParticleType.DANDELION_FLUFF,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnWindBreezeStreak(x: Float, y: Float, width: Float = 1000f) {
        particles.add(
            obtainParticle(
                x = x,
                y = y,
                vx = 3f + Random.nextFloat() * 1.5f,
                vy = 0f,
                size = 1.8f,
                color = Color(0x66FFFFFF),
                maxLife = 110f + Random.nextFloat() * 50f,
                type = ParticleType.WIND_BREEZE,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnRainDrop(cw: Float, ch: Float, groundY: Float = ch * 0.84f) {
        val rx = Random.nextFloat() * (cw + 120f) - 40f
        val depth = 0.62f + Random.nextFloat() * 0.76f
        val isBackgroundDrizzle = depth < 0.88f
        val baseSpeedPxPerSec = ch / RAIN_TRAVERSAL_SECONDS
        val rainSpeed = baseSpeedPxPerSec * depth * (0.94f + Random.nextFloat() * 0.12f)
        val rainSize = (if (isBackgroundDrizzle) 7f else 12f) + depth * 5f + Random.nextFloat() * 3f
        val rainColor = if (isBackgroundDrizzle) Color(0x65BAE6FD) else Color(0xC8E0F2FE)
        val startY = -Random.nextFloat() * ch * 0.42f - 12f
        val lifeFrames = (((groundY - startY) / rainSpeed) + 1.5f) * 60f
        particles.add(
            obtainParticle(
                x = rx,
                y = startY,
                vx = (if (Random.nextBoolean()) -1f else 1f) * cw * (0.018f + Random.nextFloat() * 0.035f) * depth,
                vy = rainSpeed,
                size = rainSize,
                color = rainColor,
                maxLife = lifeFrames,
                type = ParticleType.RAIN_DROP,
                phase = 0f,
                targetGroundY = groundY,
                alpha = if (isBackgroundDrizzle) 0.56f else 0.86f,
                depth = depth
            )
        )
    }

    fun spawnRainSplash(x: Float, y: Float) {
        pendingParticles.add(
            obtainParticle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                size = 3.5f,
                color = Color(0xCCE0F2FE),
                maxLife = 22f,
                type = ParticleType.RAIN_SPLASH,
                phase = 0f
            )
        )
        if (Random.nextFloat() < 0.40f) {
            pendingParticles.add(
                obtainParticle(
                    x = x,
                    y = y,
                    vx = 0f,
                    vy = 0f,
                    size = 4.5f,
                    color = Color(0x80BAE6FD),
                    maxLife = 26f,
                    type = ParticleType.WATER_RIPPLE,
                    phase = 0f
                )
            )
        }
    }

    fun spawnSakuraPetal(cw: Float, ch: Float, startY: Float? = null) {
        val targetGroundY = ch * (0.70f + Random.nextFloat() * 0.20f)
        val yPos = startY ?: entryY(ch)
        val depth = 0.62f + Random.nextFloat() * 0.72f
        val fallSpeed = (ch / SAKURA_TRAVERSAL_SECONDS) * depth * (0.91f + Random.nextFloat() * 0.18f)
        val lifeFrames = (((targetGroundY - yPos).coerceAtLeast(ch * 0.08f) / fallSpeed) + 2f) * 60f
        particles.add(
            obtainParticle(
                x = Random.nextFloat() * (cw + 60f) - 30f,
                y = yPos,
                vx = (Random.nextFloat() - 0.5f) * 0.018f * cw * depth,
                vy = fallSpeed,
                size = 2.6f + depth * 1.3f + Random.nextFloat() * 0.9f,
                color = SAKURA_PETAL_COLORS.random(),
                maxLife = lifeFrames,
                type = ParticleType.SAKURA_PETAL,
                phase = Random.nextFloat() * 6.28f,
                targetGroundY = targetGroundY,
                alpha = 0.48f + depth * 0.34f,
                depth = depth
            )
        )
    }

    fun spawnAutumnLeaf(cw: Float, ch: Float, startY: Float? = null) {
        val targetGroundY = ch * (0.70f + Random.nextFloat() * 0.20f)
        val yPos = startY ?: entryY(ch)
        val depth = 0.64f + Random.nextFloat() * 0.70f
        val fallSpeed = (ch / AUTUMN_TRAVERSAL_SECONDS) * depth * (0.90f + Random.nextFloat() * 0.20f)
        val lifeFrames = (((targetGroundY - yPos).coerceAtLeast(ch * 0.08f) / fallSpeed) + 2f) * 60f
        particles.add(
            obtainParticle(
                x = Random.nextFloat() * (cw + 60f) - 30f,
                y = yPos,
                vx = (Random.nextFloat() - 0.46f) * 0.015f * cw * depth,
                vy = fallSpeed,
                size = 2.9f + depth * 1.4f + Random.nextFloat() * 0.9f,
                color = AUTUMN_LEAF_COLORS.random(),
                maxLife = lifeFrames,
                type = ParticleType.AUTUMN_LEAF,
                phase = Random.nextFloat() * 6.28f,
                targetGroundY = targetGroundY,
                alpha = 0.48f + depth * 0.34f,
                depth = depth
            )
        )
    }

    fun spawnSnowflake(cw: Float, ch: Float, startY: Float? = null) {
        val targetGroundY = ch * (0.70f + Random.nextFloat() * 0.18f)
        val yPos = startY ?: entryY(ch)
        val depth = 0.52f + Random.nextFloat() * 0.78f
        val fallSpeed = (ch / SNOW_TRAVERSAL_SECONDS) * depth * (0.88f + Random.nextFloat() * 0.24f)
        val lifeFrames = (((targetGroundY - yPos).coerceAtLeast(ch * 0.08f) / fallSpeed) + 2f) * 60f
        particles.add(
            obtainParticle(
                x = Random.nextFloat() * (cw + 60f) - 30f,
                y = yPos,
                vx = (Random.nextFloat() - 0.5f) * 0.008f * cw * depth,
                vy = fallSpeed,
                size = 1.8f + depth * 1.4f + Random.nextFloat() * 0.9f,
                color = SNOW_COLORS.random(),
                maxLife = lifeFrames,
                type = ParticleType.SNOWFLAKE,
                phase = Random.nextFloat() * 6.28f,
                targetGroundY = targetGroundY,
                alpha = 0.48f + depth * 0.34f,
                depth = depth
            )
        )
    }

    fun spawnSunSparkle(x: Float, y: Float) {
        particles.add(
            obtainParticle(
                x = x,
                y = y,
                vx = (Random.nextFloat() - 0.3f) * 0.6f,
                vy = (Random.nextFloat() - 0.6f) * 0.5f,
                size = 3.0f + Random.nextFloat() * 2.5f,
                color = SUN_SPARKLE_COLORS.random(),
                maxLife = 150f + Random.nextFloat() * 60f,
                type = ParticleType.SPARKLE,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun updateWeatherEffects(
        weather: com.example.scene.WeatherType,
        cw: Float,
        ch: Float,
        isOutdoor: Boolean,
        isNight: Boolean = false,
        deltaSeconds: Float = 0.016f,
        rainGroundY: Float = ch * 0.84f
    ) {
        val dt = deltaSeconds.coerceIn(0f, 0.05f)
        // Cap applies indoors too, so rapid prop taps cannot pile up particles.
        if (particles.size > 400) {
            val removeCount = particles.size - 400
            for (i in 0 until removeCount) {
                recycleParticle(particles[i])
            }
            particles.subList(0, removeCount).clear()
        }
        // Weather-specific particles belong to the weather that created them. Clear them
        // together at a transition so old rain/petals cannot drift into a new climate.
        updatePuddles(raining = isOutdoor && weather == com.example.scene.WeatherType.RAIN, dt = dt)
        if (!isOutdoor) {
            if (activeWeather != null) clearSeasonalParticles()
            activeWeather = null
            rainSpawnAccumulator = 0f
            seasonalSpawnAccumulator = 0f
            return
        }
        if (activeWeather != weather) {
            clearSeasonalParticles()
            activeWeather = weather
            rainSpawnAccumulator = 0f
            seasonalSpawnAccumulator = 0f
            // Start with the sky already full, spread from the top down to the ground.
            val prefill = when (weather) {
                com.example.scene.WeatherType.SAKURA -> 45
                com.example.scene.WeatherType.AUTUMN -> 32
                com.example.scene.WeatherType.SNOW -> 80
                else -> 0
            }
            repeat(prefill) {
                val y = Random.nextFloat() * ch * 0.72f
                when (weather) {
                    com.example.scene.WeatherType.SAKURA -> spawnSakuraPetal(cw, ch, startY = y)
                    com.example.scene.WeatherType.AUTUMN -> spawnAutumnLeaf(cw, ch, startY = y)
                    com.example.scene.WeatherType.SNOW -> spawnSnowflake(cw, ch, startY = y)
                    else -> Unit
                }
            }
        }

        // Track weather change so stale ground particles of the old type are removed.
        // Do NOT pre-seed the ground — let it accumulate naturally from falling particles.
        groundSeededWeather = weather

        when (weather) {
            com.example.scene.WeatherType.RAIN -> {
                // Steady rain rate with a bounded catch-up so a delayed frame never bursts.
                rainSpawnAccumulator += dt * 260f
                val count = rainSpawnAccumulator.toInt().coerceAtMost(14)
                if (count > 0) {
                    rainSpawnAccumulator -= count
                    repeat(count) { spawnRainDrop(cw, ch, rainGroundY) }
                }
            }
            com.example.scene.WeatherType.SAKURA -> {
                // Cherry Blossom: slow, gentle, romantic drift
                val count = countWeatherParticles(ParticleType.SAKURA_PETAL)
                seasonalSpawnAccumulator = spawnSeasonalWeather(
                    seasonalSpawnAccumulator, count, 110, 18f, dt, cw, ch
                )
            }
            com.example.scene.WeatherType.AUTUMN -> {
                // Autumn: slow, gentle, romantic falling leaves
                val count = countWeatherParticles(ParticleType.AUTUMN_LEAF)
                seasonalSpawnAccumulator = spawnSeasonalWeather(
                    seasonalSpawnAccumulator, count, 80, 14f, dt, cw, ch
                )
            }
            com.example.scene.WeatherType.SNOW -> {
                // Winter: slow, quiet, peaceful snowfall
                val count = countWeatherParticles(ParticleType.SNOWFLAKE)
                seasonalSpawnAccumulator = spawnSeasonalWeather(
                    seasonalSpawnAccumulator, count, 170, 26f, dt, cw, ch
                )
            }
            com.example.scene.WeatherType.SUNNY -> {
                // A bounded rate accumulator keeps the sunny ambience present in short
                // sessions and consistent across frame rates without bursty catch-up.
                seasonalSpawnAccumulator += dt * 0.58f
                if (seasonalSpawnAccumulator >= 1f) {
                    seasonalSpawnAccumulator -= 1f
                    when (Random.nextInt(if (isNight) 2 else 3)) {
                        0 -> spawnDandelionFluff(-10f, ch * (0.35f + Random.nextFloat() * 0.45f))
                        1 -> spawnWindBreezeStreak(-40f, ch * (0.45f + Random.nextFloat() * 0.35f), cw)
                        else -> spawnSunSparkle(cw * Random.nextFloat(), ch * (0.25f + Random.nextFloat() * 0.50f))
                    }
                }
            }
        }
    }

    private fun countWeatherParticles(type: ParticleType): Int {
        var count = 0
        for (particle in particles) if (particle.type == type) count++
        return count
    }

    private fun spawnSeasonalWeather(
        accumulator: Float,
        count: Int,
        cap: Int,
        perSecond: Float,
        deltaSeconds: Float,
        cw: Float,
        ch: Float
    ): Float {
        if (count >= cap) return accumulator.coerceAtMost(0.99f)
        var next = accumulator + deltaSeconds.coerceAtMost(0.05f) * perSecond
        while (next >= 1f && count < cap) {
            when (activeWeather) {
                com.example.scene.WeatherType.SAKURA -> spawnSakuraPetal(cw, ch)
                com.example.scene.WeatherType.AUTUMN -> spawnAutumnLeaf(cw, ch)
                com.example.scene.WeatherType.SNOW -> spawnSnowflake(cw, ch)
                else -> break
            }
            next -= 1f
        }
        return next
    }

    fun logSpeedAudit(weather: com.example.scene.WeatherType, ch: Float) {
        val targetType = when (weather) {
            com.example.scene.WeatherType.RAIN -> ParticleType.RAIN_DROP
            com.example.scene.WeatherType.SAKURA -> ParticleType.SAKURA_PETAL
            com.example.scene.WeatherType.AUTUMN -> ParticleType.AUTUMN_LEAF
            com.example.scene.WeatherType.SNOW -> ParticleType.SNOWFLAKE
            else -> return
        }
        val speeds = particles.filter { it.type == targetType }.map { it.vy }
        if (speeds.isNotEmpty()) {
            val min = speeds.minOrNull() ?: 0f
            val max = speeds.maxOrNull() ?: 0f
            val avg = speeds.average().toFloat()
            val samples = speeds.take(6).joinToString(", ") { "%.1f px/s".format(it) }
            Log.i(
                "ParticleSpeedAudit",
                "[$weather] Count: ${speeds.size} | Min: %.1f px/s | Max: %.1f px/s | Avg: %.1f px/s | Traversal: %.2fs | Samples: [$samples]".format(
                    min, max, avg, ch / avg
                )
            )
        }
    }
}
