package com.example.engine

import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
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
    var targetGroundY: Float = 0f
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
    var sweptLife: Float = 0f
)

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

class ParticleSystem {
    val particles = mutableListOf<PixelParticle>()
    private val pendingParticles = mutableListOf<PixelParticle>()
    val fallenParticles = mutableListOf<FallenParticle>()
    var groundSeededWeather: com.example.scene.WeatherType? = null

    private val particlePool = ArrayList<PixelParticle>(256)
    private var lastChimneySmokeNanos = 0L
    private var rainSpawnAccumulator = 0f
    private var speedAuditTimer = 0f

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
        alpha: Float = 1f
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
            targetGroundY = targetGroundY
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
                p.type == ParticleType.DANDELION_FLUFF ||
                p.type == ParticleType.WIND_BREEZE
            if (isSeasonal) {
                recycleParticle(p)
            } else {
                particles[write++] = p
            }
        }
        while (particles.size > write) {
            particles.removeAt(particles.size - 1)
        }
        fallenParticles.clear()
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
            val yJitter = ((normX * 5003f).toLong() % 13) / 13f * 0.04f - 0.02f
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
                fp.isSwept = true
                val angle = Random.nextFloat() * 6.28f
                val speed = 2.0f + Random.nextFloat() * 2.5f
                fp.sweptVx = dragDeltaX * 0.35f + kotlin.math.cos(angle) * speed
                fp.sweptVy = -3.5f - Random.nextFloat() * 3.0f + dragDeltaY * 0.2f
                sweptCount++
            }
        }
        return sweptCount
    }

    fun update(deltaSeconds: Float, cw: Float = 1000f, ch: Float = 2000f) {
        val dtFactor = deltaSeconds * 60f
        var writeIndex = 0
        val size = particles.size
        for (i in 0 until size) {
            val p = particles[i]
            p.currentLife += dtFactor
            if (p.currentLife >= p.maxLife) {
                recycleParticle(p)
                continue
            }

            p.phase += deltaSeconds * 4f
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
                    p.x += p.vx * deltaSeconds
                    p.y += p.vy * deltaSeconds
                    p.alpha = 0.85f
                    if (p.targetGroundY > 0f && p.y >= p.targetGroundY) {
                        spawnRainSplash(p.x, p.targetGroundY)
                        recycleParticle(p)
                        continue
                    }
                }
                ParticleType.SAKURA_PETAL -> {
                    // Steady vertical descent at explicit px/sec (no sinusoidal modulation on p.y)
                    p.y += p.vy * deltaSeconds
                    // Romantic fluttering side-to-side sway strictly decoupled to p.x
                    val swayX = (sin(p.phase * 0.55f) * 55f + kotlin.math.cos(p.phase * 0.25f) * 25f) * (cw / 1000f).coerceIn(0.8f, 1.8f)
                    p.x += (swayX + p.vx) * deltaSeconds
                    p.alpha = 0.95f
                    if (p.x < -30f) p.x = cw + 20f else if (p.x > cw + 30f) p.x = -20f
                    if (p.targetGroundY > 0f && p.y >= p.targetGroundY) {
                        landSeasonalParticle(p, cw, ch)
                        recycleParticle(p)
                        continue
                    }
                }
                ParticleType.AUTUMN_LEAF -> {
                    // Steady vertical descent at explicit px/sec (no sinusoidal modulation on p.y)
                    p.y += p.vy * deltaSeconds
                    // Romantic tumbling autumn drift strictly decoupled to p.x
                    val swayX = (sin(p.phase * 0.45f) * 65f + kotlin.math.cos(p.phase * 0.20f) * 30f) * (cw / 1000f).coerceIn(0.8f, 1.8f)
                    p.x += (swayX + p.vx) * deltaSeconds
                    p.alpha = 0.95f
                    if (p.x < -30f) p.x = cw + 20f else if (p.x > cw + 30f) p.x = -20f
                    if (p.targetGroundY > 0f && p.y >= p.targetGroundY) {
                        landSeasonalParticle(p, cw, ch)
                        recycleParticle(p)
                        continue
                    }
                }
                ParticleType.SNOWFLAKE -> {
                    // Steady vertical descent at explicit px/sec (no sinusoidal modulation on p.y)
                    p.y += p.vy * deltaSeconds
                    // Peaceful lazy winter snow drift strictly decoupled to p.x
                    val swayX = (sin(p.phase * 0.35f) * 30f + kotlin.math.cos(p.phase * 0.18f) * 15f) * (cw / 1000f).coerceIn(0.8f, 1.8f)
                    p.x += (swayX + p.vx) * deltaSeconds
                    p.alpha = 0.95f
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
            if (fp.isSwept) {
                fp.sweptLife += deltaSeconds * 2.8f
                fp.normX += (fp.sweptVx / cw) * dtFactor
                fp.normY += (fp.sweptVy / ch) * dtFactor
                fp.sweptVy += 5.5f * (ch / 1000f) * deltaSeconds
                fp.alpha = (1f - fp.sweptLife).coerceIn(0f, 1f)
                if (fp.sweptLife >= 1f || fp.alpha <= 0.02f) {
                    continue // permanently removed — swipe gesture cleared it
                }
            }
            fallenParticles[fpWrite++] = fp
        }
        while (fallenParticles.size > fpWrite) {
            fallenParticles.removeAt(fallenParticles.size - 1)
        }

        if (pendingParticles.isNotEmpty()) {
            particles.addAll(pendingParticles)
            pendingParticles.clear()
        }
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
        if (now - lastChimneySmokeNanos < 250_000_000L) return
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
                vx = 1.4f + Random.nextFloat() * 1.8f,
                vy = (Random.nextFloat() - 0.45f) * 0.4f,
                size = 3.5f + Random.nextFloat() * 1.5f,
                color = Color(0xFFF8F9FA),
                maxLife = 120f + Random.nextFloat() * 60f,
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
                vx = 6f + Random.nextFloat() * 3f,
                vy = 0f,
                size = 1.8f,
                color = Color(0x66FFFFFF),
                maxLife = 50f + Random.nextFloat() * 20f,
                type = ParticleType.WIND_BREEZE,
                phase = Random.nextFloat() * 6.28f
            )
        )
    }

    fun spawnRainDrop(cw: Float, ch: Float) {
        val rx = Random.nextFloat() * (cw + 120f) - 40f
        val groundY = ch * (0.64f + Random.nextFloat() * 0.32f)
        // Explicit pixels-per-second: target traversal 1.5s - 2.0s (avg 1.75s) with tight +/-12% variation
        val baseSpeedPxPerSec = ch / 1.75f
        val rainSpeed = baseSpeedPxPerSec * (1.0f + (Random.nextFloat() - 0.5f) * 0.24f)
        particles.add(
            obtainParticle(
                x = rx,
                y = -25f,
                vx = -0.14f * cw, // Steady diagonal slant decoupled from fall speed
                vy = rainSpeed,
                size = 14f + Random.nextFloat() * 6f, // Rain streak length
                color = Color(0xB8BAE6FD),
                maxLife = 180f,
                type = ParticleType.RAIN_DROP,
                phase = 0f,
                targetGroundY = groundY
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
                maxLife = 14f,
                type = ParticleType.RAIN_SPLASH,
                phase = 0f
            )
        )
        repeat(2) { idx ->
            val dir = if (idx == 0) -1.2f else 1.2f
            pendingParticles.add(
                obtainParticle(
                    x = x,
                    y = y,
                    vx = dir * (0.6f + Random.nextFloat() * 0.8f),
                    vy = -(2.0f + Random.nextFloat() * 1.5f),
                    size = 2.0f,
                    color = Color(0xDDE0F2FE),
                    maxLife = 12f,
                    type = ParticleType.SPARKLE,
                    phase = 0f
                )
            )
        }
    }

    fun spawnSakuraPetal(cw: Float, ch: Float, startY: Float? = null) {
        val targetGroundY = ch * (0.70f + Random.nextFloat() * 0.20f)
        val yPos = startY ?: (-15f - Random.nextFloat() * 25f)
        // Explicit pixels-per-second: target traversal 13s-16s (avg 14.5s), ±8% variation.
        // Very slow, dreamy, romantic drift.
        val baseSpeedPxPerSec = ch / 14.5f
        val fallSpeed = baseSpeedPxPerSec * (1.0f + (Random.nextFloat() - 0.5f) * 0.16f)
        particles.add(
            obtainParticle(
                x = Random.nextFloat() * (cw + 60f) - 30f,
                y = yPos,
                vx = 0.025f * cw, // Steady slight ambient breeze
                vy = fallSpeed,
                size = 3.2f + Random.nextFloat() * 1.4f,
                color = SAKURA_PETAL_COLORS.random(),
                maxLife = 3600f,
                type = ParticleType.SAKURA_PETAL,
                phase = Random.nextFloat() * 6.28f,
                targetGroundY = targetGroundY
            )
        )
    }

    fun spawnAutumnLeaf(cw: Float, ch: Float, startY: Float? = null) {
        val targetGroundY = ch * (0.70f + Random.nextFloat() * 0.20f)
        val yPos = startY ?: (-15f - Random.nextFloat() * 25f)
        // Explicit pixels-per-second: target traversal 12s-15s (avg 13.0s), ±8% variation.
        // Slightly faster than sakura (heavier leaves), still slow and romantic.
        val baseSpeedPxPerSec = ch / 13.0f
        val fallSpeed = baseSpeedPxPerSec * (1.0f + (Random.nextFloat() - 0.5f) * 0.16f)
        particles.add(
            obtainParticle(
                x = Random.nextFloat() * (cw + 60f) - 30f,
                y = yPos,
                vx = 0.030f * cw, // Steady slight ambient breeze
                vy = fallSpeed,
                size = 3.4f + Random.nextFloat() * 1.5f,
                color = AUTUMN_LEAF_COLORS.random(),
                maxLife = 3600f,
                type = ParticleType.AUTUMN_LEAF,
                phase = Random.nextFloat() * 6.28f,
                targetGroundY = targetGroundY
            )
        )
    }

    fun spawnSnowflake(cw: Float, ch: Float, startY: Float? = null) {
        val targetGroundY = ch * (0.70f + Random.nextFloat() * 0.18f)
        val yPos = startY ?: (-15f - Random.nextFloat() * 25f)
        // Explicit pixels-per-second: target traversal 15s-18s (avg 16.5s), ±8% variation.
        // Slowest — snow floats almost weightlessly.
        val baseSpeedPxPerSec = ch / 16.5f
        val fallSpeed = baseSpeedPxPerSec * (1.0f + (Random.nextFloat() - 0.5f) * 0.16f)
        particles.add(
            obtainParticle(
                x = Random.nextFloat() * (cw + 60f) - 30f,
                y = yPos,
                vx = 0f,
                vy = fallSpeed,
                size = if (Random.nextFloat() < 0.40f) 3.2f + Random.nextFloat() * 1.0f else 2.0f + Random.nextFloat() * 0.8f,
                color = SNOW_COLORS.random(),
                maxLife = 3600f,
                type = ParticleType.SNOWFLAKE,
                phase = Random.nextFloat() * 6.28f,
                targetGroundY = targetGroundY
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
                maxLife = 90f + Random.nextFloat() * 40f,
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
        deltaSeconds: Float = 0.016f
    ) {
        if (!isOutdoor) return
        if (particles.size > 400) {
            val removeCount = particles.size - 400
            for (i in 0 until removeCount) {
                recycleParticle(particles[i])
            }
            particles.subList(0, removeCount).clear()
        }

        // Track weather change so stale ground particles of the old type are removed.
        // Do NOT pre-seed the ground — let it accumulate naturally from falling particles.
        if (groundSeededWeather != weather) {
            groundSeededWeather = weather
            // Remove ground particles that belong to the previous weather type
            var fw = 0
            for (i in fallenParticles.indices) {
                val fp = fallenParticles[i]
                val keep = when (weather) {
                    com.example.scene.WeatherType.SAKURA -> fp.type == ParticleType.SAKURA_PETAL
                    com.example.scene.WeatherType.AUTUMN -> fp.type == ParticleType.AUTUMN_LEAF
                    com.example.scene.WeatherType.SNOW   -> fp.type == ParticleType.SNOWFLAKE
                    else -> false // RAIN/SUNNY: no ground particles
                }
                if (keep) fallenParticles[fw++] = fp
            }
            while (fallenParticles.size > fw) fallenParticles.removeAt(fallenParticles.size - 1)
        }

        when (weather) {
            com.example.scene.WeatherType.RAIN -> {
                // Soft, cozy, romantic rain shower rate-limited to 130 drops/sec
                rainSpawnAccumulator += deltaSeconds * 130f
                val count = rainSpawnAccumulator.toInt().coerceAtMost(5)
                if (count > 0) {
                    rainSpawnAccumulator -= count
                    repeat(count) { spawnRainDrop(cw, ch) }
                }
            }
            com.example.scene.WeatherType.SAKURA -> {
                // Cherry Blossom: slow, gentle, romantic drift
                var sakuraCount = 0
                for (idx in 0 until particles.size) {
                    if (particles[idx].type == ParticleType.SAKURA_PETAL) sakuraCount++
                }
                if (sakuraCount == 0) {
                    // Seed initial petals distributed across the full screen height — immediately visible
                    repeat(34) { i ->
                        spawnSakuraPetal(cw, ch, ch * (0.02f + i * 0.028f))
                    }
                } else if (sakuraCount < 55 && Random.nextFloat() < (0.25f * deltaSeconds * 60f)) {
                    spawnSakuraPetal(cw, ch)
                }
            }
            com.example.scene.WeatherType.AUTUMN -> {
                // Autumn: slow, gentle, romantic falling leaves
                var leafCount = 0
                for (idx in 0 until particles.size) {
                    if (particles[idx].type == ParticleType.AUTUMN_LEAF) leafCount++
                }
                if (leafCount == 0) {
                    // Seed initial leaves distributed across the full screen height — immediately visible
                    repeat(28) { i ->
                        spawnAutumnLeaf(cw, ch, ch * (0.02f + i * 0.032f))
                    }
                } else if (leafCount < 45 && Random.nextFloat() < (0.22f * deltaSeconds * 60f)) {
                    spawnAutumnLeaf(cw, ch)
                }
            }
            com.example.scene.WeatherType.SNOW -> {
                // Winter: slow, quiet, peaceful snowfall
                var snowCount = 0
                for (idx in 0 until particles.size) {
                    if (particles[idx].type == ParticleType.SNOWFLAKE) snowCount++
                }
                if (snowCount == 0) {
                    // Seed initial snowflakes distributed across full screen height — immediately visible
                    repeat(50) { i ->
                        spawnSnowflake(cw, ch, ch * (0.01f + i * 0.018f))
                    }
                } else if (snowCount < 75 && Random.nextFloat() < (0.35f * deltaSeconds * 60f)) {
                    spawnSnowflake(cw, ch)
                }
            }
            com.example.scene.WeatherType.SUNNY -> {
                // Summer season — UNTOUCHED as requested!
                if (Random.nextFloat() < 0.12f) {
                    spawnDandelionFluff(-10f, ch * (0.35f + Random.nextFloat() * 0.45f))
                }
                // Only spawn sun sparkles during the day, never at night
                if (!isNight && Random.nextFloat() < 0.10f) {
                    spawnSunSparkle(cw * Random.nextFloat(), ch * (0.25f + Random.nextFloat() * 0.50f))
                }
                if (Random.nextFloat() < 0.04f) {
                    spawnWindBreezeStreak(-40f, ch * (0.45f + Random.nextFloat() * 0.35f), cw)
                }
            }
        }

        if (weather != com.example.scene.WeatherType.SUNNY) {
            speedAuditTimer += deltaSeconds
            if (speedAuditTimer >= 2.0f) {
                speedAuditTimer = 0f
                logSpeedAudit(weather, ch)
            }
        }
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
