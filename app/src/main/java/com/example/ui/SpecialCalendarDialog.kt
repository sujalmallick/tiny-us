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
fun SpecialCalendarDialog(
    onDismiss: () -> Unit,
    audio: AmbientAudio? = null,
    boyfriendName: String = com.example.data.ProfileManager.getProfile().boyName,
    girlfriendName: String = com.example.data.ProfileManager.getProfile().girlName
) {
    var displayedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedMemory by remember { mutableStateOf<TinyUsMemory?>(null) }
    val today = remember { LocalDate.now() }
    val todayMemories = remember(displayedYearMonth) { SpecialCalendarManager.getMemoriesForDate(today) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9F5)),
            border = BorderStroke(2.dp, SoftRose.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("special_calendar_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = DeepRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OUR SPECIAL CALENDAR",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepRose,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "$boyfriendName & $girlfriendName • Precious Moments",
                            fontSize = 11.sp,
                            color = DarkSlate.copy(alpha = 0.55f),
                            fontFamily = FontFamily.Serif
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DarkSlate.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Today's Special Event Banner (if today has an event!)
                if (todayMemories.isNotEmpty()) {
                    val firstToday = todayMemories.first()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DeepRose.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, DeepRose.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                audio?.playHeartChime()
                                selectedMemory = firstToday
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "❤️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TODAY • ${firstToday.title.uppercase()}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepRose
                                )
                                Text(
                                    text = "Tap to view today's memory",
                                    fontSize = 10.sp,
                                    color = DarkSlate.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Month Navigation Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            audio?.playBubblePop()
                            displayedYearMonth = displayedYearMonth.minusMonths(1)
                        },
                        modifier = Modifier.size(32.dp).testTag("calendar_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = DeepRose,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val monthTitle = remember(displayedYearMonth) {
                            displayedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                        }
                        Text(
                            text = monthTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = DarkSlate
                        )
                        if (displayedYearMonth != YearMonth.from(today)) {
                            Text(
                                text = "Jump to Today",
                                fontSize = 10.sp,
                                color = DeepRose,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    audio?.playBubblePop()
                                    displayedYearMonth = YearMonth.from(today)
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            audio?.playBubblePop()
                            displayedYearMonth = displayedYearMonth.plusMonths(1)
                        },
                        modifier = Modifier.size(32.dp).testTag("calendar_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = DeepRose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days of Week Header
                val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { d ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = d,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate.copy(alpha = 0.45f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Days Grid
                val firstDayOfWeek = displayedYearMonth.atDay(1).dayOfWeek.value // 1=Mon, 7=Sun
                val daysInMonth = displayedYearMonth.lengthOfMonth()
                val emptySlotsBefore = firstDayOfWeek - 1
                val totalCells = emptySlotsBefore + daysInMonth
                val totalRows = (totalCells + 6) / 7

                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - emptySlotsBefore + 1
                            if (dayNum in 1..daysInMonth) {
                                val cellDate = displayedYearMonth.atDay(dayNum)
                                val isCellToday = (cellDate == today)
                                val cellMemories = SpecialCalendarManager.getMemoriesForDate(cellDate)
                                val hasEvent = cellMemories.isNotEmpty()
                                val memoryType = cellMemories.firstOrNull()?.type

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isCellToday -> DeepRose.copy(alpha = 0.14f)
                                                hasEvent -> SoftRose.copy(alpha = 0.22f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isCellToday) 1.5.dp else if (hasEvent) 1.dp else 0.dp,
                                            color = if (isCellToday) DeepRose else if (hasEvent) SoftRose.copy(alpha = 0.6f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable(enabled = hasEvent) {
                                            audio?.playHeartChime()
                                            selectedMemory = cellMemories.first()
                                        }
                                        .testTag(if (hasEvent) "calendar_event_$cellDate" else "calendar_day_$dayNum"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isCellToday || hasEvent) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isCellToday -> DeepRose
                                                hasEvent -> Color(0xFF8B1E22)
                                                else -> DarkSlate.copy(alpha = 0.85f)
                                            }
                                        )

                                        if (hasEvent) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            CalendarPixelMarker(type = memoryType ?: SpecialMemoryType.RELATIONSHIP)
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f).height(40.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrapbook Legend Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(icon = "❤️", label = "Moments")
                    LegendItem(icon = "🎂", label = "Birthdays")
                    LegendItem(icon = "✨", label = "Next Meet")
                    LegendItem(icon = "🌸", label = "Special")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("calendar_close_button")
                ) {
                    Text("Close Scrapbook", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // Detail Pop-up Card when a date is tapped
    if (selectedMemory != null) {
        CalendarMemoryDetailCard(
            memory = selectedMemory!!,
            onDismiss = { selectedMemory = null }
        )
    }
}

@Composable
fun CalendarMemoryDetailCard(
    memory: TinyUsMemory,
    onDismiss: () -> Unit
) {
    var countdown by remember {
        mutableStateOf(SpecialCalendarManager.calculateCountdown(memory.date))
    }

    if (memory.isFuture) {
        LaunchedEffect(memory.date) {
            while (isActive) {
                countdown = SpecialCalendarManager.calculateCountdown(memory.date)
                delay(1000)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9F6)),
            border = BorderStroke(2.dp, SoftRose.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("memory_detail_card")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val iconEmoji = when (memory.type) {
                    SpecialMemoryType.BIRTHDAY -> "🎂"
                    SpecialMemoryType.FUTURE_MEETING -> "✨"
                    SpecialMemoryType.KISS -> "💋"
                    SpecialMemoryType.PRIVATE -> "🌸"
                    else -> "❤️"
                }
                Text(
                    text = iconEmoji,
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = memory.title.uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepRose,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                val formattedDate = remember(memory.date) {
                    memory.date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                }
                Text(
                    text = formattedDate,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = DarkSlate.copy(alpha = 0.70f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "• • •",
                    fontSize = 12.sp,
                    color = SoftRose
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (memory.isFuture) {
                    if (countdown.isToday) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DeepRose.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, DeepRose.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TODAY ❤️",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepRose
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "You're finally together again.",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = DarkSlate
                                )
                            }
                        }
                    } else if (countdown.isPassed) {
                        Text(
                            text = memory.description,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Serif,
                            color = DarkSlate.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Coming in...",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkSlate.copy(alpha = 0.55f),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF7ECE1),
                                border = BorderStroke(1.dp, Color(0xFFE0C9B3)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${countdown.days} DAYS",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepRose
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = String.format("%02d HOURS  %02d MINUTES  %02d SECONDS", countdown.hours, countdown.minutes, countdown.seconds),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = DarkSlate.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = memory.description,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif,
                                color = DarkSlate.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = memory.description,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Serif,
                        color = DarkSlate.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("memory_detail_close_button")
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
internal fun CalendarPixelMarker(type: SpecialMemoryType) {
    Canvas(modifier = Modifier.size(8.dp, 8.dp)) {
        val p = size.width / 4f
        when (type) {
            SpecialMemoryType.BIRTHDAY -> {
                drawRect(Color(0xFFD4A373), Offset(0f, 2 * p), Size(4 * p, 2 * p))
                drawRect(Color(0xFFFFD166), Offset(1.5f * p, 0.5f * p), Size(p, 1.5f * p))
                drawRect(Color(0xFFFF4D6D), Offset(1.5f * p, 0f), Size(p, 0.8f * p))
            }
            SpecialMemoryType.FUTURE_MEETING -> {
                drawRect(Color(0xFFFFB703), Offset(p, 0f), Size(2 * p, 4 * p))
                drawRect(Color(0xFFFFB703), Offset(0f, p), Size(4 * p, 2 * p))
                drawRect(Color(0xFFFFD166), Offset(p, p), Size(2 * p, 2 * p))
            }
            SpecialMemoryType.PRIVATE -> {
                drawRect(Color(0xFFB5838D), Offset(0.5f * p, 0.5f * p), Size(3 * p, 2 * p))
                drawRect(Color(0xFFB5838D), Offset(p, 2.5f * p), Size(2 * p, p))
                drawRect(Color(0xFFB5838D), Offset(1.5f * p, 3.5f * p), Size(p, 0.5f * p))
            }
            else -> {
                drawRect(DeepRose, Offset(0.5f * p, 0.5f * p), Size(3 * p, 2 * p))
                drawRect(DeepRose, Offset(p, 2.5f * p), Size(2 * p, p))
                drawRect(DeepRose, Offset(1.5f * p, 3.5f * p), Size(p, 0.5f * p))
            }
        }
    }
}

@Composable
internal fun LegendItem(icon: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, fontSize = 10.sp, color = DarkSlate.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
    }
}
