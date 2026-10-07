package com.example.ui

import com.example.engine.WorldAudio
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.engine.ParticleSystem
import com.example.ui.theme.DeepRose
import com.example.ui.theme.TinyColors
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource

/** The opening scene; it plays through [audio] and releases it when the splash ends. */
@Composable
fun SplashScreen(
    prefs: PreferencesManager,
    audio: WorldAudio,
    onSplashComplete: () -> Unit
) {
    DisposableEffect(audio) {
        onDispose {
            audio.release()
        }
    }
    val particles = remember { ParticleSystem() }

    // Heartbeat pulsating animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = stringResource(Res.string.ui_heartscale)
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
            // The picture's own sky, so a tall screen just has more sky above the scene.
            .background(SplashSky)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Tap anywhere to skip splash
                onSplashComplete()
            }
    ) {
        // The two of them on the pier at sunset, with Mochi: fitted to the width and sitting on
        // the bottom edge, so the lamp and the lighthouse are never cropped away.
        Image(
            painter = painterResource(Res.drawable.splash_sunset),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .aspectRatio(SPLASH_ASPECT)
                // The top of the picture melts into the plain sky above it: no seam.
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(listOf(SplashSky, Color.Transparent), startY = 0f, endY = size.height * 0.14f))
                }
        )

        // A few hearts drifting up over the sea.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            if (Random.nextFloat() < 0.03f) {
                particles.spawnHeart(cw * (0.3f + Random.nextFloat() * 0.4f), ch * 0.72f)
            }
            particles.update(0.016f)
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
                    .clip(PixelCircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PixelIcons.Favorite,
                    contentDescription = null,
                    tint = DeepRose,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(Res.string.ui_tiny_us),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp,
                color = Color.White,
                style = TextStyle(shadow = SkyShadow)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(Res.string.ui_a_little_place_for_you_and_me),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                color = Color.White.copy(alpha = 0.92f),
                style = TextStyle(shadow = SkyShadow)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.ui_splash_for_you, prefs.girlfriendName),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFD6E0),
                style = TextStyle(shadow = SkyShadow)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = stringResource(Res.string.ui_entering_our_cozy_world),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                style = TextStyle(shadow = SkyShadow)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.ui_tap_anywhere_to_skip),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

/** The sunset picture's width over its height (580 x 744). */
private const val SPLASH_ASPECT = 580f / 744f

/** The top of the sunset picture's sky, continued above it on tall screens. */
private val SplashSky = Color(0xFF353783)

/** A soft dark shadow so the white words read over the bright sunset. */
private val SkyShadow = Shadow(color = Color(0x99201640), offset = Offset(0f, 3f), blurRadius = 6f)
