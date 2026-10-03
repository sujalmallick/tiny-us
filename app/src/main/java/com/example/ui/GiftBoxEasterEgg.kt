package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.engine.AmbientAudio
import com.example.engine.MusicBoxState
import com.example.engine.Song
import com.example.scene.SceneEngine
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.example.data.SpecialCalendarManager
import com.example.data.TinyUsMemory
import com.example.data.SpecialMemoryType
import com.example.data.LiveCountdown
import com.example.data.LoveNoteItem
import com.example.data.MemoryItem
import com.example.data.PreferencesManager
import com.example.data.RelationshipTimeManager
import com.example.data.TinyMoment
import com.example.data.PolaroidManager
import com.example.data.PolaroidMemory
import com.example.scene.SceneType
import com.example.ui.theme.BlushPink
import com.example.ui.theme.CozyCream
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepRose
import com.example.ui.theme.PeachMuted
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftRose
import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isOpened) 2.dp else 1.5.dp,
                color = if (isOpened) Color(0xFFFF4D6D) else Color(0xFFFFB5C2),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isOpened) Color(0xFFFFF7F9) else Color(0xFFFFFFFF)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isOpened) {
                // UNOPENED GIFT BOX
                Text(
                    text = "A Secret Surprise for You",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC9184A)
                )
                Spacer(modifier = Modifier.height(12.dp))

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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkSlate.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tapCount >= i) Color(0xFFFF4D6D) else Color(0xFFFFD1DC)
                                )
                        )
                    }
                }
            } else {
                // Compact opened state — full content shown in RomanticSurpriseDialog
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your surprise is ready",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFFC9184A),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showSurpriseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D6D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Your Surprise", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = {
                        tapCount = 0
                        isOpened = false
                        isNameRevealed = false
                    }) {
                        Text("Rewrap Gift", color = DarkSlate.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("romantic_surprise_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = CozyCream,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A Gift Made With Love",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFFC9184A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "MADE with love by yours",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = DarkSlate.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Glowing interactive secret name capsule
                Surface(
                    modifier = Modifier
                        .shadow(
                            elevation = (8f * glowPulse).dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = Color(0xFFFF4D6D),
                            spotColor = Color(0xFFFF4D6D)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { isNameRevealed = !isNameRevealed },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isNameRevealed) Color(0xFFFFECEF) else Color(0xFFFFF0F3),
                    border = BorderStroke(
                        width = (1.5f + 1.5f * glowPulse).dp,
                        color = Color(0xFFFF4D6D).copy(alpha = 0.5f + 0.5f * glowPulse)
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isNameRevealed) {
                            Text(
                                text = "Tap to Unhide Name",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFFC9184A),
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 0.5.sp
                            )
                        } else {
                            val profile = com.example.data.ProfileManager.getProfile()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = profile.boyName,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Serif,
                                    color = Color(0xFFC9184A),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Always yours! (Tap to hide)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepRose.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Romantic letter
                val profile = com.example.data.ProfileManager.getProfile()
                val creatorName = if (isNameRevealed) profile.boyName else "Your Favorite Person"
                val letterContent = profile.secretLetter.ifBlank {
                    "I built this little digital home so we can always share cozy moments together, no matter where we are. Every single pixel, every melody, and every little secret was crafted with all my love, just for you."
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "To the love of my life,",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$letterContent\n\nForever yours,\n$creatorName",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF0F3),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = profile.secretCodeTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC9184A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = profile.secretCodeBody,
                                    fontSize = 11.sp,
                                    color = DarkSlate.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "A Little Guide for You (How to Play)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC9184A),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onJumpToMomoStall()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC1121F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Take Me to Street Food Date!", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = DarkSlate.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    TextButton(onClick = onRewrap) {
                        Text("Rewrap Gift", color = DarkSlate.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
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
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon.isNotEmpty()) {
                Text(text = icon, fontSize = 18.sp)
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = Color(0xFF2B2D42)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 11.5.sp,
                    color = DarkSlate.copy(alpha = 0.75f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
