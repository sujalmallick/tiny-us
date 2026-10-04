package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelCornerShape
import com.example.ui.theme.PixelCircleShape

@Composable
fun GiftBoxEasterEgg(
    onJumpToMomoStall: () -> Unit
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var isOpened by remember { mutableStateOf(false) }
    var isNameRevealed by remember { mutableStateOf(false) }
    var showSurpriseDialog by remember { mutableStateOf(false) }

    if (showSurpriseDialog) {
        RomanticSurpriseDialog(
            onDismiss = { showSurpriseDialog = false },
            onJumpToMomoStall = onJumpToMomoStall,
            onRewrap = {
                showSurpriseDialog = false
                tapCount = 0
                isOpened = false
                isNameRevealed = false
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "giftGlowTransition")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val wobbleAngle by animateFloatAsState(
        targetValue = when {
            isOpened -> 0f
            tapCount == 0 -> 0f
            tapCount % 2 == 1 -> -10f
            else -> 10f
        },
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "wobble"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = TinyRadius.Large,
        color = if (isOpened) TinyColors.RoseSoft else TinyColors.Card,
        contentColor = TinyColors.Ink,
        border = BorderStroke(1.dp, if (isOpened) TinyColors.Rose.copy(alpha = 0.55f) else TinyColors.Blush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TinySpace.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isOpened) {
                // UNOPENED GIFT BOX
                Text(
                    text = "A Secret Surprise for You",
                    style = TinyType.Section.copy(color = TinyColors.Rose),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(TinySpace.md))

                // Interactive Pixel Gift Box with Ribbon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            rotationZ = wobbleAngle
                            val scale = 1f + (tapCount * 0.05f)
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            tapCount++
                            if (tapCount >= 5) {
                                isOpened = true
                                showSurpriseDialog = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(68.dp)) {
                        val p = size.width / 20f

                        // Main Gift Box (Rose / Scarlet)
                        drawRect(Color(0xFFE63946), Offset(2 * p, 6 * p), Size(16 * p, 13 * p))
                        drawRect(Color(0xFFC1121F), Offset(2 * p, 17 * p), Size(16 * p, 2 * p))

                        // Box Lid
                        drawRect(Color(0xFFFF4D6D), Offset(1 * p, 4 * p), Size(18 * p, 4 * p))
                        drawRect(Color(0xFFC1121F), Offset(1 * p, 7 * p), Size(18 * p, 1 * p))

                        // Golden Ribbon
                        drawRect(Color(0xFFFFD166), Offset(8 * p, 4 * p), Size(4 * p, 15 * p))
                        drawRect(Color(0xFFFFEEAA), Offset(9 * p, 4 * p), Size(2 * p, 15 * p))
                        drawRect(Color(0xFFFFD166), Offset(2 * p, 11 * p), Size(16 * p, 3 * p))
                        drawRect(Color(0xFFFFEEAA), Offset(2 * p, 12 * p), Size(16 * p, 1 * p))

                        // Ribbon Bow Loops on top
                        drawRect(Color(0xFFFFD166), Offset(5 * p, 1 * p), Size(4 * p, 3 * p))
                        drawRect(Color(0xFFFFEEAA), Offset(6 * p, 1.5f * p), Size(2 * p, 2 * p))
                        drawRect(Color(0xFFFFD166), Offset(11 * p, 1 * p), Size(4 * p, 3 * p))
                        drawRect(Color(0xFFFFEEAA), Offset(12 * p, 1.5f * p), Size(2 * p, 2 * p))
                        // Bow knot
                        drawRect(Color(0xFFFF9E00), Offset(8.5f * p, 2.5f * p), Size(3 * p, 2.5f * p))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress teaser text
                val hintText = when (tapCount) {
                    0 -> "Tied with a golden ribbon... Tap 5 times to open!"
                    1 -> "Untying the ribbon... (1/5)"
                    2 -> "Loosening the golden knot... (2/5)"
                    3 -> "Something made specially for you inside! (3/5)"
                    4 -> "Almost open! Just 1 more tap! (4/5)"
                    else -> "Opening with love!"
                }

                Text(
                    text = hintText,
                    style = TinyType.Caption,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(TinySpace.sm))

                // Progress dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(PixelCircleShape)
                                .background(
                                    if (tapCount >= i) TinyColors.Rose else TinyColors.Blush.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            } else {
                // Compact opened state — full content shown in RomanticSurpriseDialog
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
                ) {
                    Text(
                        text = "Your surprise is ready",
                        style = TinyType.Section.copy(color = TinyColors.Rose),
                        textAlign = TextAlign.Center
                    )
                    TinyButton(
                        text = "Open Your Surprise",
                        onClick = { showSurpriseDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        style = TinyButtonStyle.Primary,
                        icon = TinyIcons.Gift
                    )
                    TinyButton(
                        text = "Rewrap Gift",
                        onClick = {
                            tapCount = 0
                            isOpened = false
                            isNameRevealed = false
                        },
                        style = TinyButtonStyle.Ghost
                    )
                }
            }
        }
    }
}

@Composable
internal fun RomanticSurpriseDialog(
    onDismiss: () -> Unit,
    onJumpToMomoStall: () -> Unit,
    onRewrap: () -> Unit
) {
    var isNameRevealed by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "surpriseGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "surpriseGlowPulse"
    )

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("romantic_surprise_dialog"),
        verticalSpacing = TinySpace.lg
    ) {
        TinyDialogHeader(
            title = "A Gift Made With Love",
            subtitle = "MADE with love by yours",
            icon = TinyIcons.Gift
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TinySpace.lg)
        ) {
            // Glowing interactive secret name capsule
            val capsuleShape = PixelCornerShape(18.dp)
            Surface(
                modifier = Modifier
                    .padding(top = TinySpace.xs)
                    .shadow(
                        elevation = (6f * glowPulse).dp,
                        shape = capsuleShape,
                        ambientColor = TinyColors.Rose,
                        spotColor = TinyColors.Rose
                    )
                    .clip(capsuleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isNameRevealed = !isNameRevealed },
                shape = capsuleShape,
                color = TinyColors.RoseSoft,
                contentColor = TinyColors.Rose,
                border = BorderStroke(
                    width = (1f + 1f * glowPulse).dp,
                    color = TinyColors.Rose.copy(alpha = 0.3f + 0.45f * glowPulse)
                )
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isNameRevealed) {
                        Text(
                            text = "Tap to Unhide Name",
                            style = TinyType.Section.copy(color = TinyColors.Rose, letterSpacing = 0.5.sp)
                        )
                    } else {
                        val profile = com.example.data.ProfileManager.getProfile()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = profile.boyName,
                                style = TinyType.Display.copy(color = TinyColors.Rose, letterSpacing = 0.5.sp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Always yours! (Tap to hide)",
                                style = TinyType.Micro.copy(color = TinyColors.Rose)
                            )
                        }
                    }
                }
            }

            // Romantic letter
            val profile = com.example.data.ProfileManager.getProfile()
            val creatorName = if (isNameRevealed) profile.boyName else "Your Favorite Person"
            val letterContent = profile.secretLetter.ifBlank {
                "I built this little digital home so we can always share cozy moments together, no matter where we are. Every single pixel, every melody, and every little secret was crafted with all my love, just for you."
            }
            TinyCard(spacing = TinySpace.sm) {
                Text(
                    text = "To the love of my life,",
                    style = TinyType.BodyStrong
                )
                Text(
                    text = "$letterContent\n\nForever yours,\n$creatorName",
                    style = TinyType.Body
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TinySpace.xs)
                        .background(TinyColors.RoseSoft, TinyRadius.Medium)
                        .padding(TinySpace.md)
                ) {
                    Text(
                        text = profile.secretCodeTitle,
                        style = TinyType.Label.copy(color = TinyColors.Rose)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.secretCodeBody,
                        style = TinyType.Caption.copy(color = TinyColors.Ink)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
            ) {
                Text(
                    text = "A Little Guide for You (How to Play)",
                    style = TinyType.Section
                )
                GuideItem(
                    icon = "",
                    title = "Street Food Date (Our Food Stall)",
                    desc = "Go on a street food date at our cozy stall! Watch the couple share steaming bites. Tap the steamer to puff steam, tap the sign for neon stars, and tap the spicy dip!"
                )
                GuideItem(
                    icon = "",
                    title = "Double-Click Secret Whispers",
                    desc = "Double-tap on either character to hear them jump and whisper sweet affectionate secrets to each other!"
                )
                GuideItem(
                    icon = "",
                    title = "Cozy Couple Hug",
                    desc = "Tap right between both characters to make them wrap in a sweet warm hug with a fountain of floating hearts!"
                )
                GuideItem(
                    icon = "",
                    title = "Interactive World Touches",
                    desc = "Tap the sky for shooting stars at night, or fluffy clouds by day. Tap meadow flowers to blow swirling petals. Tap the big tree to shower drifting leaves. Tap our sleeping cat to hear him purr! Tap the streetlamp at night to toggle cozy light."
                )
                GuideItem(
                    icon = "",
                    title = "Atmosphere and Relaxing Melodies",
                    desc = "Switch skies anytime (Day, Sunset, Starry Night) and toggle soothing music box lullabies whenever you want to relax."
                )
                GuideItem(
                    icon = "",
                    title = "Love Letters and Keepsakes",
                    desc = "Write secret letters in our mailbox that stay saved forever, and view our days together and memories!"
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(TinySpace.xs)) {
            TinyButton(
                text = "Take Me to Street Food Date!",
                onClick = {
                    onDismiss()
                    onJumpToMomoStall()
                },
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TinyButton(text = "Close", onClick = onDismiss, style = TinyButtonStyle.Ghost)
                TinyButton(text = "Rewrap Gift", onClick = onRewrap, style = TinyButtonStyle.Ghost)
            }
        }
    }
}

@Composable
internal fun GuideItem(
    icon: String,
    title: String,
    desc: String
) {
    TinyCard(padding = TinySpace.md, spacing = 0.dp) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)
        ) {
            if (icon.isNotEmpty()) {
                Text(text = icon, fontSize = 18.sp)
            }
            Column {
                Text(
                    text = title,
                    style = TinyType.Label
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = TinyType.Caption
                )
            }
        }
    }
}
