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
fun DailyTinyMomentDialog(
    moment: TinyMoment,
    daysTogether: Long,
    boyfriendName: String,
    girlfriendName: String,
    onDismiss: () -> Unit,
    onJumpToScene: (SceneType) -> Unit,
    allMoments: List<TinyMoment> = emptyList()
) {
    var showRelationshipDetails by remember { mutableStateOf(false) }

    // Dynamic day counter that automatically refreshes across midnight
    var currentDay by remember {
        mutableLongStateOf(daysTogether.coerceAtLeast(RelationshipTimeManager.calculateTinyUsDay()))
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            val nowDay = RelationshipTimeManager.calculateTinyUsDay()
            if (nowDay != currentDay) {
                currentDay = nowDay
            }
            delay(10_000L) // 10s check for midnight transition without draining battery
        }
    }

    val momentsList = if (allMoments.isNotEmpty()) allMoments else listOf(moment)
    var selectedIndex by remember { mutableIntStateOf(moment.dayIndex) }
    val currentMoment = momentsList.find { it.dayIndex == selectedIndex } ?: moment

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tiny_moment_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Heart badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BlushPink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = DeepRose,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Today's Tiny Moment",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = DeepRose
                )

                Text(
                    text = "$boyfriendName & $girlfriendName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkSlate
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic, shining clickable Day milestone capsule
                val infiniteTransition = rememberInfiniteTransition(label = "dayMilestoneShine")
                val shineSweep by infiniteTransition.animateFloat(
                    initialValue = -100f,
                    targetValue = 280f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shineSweep"
                )
                val shineGlow by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "shineGlow"
                )

                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.93f else 1f,
                    animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                    label = "day_scale"
                )

                Surface(
                    modifier = Modifier
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .shadow(
                            elevation = (4f * shineGlow).dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color(0xFFFF4D6D),
                            spotColor = Color(0xFFFF758F)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isPressed = !isPressed
                            showRelationshipDetails = true
                        }
                        .testTag("clickable_day_counter"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF0F3),
                    border = BorderStroke(
                        width = (1.2f + 0.8f * shineGlow).dp,
                        color = Color(0xFFFF4D6D).copy(alpha = 0.45f + 0.5f * shineGlow)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .drawWithContent {
                                drawContent()
                                val shineWidth = 40.dp.toPx()
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.85f * shineGlow),
                                            Color(0xFFFFD166).copy(alpha = 0.65f * shineGlow),
                                            Color.Transparent
                                        ),
                                        start = Offset(shineSweep - shineWidth, 0f),
                                        end = Offset(shineSweep + shineWidth, size.height)
                                    ),
                                    size = size
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "\u2726",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB703).copy(alpha = 0.7f + 0.3f * shineGlow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Day $currentDay of Tiny Us",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFFC9184A),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "\u2726",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB703).copy(alpha = 0.7f + 0.3f * shineGlow)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tap to view our live love clock",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    color = DeepRose.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentMoment.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate,
                                modifier = Modifier.weight(1f)
                            )
                            if (momentsList.size > 1) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val currentIdx = momentsList.indexOfFirst { it.dayIndex == selectedIndex }
                                            val prevIdx = if (currentIdx <= 0) momentsList.size - 1 else currentIdx - 1
                                            selectedIndex = momentsList[prevIdx].dayIndex
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Previous scene",
                                            tint = PeachMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val currentIdx = momentsList.indexOfFirst { it.dayIndex == selectedIndex }
                                            val nextIdx = (currentIdx + 1) % momentsList.size
                                            selectedIndex = momentsList[nextIdx].dayIndex
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Next scene",
                                            tint = PeachMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentMoment.description,
                            fontSize = 14.sp,
                            color = DarkSlate.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "“${currentMoment.quote}”",
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = PeachMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = DarkSlate)
                    }

                    Button(
                        onClick = {
                            val matchedScene = when (currentMoment.dayIndex) {
                                0 -> SceneType.FLOWER
                                1 -> SceneType.UNDER_TREE
                                2 -> SceneType.COOKING
                                3 -> SceneType.SLEEP
                                4 -> SceneType.WALK
                                5 -> SceneType.LOOKING
                                6 -> SceneType.EVENING_RIDE
                                7 -> SceneType.MOMO_STALL
                                8 -> SceneType.COZY_LOFT
                                else -> SceneType.entries[currentMoment.dayIndex % SceneType.entries.size]
                            }
                            onJumpToScene(matchedScene)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("jump_scene_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepRose)
                    ) {
                        Text("Watch Scene", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showRelationshipDetails) {
        RelationshipDurationDialog(
            boyfriendName = boyfriendName,
            girlfriendName = girlfriendName,
            onDismiss = { showRelationshipDetails = false }
        )
    }
}

/**
 * Secondary relationship detail popup showing exact duration.
 * Displays years, months, days, and live ticking hours, minutes, seconds.
 * 100% offline, lightweight ticker stops when closed.
 */
@Composable
fun RelationshipDurationDialog(
    boyfriendName: String,
    girlfriendName: String,
    onDismiss: () -> Unit
) {
    var duration by remember {
        mutableStateOf(RelationshipTimeManager.getExactDuration())
    }

    // Lightweight live ticker active ONLY while this popup is visible
    LaunchedEffect(Unit) {
        while (isActive) {
            duration = RelationshipTimeManager.getExactDuration()
            delay(1000L)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("relationship_duration_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = CozyCream,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, BlushPink)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Heart icon
                Text(
                    text = "♡",
                    fontSize = 26.sp,
                    color = DeepRose,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "OUR TIME",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = DeepRose
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$boyfriendName & $girlfriendName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkSlate
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Together since",
                    fontSize = 11.sp,
                    color = DarkSlate.copy(alpha = 0.65f)
                )

                val formattedStartDate = remember {
                    com.example.data.RelationshipTimeManager.relationshipStartDate.format(
                        java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH)
                    )
                }

                Text(
                    text = formattedStartDate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkSlate
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(1.dp)
                        .background(Color(0x288B5E3C))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Years, Months, Days
                Text(
                    text = "${duration.years} years",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )
                Text(
                    text = "${duration.months} months",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )
                Text(
                    text = "${duration.days} days",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Live ticking time: hours, minutes, seconds
                val timeStr = String.format(
                    java.util.Locale.US,
                    "%02d hours  %02d mins  %02d secs",
                    duration.hours,
                    duration.minutes,
                    duration.seconds
                )
                Text(
                    text = timeStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PeachMuted,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(1.dp)
                        .background(Color(0x288B5E3C))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Total days count
                val formattedTotalDays = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
                    .format(duration.totalDays)
                Text(
                    text = "\u2726 $formattedTotalDays days \u2726",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    color = DeepRose
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(42.dp)
                        .testTag("close_duration_button"),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepRose)
                ) {
                    Text(
                        text = "Close",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Compact shining day badge with animated light sweep and glowing pulse.
 * Prompts user to click and view relationship duration and keepsakes.
 */
@Composable
fun ShiningDayBadge(
    dayCount: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("shining_day_badge"),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFF0F3),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFFF4D6D).copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u2726",
                fontSize = 9.sp,
                color = Color(0xFFFFB703).copy(alpha = 0.80f)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Day $dayCount",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC9184A),
                fontFamily = FontFamily.Serif
            )
        }
    }
}
