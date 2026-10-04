package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.EmoteType
import com.example.engine.ParticleSystem
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import com.example.ui.theme.BlushPink
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepRose
import com.example.ui.theme.PeachMuted
import com.example.ui.theme.SoftRose
import com.example.ui.theme.TinyColors
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val audio = remember { AmbientAudio(context.applicationContext).apply { isEnabled = prefs.soundEnabled } }
    DisposableEffect(audio) {
        onDispose {
            audio.release()
        }
    }
    val particles = remember { ParticleSystem() }

    val boy = remember {
        PixelCharacter(
            isGirl = false,
            name = prefs.boyfriendName,
            worldX = 0.40f,
            worldY = 0.62f,
            pose = CharacterPose.IDLE,
            direction = Direction.RIGHT,
            emote = EmoteType.HEART,
            emoteTimer = 99f,
            look = com.example.engine.AvatarLook.of(prefs.getAvatarAppearance(isSlotB = false))
        )
    }

    val girl = remember {
        PixelCharacter(
            isGirl = true,
            name = prefs.girlfriendName,
            worldX = 0.60f,
            worldY = 0.62f,
            pose = CharacterPose.IDLE,
            direction = Direction.LEFT,
            emote = EmoteType.BLUSH,
            emoteTimer = 99f,
            look = com.example.engine.AvatarLook.of(prefs.getAvatarAppearance(isSlotB = true))
        )
    }

    // Heartbeat pulsating animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    // Gentle vertical bob for characters
    val charBob by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "charBob"
    )

    // Sound chime and auto-advance timer
    LaunchedEffect(Unit) {
        if (prefs.soundEnabled) {
            audio.playHeartChime()
        }
        delay(2400)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF0F5),
                        Color(0xFFFFE3E8),
                        Color(0xFFFCD5CE)
                    )
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Tap anywhere to skip splash
                onSplashComplete()
            }
    ) {
        // Background particles and decorative meadow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            val p = (cw / 100f).coerceIn(3f, 4.5f)

            // Spawn ambient floating petals/hearts
            if (Random.nextFloat() < 0.08f) {
                particles.spawnPetals(cw * Random.nextFloat(), ch * 0.9f, 1)
            }
            if (Random.nextFloat() < 0.04f) {
                particles.spawnHeart(cw * (0.3f + Random.nextFloat() * 0.4f), ch * 0.7f)
            }
            particles.update(0.016f)

            // Draw cozy grassy curved hill at bottom
            val hillY = ch * 0.63f
            drawCircle(
                color = Color(0xFF80B918),
                radius = cw * 0.85f,
                center = Offset(cw * 0.5f, hillY + cw * 0.85f - 18 * p)
            )
            drawCircle(
                color = Color(0xFF55A630),
                radius = cw * 0.85f,
                center = Offset(cw * 0.5f, hillY + cw * 0.85f)
            )

            // Render characters standing close together
            PixelArtRenderer.drawCharacter(
                drawScope = this,
                char = boy.copy(bounceOffset = charBob),
                centerX = cw * 0.42f,
                bottomY = hillY,
                pixelSize = p
            )

            PixelArtRenderer.drawCharacter(
                drawScope = this,
                char = girl.copy(bounceOffset = -charBob),
                centerX = cw * 0.58f,
                bottomY = hillY,
                pixelSize = p
            )

            // Draw floating particles
            particles.particles.forEach { pt ->
                drawRect(
                    color = pt.color.copy(alpha = pt.alpha),
                    topLeft = Offset(pt.x, pt.y),
                    size = Size(pt.size, pt.size)
                )
            }
        }

        // Top Branding and Pulsing Pixel Heart
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Beating heart badge
            Box(
                modifier = Modifier
                    .scale(heartScale)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = DeepRose,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Tiny Us",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp,
                color = DeepRose
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "A little place for you and me",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                color = TinyColors.InkMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "for you ${prefs.girlfriendName}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TinyColors.Rose
            )
        }

        // Bottom Loading / Hint Text
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Entering our cozy world...",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.95f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap anywhere to skip",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}
