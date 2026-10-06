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
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource

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
        label = stringResource(Res.string.ui_glowpulse)
    )

    val wobbleAngle by animateFloatAsState(
        targetValue = when {
            isOpened -> 0f
            tapCount == 0 -> 0f
            tapCount % 2 == 1 -> -10f
            else -> 10f
        },
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = stringResource(Res.string.ui_wobble)
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
                    text = stringResource(Res.string.ui_a_secret_surprise_for_you),
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
                    0 -> stringResource(Res.string.ui_gift_hint_0)
                    1 -> stringResource(Res.string.ui_gift_hint_1)
                    2 -> stringResource(Res.string.ui_gift_hint_2)
                    3 -> stringResource(Res.string.ui_gift_hint_3)
                    4 -> stringResource(Res.string.ui_gift_hint_4)
                    else -> stringResource(Res.string.ui_gift_hint_5)
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
                        text = stringResource(Res.string.ui_your_surprise_is_ready),
                        style = TinyType.Section.copy(color = TinyColors.Rose),
                        textAlign = TextAlign.Center
                    )
                    TinyButton(
                        text = stringResource(Res.string.ui_open_your_surprise),
                        onClick = { showSurpriseDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        style = TinyButtonStyle.Primary,
                        icon = TinyIcons.Gift
                    )
                    TinyButton(
                        text = stringResource(Res.string.ui_rewrap_gift),
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
        label = stringResource(Res.string.ui_surpriseglowpulse)
    )

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("romantic_surprise_dialog"),
        verticalSpacing = TinySpace.lg
    ) {
        TinyDialogHeader(
            title = stringResource(Res.string.ui_a_gift_made_with_love),
            subtitle = stringResource(Res.string.ui_made_with_love_by_yours),
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
                            text = stringResource(Res.string.ui_tap_to_unhide_name),
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
                                text = stringResource(Res.string.ui_always_yours_tap_to_hide),
                                style = TinyType.Micro.copy(color = TinyColors.Rose)
                            )
                        }
                    }
                }
            }

            // Romantic letter
            val profile = com.example.data.ProfileManager.getProfile()
            val creatorName = if (isNameRevealed) profile.boyName else stringResource(Res.string.ui_gift_your_favorite_person)
            val letterContent = profile.secretLetter.ifBlank {
                stringResource(Res.string.ui_gift_letter_default)
            }
            TinyCard(spacing = TinySpace.sm) {
                Text(
                    text = stringResource(Res.string.ui_to_the_love_of_my_life),
                    style = TinyType.BodyStrong
                )
                Text(
                    text = stringResource(Res.string.ui_letter_signoff, letterContent, creatorName),
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
                    text = stringResource(Res.string.ui_a_little_guide_for_you_how_to_play),
                    style = TinyType.Section
                )
                GuideItem(
                    icon = "",
                    title = stringResource(Res.string.ui_street_food_date_our_food_stall),
                    desc = stringResource(Res.string.ui_gift_guide_0)
                )
                GuideItem(
                    icon = "",
                    title = stringResource(Res.string.ui_double_click_secret_whispers),
                    desc = stringResource(Res.string.ui_gift_guide_1)
                )
                GuideItem(
                    icon = "",
                    title = stringResource(Res.string.ui_cozy_couple_hug),
                    desc = stringResource(Res.string.ui_gift_guide_2)
                )
                GuideItem(
                    icon = "",
                    title = stringResource(Res.string.ui_interactive_world_touches),
                    desc = stringResource(Res.string.ui_gift_guide_3)
                )
                GuideItem(
                    icon = "",
                    title = stringResource(Res.string.ui_atmosphere_and_relaxing_melodies),
                    desc = stringResource(Res.string.ui_gift_guide_4)
                )
                GuideItem(
                    icon = "",
                    title = stringResource(Res.string.ui_love_letters_and_keepsakes),
                    desc = stringResource(Res.string.ui_gift_guide_5)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(TinySpace.xs)) {
            TinyButton(
                text = stringResource(Res.string.ui_take_me_to_street_food_date),
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
                TinyButton(text = stringResource(Res.string.ui_close), onClick = onDismiss, style = TinyButtonStyle.Ghost)
                TinyButton(text = stringResource(Res.string.ui_rewrap_gift), onClick = onRewrap, style = TinyButtonStyle.Ghost)
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
