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

import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.text.font.FontStyle
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType

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

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tiny_moment_dialog"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TinyIconBadge(icon = TinyIcons.Heart, size = 48.dp, iconSize = 24.dp)

            Spacer(modifier = Modifier.height(TinySpace.md))

            Text(
                text = "Today's Tiny Moment",
                style = TinyType.Label.copy(color = TinyColors.Rose)
            )

            Text(
                text = "$boyfriendName & $girlfriendName",
                style = TinyType.Title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(TinySpace.sm))

            // Signature shining day pill: light sweep + glow pulse, in the rose palette
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

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPressed = !isPressed
                        showRelationshipDetails = true
                    }
                    .testTag("clickable_day_counter"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .shadow(
                            elevation = (3f * shineGlow).dp,
                            shape = TinyRadius.Large,
                            ambientColor = TinyColors.Rose,
                            spotColor = TinyColors.Blush
                        )
                        .clip(TinyRadius.Large),
                    shape = TinyRadius.Large,
                    color = TinyColors.RoseSoft,
                    border = BorderStroke(
                        width = (1f + 0.6f * shineGlow).dp,
                        color = TinyColors.Rose.copy(alpha = 0.35f + 0.45f * shineGlow)
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
                                            Color.White.copy(alpha = 0.75f * shineGlow),
                                            TinyColors.Blush.copy(alpha = 0.45f * shineGlow),
                                            Color.Transparent
                                        ),
                                        start = Offset(shineSweep - shineWidth, 0f),
                                        end = Offset(shineSweep + shineWidth, size.height)
                                    ),
                                    size = size
                                )
                            }
                            .padding(horizontal = TinySpace.lg, vertical = TinySpace.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = TinyIcons.Sparkle,
                                contentDescription = null,
                                tint = TinyColors.Rose.copy(alpha = 0.6f + 0.4f * shineGlow),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Day $currentDay of Tiny Us",
                                style = TinyType.Label.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = TinyColors.Rose
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = TinyIcons.Sparkle,
                                contentDescription = null,
                                tint = TinyColors.Rose.copy(alpha = 0.6f + 0.4f * shineGlow),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Tap to view our live love clock",
                style = TinyType.Micro
            )
        }

        TinyCard(spacing = TinySpace.sm) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentMoment.title,
                    style = TinyType.Section,
                    modifier = Modifier.weight(1f)
                )
                if (momentsList.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val currentIdx = momentsList.indexOfFirst { it.dayIndex == selectedIndex }
                                val prevIdx = if (currentIdx <= 0) momentsList.size - 1 else currentIdx - 1
                                selectedIndex = momentsList[prevIdx].dayIndex
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Previous scene",
                                tint = TinyColors.InkMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val currentIdx = momentsList.indexOfFirst { it.dayIndex == selectedIndex }
                                val nextIdx = (currentIdx + 1) % momentsList.size
                                selectedIndex = momentsList[nextIdx].dayIndex
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Next scene",
                                tint = TinyColors.InkMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = currentMoment.description,
                style = TinyType.Body
            )
            Text(
                text = "“${currentMoment.quote}”",
                style = TinyType.Caption.copy(fontStyle = FontStyle.Italic)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TinyButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                style = TinyButtonStyle.Ghost
            )

            TinyButton(
                text = "Watch Scene",
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
                modifier = Modifier.weight(1.4f),
                style = TinyButtonStyle.Primary,
                testTag = "jump_scene_button"
            )
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

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("relationship_duration_dialog"),
        widthFraction = 0.92f,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = TinyIcons.HeartOutline,
                contentDescription = null,
                tint = TinyColors.Rose,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(TinySpace.sm))

            Text(
                text = "OUR TIME",
                style = TinyType.Label.copy(color = TinyColors.Rose, letterSpacing = 1.5.sp)
            )

            Text(
                text = "$boyfriendName & $girlfriendName",
                style = TinyType.Title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(TinySpace.sm))

            Text(
                text = "Together since",
                style = TinyType.Caption
            )

            val formattedStartDate = remember {
                com.example.data.RelationshipTimeManager.relationshipStartDate.format(
                    java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH)
                )
            }

            Text(
                text = formattedStartDate,
                style = TinyType.BodyStrong
            )
        }

        TinyDivider()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Years, Months, Days
            Text(
                text = "${duration.years} years",
                style = TinyType.Section.copy(fontSize = 18.sp, lineHeight = 24.sp)
            )
            Text(
                text = "${duration.months} months",
                style = TinyType.Section.copy(fontSize = 18.sp, lineHeight = 24.sp)
            )
            Text(
                text = "${duration.days} days",
                style = TinyType.Section.copy(fontSize = 18.sp, lineHeight = 24.sp)
            )

            Spacer(modifier = Modifier.height(TinySpace.sm))

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
                style = TinyType.Caption.copy(letterSpacing = 0.5.sp)
            )
        }

        TinyDivider()

        // Total days count
        val formattedTotalDays = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
            .format(duration.totalDays)
        Text(
            text = "$formattedTotalDays days",
            style = TinyType.Display.copy(color = TinyColors.Rose),
            textAlign = TextAlign.Center
        )

        TinyButton(
            text = "Close",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(0.6f),
            style = TinyButtonStyle.Secondary,
            testTag = "close_duration_button"
        )
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
            .clip(TinyRadius.Small)
            .clickable(onClick = onClick)
            .testTag("shining_day_badge"),
        shape = TinyRadius.Small,
        color = TinyColors.RoseSoft,
        border = BorderStroke(
            width = 1.dp,
            color = TinyColors.Rose.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = TinyIcons.Sparkle,
                contentDescription = null,
                tint = TinyColors.Rose.copy(alpha = 0.8f),
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Day $dayCount",
                style = TinyType.Micro.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = TinyColors.Rose
                )
            )
        }
    }
}
