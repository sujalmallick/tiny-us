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
fun MemoriesDialog(
    memories: List<MemoryItem>,
    onDismiss: () -> Unit,
    onAddMemory: (title: String, note: String, date: String, iconType: String) -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("heart") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("memories_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Our Keepsakes",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Text(
                            text = "Memories captured in our tiny world",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_memories")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!showAddSheet) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(memories) { mem ->
                            MemoryCard(mem)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showAddSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_memory_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Our Memory", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Add Memory Form
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Memory Title") },
                            placeholder = { Text("e.g. Rainy Day Cocoa") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newDate,
                            onValueChange = { newDate = it },
                            label = { Text("Date / Season") },
                            placeholder = { Text("e.g. Autumn Afternoon") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newNote,
                            onValueChange = { newNote = it },
                            label = { Text("Sweet Note") },
                            placeholder = { Text("What made this moment special?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        Text("Select Icon:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkSlate)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val icons = listOf("heart", "flower", "tree", "cooking", "couch", "stars")
                            icons.forEach { ic ->
                                val isSelected = selectedIcon == ic
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) DeepRose else BlushPink.copy(alpha = 0.5f))
                                        .clickable { selectedIcon = ic },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconForType(ic),
                                        contentDescription = ic,
                                        tint = if (isSelected) Color.White else DarkSlate,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAddSheet = false }) {
                                Text("Cancel", color = DarkSlate)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newTitle.isNotBlank()) {
                                        onAddMemory(newTitle, newNote, newDate, selectedIcon)
                                        showAddSheet = false
                                        newTitle = ""
                                        newNote = ""
                                        newDate = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepRose)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(mem: MemoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BlushPink.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForType(mem.iconType),
                    contentDescription = null,
                    tint = DeepRose,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mem.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkSlate,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mem.date,
                        fontSize = 11.sp,
                        color = DarkSlate.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mem.note,
                    fontSize = 13.sp,
                    color = DarkSlate.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun LoveNotesDialog(
    notes: List<LoveNoteItem>,
    boyfriendName: String,
    girlfriendName: String,
    onDismiss: () -> Unit,
    onAddNote: (text: String, author: String) -> Unit
) {
    var showWriteMode by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var noteAuthor by remember { mutableStateOf("From $boyfriendName") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("love_notes_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Secret Love Letters",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Text(
                            text = "Heartfelt words left for each other",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_love_notes")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!showWriteMode) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notes) { note ->
                            NoteCard(note)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showWriteMode = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("write_note_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRose),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Write Secret Letter", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Write Note Form
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Your Message") },
                            placeholder = { Text("Write something sweet that will make them smile...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )

                        Text("From:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkSlate)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = noteAuthor == "From $boyfriendName",
                                onClick = { noteAuthor = "From $boyfriendName" },
                                label = { Text("From $boyfriendName") }
                            )
                            FilterChip(
                                selected = noteAuthor == "From $girlfriendName",
                                onClick = { noteAuthor = "From $girlfriendName" },
                                label = { Text("From $girlfriendName") }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showWriteMode = false }) {
                                Text("Cancel", color = DarkSlate)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (noteText.isNotBlank()) {
                                        onAddNote(noteText, noteAuthor)
                                        showWriteMode = false
                                        noteText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftRose)
                            ) {
                                Text("Send to Mailbox")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: LoveNoteItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, BlushPink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = "“${note.text}”",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                color = DarkSlate,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.author,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepRose
                )
                Text(
                    text = note.date,
                    fontSize = 11.sp,
                    color = DarkSlate.copy(alpha = 0.4f)
                )
            }
        }
    }
}

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

@Composable
fun ScenePickerDialog(
    currentScene: SceneType,
    onDismiss: () -> Unit,
    onSelectScene: (SceneType) -> Unit,
    onRandomScene: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scene_picker_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose a Scene",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_scenes")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onRandomScene()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("random_scene_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PeachMuted),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Surprise Me (Random Scene)", color = DarkSlate, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(SceneType.values()) { sc ->
                        val isSelected = sc == currentScene
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectScene(sc)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BlushPink.copy(alpha = 0.5f) else Color.White
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, DeepRose) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) DeepRose else BlushPink.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getSceneIcon(sc),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else DarkSlate,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = sc.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DarkSlate
                                    )
                                    Text(
                                        text = sc.subtitle,
                                        fontSize = 11.sp,
                                        color = DarkSlate.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepRose,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = DarkSlate,
                fontFamily = FontFamily.Serif
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = DarkSlate.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 25.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    prefs: PreferencesManager,
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit,
    onReplayScene: () -> Unit,
    onOpenScenePicker: () -> Unit = {},
    onOpenMemories: () -> Unit = {},
    onOpenLoveNotes: () -> Unit = {},
    onOpenPolaroids: () -> Unit = {},
    onOpenDreamJournal: () -> Unit = {},
    onOpenWardrobe: () -> Unit = {},
    onOpenDateAdventures: () -> Unit = {},
    onOpenDailyMoment: () -> Unit = {},
    onOpenMiniGames: () -> Unit = {},
    onOpenSharedMood: () -> Unit = {},
    onOpenLongDistance: () -> Unit = {},
    onJumpToScene: (SceneType) -> Unit = {}
) {
    var boyName by remember { mutableStateOf(prefs.boyfriendName) }
    var girlName by remember { mutableStateOf(prefs.girlfriendName) }
    var anniversaryDate by remember { mutableStateOf(prefs.anniversaryDate) }
    var boyBirthday by remember { mutableStateOf(prefs.boyfriendBirthday) }
    var girlBirthday by remember { mutableStateOf(prefs.girlfriendBirthday) }
    var soundEnabled by remember { mutableStateOf(prefs.soundEnabled) }
    var atmosphere by remember { mutableStateOf(prefs.atmosphereMode) }
    var glassIntensity by remember { mutableStateOf(prefs.buttonGlassIntensity) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var tinyCareEnabled by remember { mutableStateOf(prefs.tinyCareEnabled) }
    var enabledCategories by remember { mutableStateOf(prefs.tinyCareCategories) }
    var showPermissionExplanation by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showPermissionExplanation = false
            tinyCareEnabled = true
            com.example.care.TinyCareScheduler.enable(context)
        } else {
            tinyCareEnabled = false
            prefs.tinyCareEnabled = false
            com.example.care.TinyCareScheduler.disable(context)
        }
        onSettingsChanged()
    }

    LaunchedEffect(Unit) {
        if (tinyCareEnabled) {
            val systemAllowed = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            val permissionGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true

            if (!systemAllowed || !permissionGranted) {
                tinyCareEnabled = false
                prefs.tinyCareEnabled = false
                com.example.care.TinyCareScheduler.disable(context)
                onSettingsChanged()
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CozyCream,
        modifier = Modifier.testTag("settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onDismiss() }
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkSlate
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tiny Us Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DarkSlate.copy(alpha = 0.6f)
                    )
                }
            }

            // ── 1. Our World ──
            SettingsCategoryHeader(
                icon = Icons.Default.Favorite,
                title = "Our World",
                subtitle = "Names and special dates for your story together"
            )
            SettingsSectionCard {
                // Names (Max 10 chars)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = boyName,
                        onValueChange = {
                            val trimmed = it.take(10)
                            boyName = trimmed
                            prefs.boyfriendName = trimmed
                            onSettingsChanged()
                        },
                        label = { Text("Boy's Name") },
                        modifier = Modifier.weight(1f).testTag("input_boy_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = girlName,
                        onValueChange = {
                            val trimmed = it.take(10)
                            girlName = trimmed
                            prefs.girlfriendName = trimmed
                            onSettingsChanged()
                        },
                        label = { Text("Girl's Name") },
                        modifier = Modifier.weight(1f).testTag("input_girl_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Anniversary Date
                OutlinedTextField(
                    value = anniversaryDate,
                    onValueChange = {
                        anniversaryDate = it
                        prefs.anniversaryDate = it
                        onSettingsChanged()
                    },
                    label = { Text("Anniversary Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_anniversary_date"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Birthdays (recurring yearly)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = boyBirthday,
                        onValueChange = {
                            boyBirthday = it
                            prefs.boyfriendBirthday = it
                            onSettingsChanged()
                        },
                        label = { Text("Boy's Birthday") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f).testTag("input_boy_bday"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = girlBirthday,
                        onValueChange = {
                            girlBirthday = it
                            prefs.girlfriendBirthday = it
                            onSettingsChanged()
                        },
                        label = { Text("Girl's Birthday") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f).testTag("input_girl_bday"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Secret Gift Box Easter Egg (Tap 5 times to reveal!)
                GiftBoxEasterEgg(
                    onJumpToMomoStall = {
                        onJumpToScene(SceneType.MOMO_STALL)
                    }
                )
            }

            // ── 2. Characters & Wardrobe ──
            SettingsCategoryHeader(
                icon = Icons.Default.Favorite,
                title = "Characters & Wardrobe",
                subtitle = "Sweaters, hoodies & cute ribbons for both characters"
            )
            SettingsSectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepRose.copy(alpha = 0.08f))
                        .clickable {
                            onDismiss()
                            onOpenWardrobe()
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DeepRose.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = DeepRose, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Cottage Wardrobe", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DarkSlate)
                            Text("Hoodies & Outfits", fontSize = 12.sp, color = DarkSlate.copy(alpha = 0.6f))
                        }
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenWardrobe()
                        },
                        modifier = Modifier.testTag("settings_open_wardrobe_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── 3. Atmosphere & Sky ──
            SettingsCategoryHeader(
                icon = Icons.Default.WbSunny,
                title = "Atmosphere & Sky",
                subtitle = "Sync with the real sky or set an intimate mood"
            )
            SettingsSectionCard {
                Column {
                    Text("Sky & Atmosphere:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkSlate)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val modes = listOf("AUTO", "DAY", "SUNSET", "NIGHT")
                        modes.forEach { m ->
                            FilterChip(
                                selected = atmosphere == m,
                                onClick = {
                                    atmosphere = m
                                    prefs.atmosphereMode = m
                                    onSettingsChanged()
                                },
                                label = { Text(m) }
                            )
                        }
                    }
                }
            }

            // ── 4. Sounds & Music ──
            SettingsCategoryHeader(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Sounds & Music",
                subtitle = "Gentle music box lullabies & peaceful nature ambience"
            )
            SettingsSectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFAF7F2))
                        .clickable {
                            soundEnabled = !soundEnabled
                            prefs.soundEnabled = soundEnabled
                            onSettingsChanged()
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (soundEnabled) DeepRose.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                tint = if (soundEnabled) DeepRose else DarkSlate.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Cozy Ambient Lullaby & Audio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DarkSlate)
                            Text(if (soundEnabled) "Gentle music box sounds on" else "Sounds muted", fontSize = 12.sp, color = DarkSlate.copy(alpha = 0.6f))
                        }
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            prefs.soundEnabled = it
                            onSettingsChanged()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DeepRose,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        )
                    )
                }
            }

            // ── 5. Memories & Keepsakes ──
            SettingsCategoryHeader(
                icon = Icons.Default.AutoAwesome,
                title = "Memories & Keepsakes",
                subtitle = "Your love notes, special days & polaroid moments"
            )
            SettingsSectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenMemories()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Our Keepsakes", fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onOpenLoveNotes()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE07A5F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Love Notes", fontSize = 12.5.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenPolaroids()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9184A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tiny Moments Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onOpenDreamJournal,
                        modifier = Modifier.weight(1f).testTag("settings_open_dream_journal_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2D8B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shared Dream Journal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Couple Activities & Connection ──
            SettingsCategoryHeader(
                icon = Icons.Default.Favorite,
                title = "Couple Activities & Connection",
                subtitle = "Adventures, reflections, mini-games & long-distance signals"
            )
            SettingsSectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenDateAdventures()
                        },
                        modifier = Modifier.weight(1f).testTag("settings_date_adventures_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🧺 Date Adventures", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onOpenDailyMoment()
                        },
                        modifier = Modifier.weight(1f).testTag("settings_daily_moment_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRose),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("💭 Daily Moment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenMiniGames()
                        },
                        modifier = Modifier.weight(1f).testTag("settings_mini_games_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🎲 Mini-Games", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onOpenSharedMood()
                        },
                        modifier = Modifier.weight(1f).testTag("settings_shared_mood_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PeachMuted),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🌸 Shared Mood", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = {
                        onDismiss()
                        onOpenLongDistance()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("settings_long_distance_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9184A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💌 Long-Distance Signals", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── 6. Tiny Care Notifications ──
            SettingsCategoryHeader(
                icon = Icons.Default.Favorite,
                title = "Tiny Care",
                subtitle = "Gentle, wholesome offline check-ins for each other"
            )
            // Tiny Care: Wholesome Offline Reminders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.90f))
                    .padding(14.dp)
                    .testTag("tiny_care_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = DeepRose
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Tiny Care", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Gentle, wholesome offline reminders", fontSize = 12.sp, color = DarkSlate.copy(alpha = 0.6f))
                        }
                    }

                    Switch(
                        checked = tinyCareEnabled,
                        onCheckedChange = { willEnable ->
                            if (willEnable) {
                                val needsRuntimePermission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (needsRuntimePermission) {
                                    showPermissionExplanation = true
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val systemAllowed = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                                    if (systemAllowed) {
                                        showPermissionExplanation = true
                                        tinyCareEnabled = true
                                        com.example.care.TinyCareScheduler.enable(context)
                                    } else {
                                        showPermissionExplanation = true
                                    }
                                }
                            } else {
                                showPermissionExplanation = false
                                tinyCareEnabled = false
                                com.example.care.TinyCareScheduler.disable(context)
                            }
                            onSettingsChanged()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DeepRose,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        ),
                        modifier = Modifier.testTag("tiny_care_switch")
                    )
                }

                if (showPermissionExplanation) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notification permission is needed so Tiny Care can deliver quiet offline reminders.",
                        fontSize = 11.5.sp,
                        color = DeepRose,
                        lineHeight = 15.sp
                    )
                }

                if (tinyCareEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8F9FA))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = DarkSlate.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quiet hours: 11:00 PM - 7:00 AM (sleep window)",
                            fontSize = 11.5.sp,
                            color = DarkSlate.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Remind me about:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val categories = com.example.care.TinyCareCategory.values()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (chunk in categories.toList().chunked(2)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (cat in chunk) {
                                    val isSelected = cat.id in enabledCategories
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updated = if (isSelected) {
                                                if (enabledCategories.size > 1) enabledCategories - cat.id else enabledCategories
                                            } else {
                                                enabledCategories + cat.id
                                            }
                                            enabledCategories = updated
                                            prefs.tinyCareCategories = updated
                                            onSettingsChanged()
                                        },
                                        label = { Text(cat.title, fontSize = 11.5.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            com.example.care.TinyCareScheduler.sendTestNotification(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tiny_care_test_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFCE4EC),
                            contentColor = DeepRose
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send preview reminder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Preview reminder does not count toward daily frequency or message history.",
                        fontSize = 10.5.sp,
                        color = DarkSlate.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── 7. Appearance ──
            SettingsCategoryHeader(
                icon = Icons.Default.AutoAwesome,
                title = "Appearance & Controls",
                subtitle = "Frosted glassmorphism intensity for buttons"
            )
            // Button Glassmorphism Setting
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.90f))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = DeepRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Button Glassmorphism",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DarkSlate
                        )
                    }
                    Text(
                        text = "${(glassIntensity * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DeepRose
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Adjust the frosted translucency and shine of buttons",
                    fontSize = 12.sp,
                    color = DarkSlate.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live Preview Row on night-sky backdrop
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2B3044))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Preview",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    val previewTopAlpha = (0.20f + 0.75f * glassIntensity).coerceIn(0.12f, 0.95f)
                    val previewBottomAlpha = (0.08f + 0.55f * glassIntensity).coerceIn(0.06f, 0.75f)
                    val previewFill = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = previewTopAlpha), Color.White.copy(alpha = previewBottomAlpha))
                    )
                    val previewBorderTop = (0.35f + 0.60f * glassIntensity).coerceIn(0.20f, 0.98f)
                    val previewBorderBottom = (0.10f + 0.35f * glassIntensity).coerceIn(0.08f, 0.50f)
                    val previewBorder = BorderStroke(
                        1.dp,
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = previewBorderTop), Color.White.copy(alpha = previewBorderBottom)))
                    )

                    val previewClear = (1f - glassIntensity).coerceIn(0f, 1f)
                    val previewSettingsTint = androidx.compose.ui.graphics.lerp(DarkSlate, Color(0xFFFFF0F5), previewClear)
                    val previewSparkleTint = androidx.compose.ui.graphics.lerp(Color(0xFFE65100), Color(0xFFFFA726), previewClear)
                    val previewHeartTint = androidx.compose.ui.graphics.lerp(DeepRose, Color(0xFFFF6B81), previewClear)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(previewFill)
                                .border(previewBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewClear > 0.15f) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black.copy(alpha = (0.75f * previewClear).coerceIn(0f, 0.85f)), modifier = Modifier.size(16.dp).offset(0.6.dp, 0.6.dp))
                            }
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = previewSparkleTint, modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(previewFill)
                                .border(previewBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewClear > 0.15f) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Black.copy(alpha = (0.75f * previewClear).coerceIn(0f, 0.85f)), modifier = Modifier.size(16.dp).offset(0.6.dp, 0.6.dp))
                            }
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = previewHeartTint, modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(previewFill)
                                .border(previewBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewClear > 0.15f) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Black.copy(alpha = (0.75f * previewClear).coerceIn(0f, 0.85f)), modifier = Modifier.size(16.dp).offset(0.6.dp, 0.6.dp))
                            }
                            Icon(Icons.Default.Settings, contentDescription = null, tint = previewSettingsTint, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = glassIntensity,
                    onValueChange = {
                        glassIntensity = it
                        prefs.buttonGlassIntensity = it
                        onSettingsChanged()
                    },
                    valueRange = 0.10f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = DeepRose,
                        activeTrackColor = DeepRose,
                        inactiveTrackColor = DeepRose.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("button_glassmorphism_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Clear / Subtle (10%)", fontSize = 11.sp, color = DarkSlate.copy(alpha = 0.5f))
                    Text("Frosted (100%)", fontSize = 11.sp, color = DarkSlate.copy(alpha = 0.5f))
                }
            }

            // ── 8. World Exploration & Privacy ──
            SettingsCategoryHeader(
                icon = Icons.Default.Shuffle,
                title = "World & Privacy",
                subtitle = "Scenes exploration and offline privacy guarantee"
            )
            SettingsSectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onOpenScenePicker()
                        },
                        modifier = Modifier.weight(1f).testTag("choose_scene_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Scene", fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = {
                            onReplayScene()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).testTag("replay_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Replay Scene", fontSize = 12.5.sp)
                    }
                }

                // Wholesome offline assurance card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F4F0))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = SageGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tiny Us is 100% offline & private. All names, dates, notes, and memories stay safely on this device.",
                        fontSize = 11.5.sp,
                        color = DarkSlate.copy(alpha = 0.75f),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Dream Journal — keyword engine + dialog
// ─────────────────────────────────────────────────────────────────────────────

/** Maps a typed dream phrase to a canonical theme name and matched keywords. */
fun parseDreamTheme(text: String): Pair<String, List<String>> {
    val lower = text.lowercase()
    val matched = mutableListOf<String>()
    var theme = "FALLBACK"

    // Priority order: first match wins for theme, all matches collected for keywords
    if (lower.containsAny("japan", "tokyo", "kyoto", "torii", "sakura", "cherry blossom")) {
        matched += lower.matchingKeywords("japan", "tokyo", "kyoto", "torii", "sakura", "cherry blossom")
        theme = "JAPAN"
    }
    if (lower.containsAny("norway", "aurora", "northern lights", "fjord", "nordic")) {
        matched += lower.matchingKeywords("norway", "aurora", "northern lights", "fjord", "nordic")
        if (theme == "FALLBACK") theme = "NORWAY"
    }
    if (lower.containsAny("ocean", "sea", "beach", "waves", "water", "coast")) {
        matched += lower.matchingKeywords("ocean", "sea", "beach", "waves", "water", "coast")
        if (theme == "FALLBACK") theme = "OCEAN"
    }
    if (lower.containsAny("flying", "fly", "float", "floating", "sky", "clouds", "weightless")) {
        matched += lower.matchingKeywords("flying", "fly", "float", "floating", "sky", "clouds", "weightless")
        if (theme == "FALLBACK") theme = "FLYING"
    }
    if (lower.containsAny("stars", "star", "space", "galaxy", "stargaze", "cosmos", "universe")) {
        matched += lower.matchingKeywords("stars", "star", "space", "galaxy", "stargaze", "cosmos", "universe")
        if (theme == "FALLBACK") theme = "STARS"
    }
    if (lower.containsAny("forest", "woods", "nature", "trees", "jungle", "emerald")) {
        matched += lower.matchingKeywords("forest", "woods", "nature", "trees", "jungle", "emerald")
        if (theme == "FALLBACK") theme = "FOREST"
    }
    if (lower.containsAny("home", "cottage", "cozy", "warm", "hearth", "fireplace", "blanket")) {
        matched += lower.matchingKeywords("home", "cottage", "cozy", "warm", "hearth", "fireplace", "blanket")
        if (theme == "FALLBACK") theme = "HOME"
    }
    if (lower.containsAny("rain", "storm", "thunder", "rainy", "drizzle", "monsoon")) {
        matched += lower.matchingKeywords("rain", "storm", "thunder", "rainy", "drizzle", "monsoon")
        if (theme == "FALLBACK") theme = "RAIN"
    }
    if (lower.containsAny("city", "night out", "street", "lights", "urban", "skyline")) {
        matched += lower.matchingKeywords("city", "night out", "street", "lights", "urban", "skyline")
        if (theme == "FALLBACK") theme = "CITY"
    }
    if (lower.containsAny("cake", "sweet", "momo", "food", "picnic", "feast", "dessert")) {
        matched += lower.matchingKeywords("cake", "sweet", "momo", "food", "picnic", "feast", "dessert")
        if (theme == "FALLBACK") theme = "SWEET"
    }

    return Pair(theme, matched.distinct())
}

private fun String.containsAny(vararg words: String): Boolean = words.any { word ->
    if (word.contains(' ')) this.contains(word)
    else Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(this)
}
private fun String.matchingKeywords(vararg words: String): List<String> = words.filter { word ->
    if (word.contains(' ')) this.contains(word)
    else Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(this)
}

@Composable
fun DreamJournalDialog(
    prefs: com.example.data.PreferencesManager,
    onDismiss: () -> Unit,
    onVisualizeDream: (theme: String, text: String) -> Unit
) {
    var dreamText by remember { mutableStateOf("") }
    var dreamsList by remember { mutableStateOf(prefs.getDreamEntries()) }
    var errorText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("dream_journal_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = CozyCream,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Shared Dream Journal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Color(0xFF4A0070)
                        )
                        Text(
                            text = "Type a dream and watch it come alive",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate.copy(alpha = 0.5f))
                    }
                }

                // Dream input
                OutlinedTextField(
                    value = dreamText,
                    onValueChange = {
                        dreamText = it
                        errorText = ""
                    },
                    label = { Text("Describe your dream...") },
                    placeholder = { Text("e.g. We walked under cherry blossoms in Japan", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorText.isNotEmpty()) {
                    Text(errorText, fontSize = 11.sp, color = Color(0xFFD32F2F))
                }

                // Visualize button
                Button(
                    onClick = {
                        val trimmed = dreamText.trim()
                        if (trimmed.isEmpty()) {
                            errorText = "Please write a few words about your dream first."
                            return@Button
                        }
                        val (theme, keywords) = parseDreamTheme(trimmed)
                        val entry = com.example.data.DreamEntry(
                            id = java.util.UUID.randomUUID().toString(),
                            text = trimmed,
                            matchedKeywords = keywords,
                            dreamTheme = theme,
                            timestamp = System.currentTimeMillis()
                        )
                        prefs.addDreamEntry(entry)
                        dreamsList = prefs.getDreamEntries()
                        onVisualizeDream(theme, trimmed)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2D8B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Visualize Dream", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Dream history
                if (dreamsList.isNotEmpty()) {
                    Text(
                        text = "Past Dreams",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4A0070)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dreamsList.take(10).forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.text,
                                            fontSize = 12.sp,
                                            color = DarkSlate,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            val themeLabel = when (entry.dreamTheme) {
                                                "JAPAN" -> "Japan"
                                                "NORWAY" -> "Aurora"
                                                "OCEAN" -> "Ocean"
                                                "FLYING" -> "Flying"
                                                "STARS" -> "Stars"
                                                "FOREST" -> "Forest"
                                                "HOME" -> "Cozy"
                                                "RAIN" -> "Rain"
                                                "CITY" -> "City"
                                                "SWEET" -> "Sweet"
                                                else -> "Dream"
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFEDE7F6)
                                            ) {
                                                Text(
                                                    text = themeLabel,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF4A0070),
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                onVisualizeDream(entry.dreamTheme, entry.text)
                                                onDismiss()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = "Relive dream",
                                                tint = Color(0xFF7B2D8B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                prefs.deleteDreamEntry(entry.id)
                                                dreamsList = prefs.getDreamEntries()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete dream",
                                                tint = DarkSlate.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Close", color = DarkSlate.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}

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
private fun RomanticSurpriseDialog(
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
                    text = "A Little Guide for My Girl (How to Play)",
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
private fun GuideItem(
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

@Composable
fun TbSecretDialog(
    onDismiss: () -> Unit
) = SecretKeepsakeDialog(onDismiss)

@Composable
fun SecretKeepsakeDialog(
    onDismiss: () -> Unit,
    prefs: com.example.data.PreferencesManager? = null
) {
    val profile = com.example.data.ProfileManager.getProfile()
    val title = prefs?.secretCode?.ifBlank { null } ?: profile.secretCodeTitle.ifBlank { profile.boyName }
    val subtitle = profile.secretCodeSubtitle.ifBlank { "A keepsake from the heart" }
    val body = prefs?.secretCodeBody?.ifBlank { null } ?: profile.secretCodeBody.ifBlank {
        "Not in money or gold,\nbut in endless love, quiet cuddles,\nand a heart that belongs\nentirely to you."
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFFFFDFB),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("tb_secret_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Intimate header
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 2.sp,
                    color = DarkSlate
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    color = DeepRose,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Subtle minimalist hair-thin separator
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(Color(0xFFE8E5E0))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = body,
                    fontSize = 13.5.sp,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 22.sp,
                    color = DarkSlate.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Minimalist button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF7F4EF))
                        .padding(horizontal = 24.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Close",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkSlate.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun MusicPlayerDialog(
    audio: AmbientAudio,
    engine: SceneEngine,
    onEnableAudio: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isPlaying = audio.musicBoxState == MusicBoxState.PLAYING

    // Subtle vinyl rotation animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )
    val vinylAngle = if (isPlaying) rotation else 0f

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFAFAFA),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header: Minimal Title + Shared Earphones Pill + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Music Box",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "listening together",
                            fontSize = 11.sp,
                            color = DarkSlate.copy(alpha = 0.5f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Minimalist Earphones pill toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (engine.earphonesActive) Color(0xFFFFE8EC) else Color(0xFFF1F3F5)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (engine.earphonesActive) DeepRose.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    val next = !engine.earphonesActive
                                    engine.setEarphones(next)
                                    if (next) {
                                        onEnableAudio?.invoke()
                                        audio.isEnabled = true
                                        audio.playHeartChime()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "Earphones",
                                    tint = if (engine.earphonesActive) DeepRose else DarkSlate.copy(alpha = 0.55f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (engine.earphonesActive) "Shared" else "Earphones",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (engine.earphonesActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (engine.earphonesActive) DeepRose else DarkSlate.copy(alpha = 0.65f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = DarkSlate.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Now Playing Hero Card (Warm, Minimalist, Airy)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF0ECE8))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Spinning Vinyl graphic
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .graphicsLayer { rotationZ = vinylAngle },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radius = size.minDimension / 2f
                                    // Outer Vinyl Disc
                                    drawCircle(
                                        color = Color(0xFF23252B),
                                        radius = radius
                                    )
                                    // Groove lines
                                    drawCircle(
                                        color = Color(0xFF32353E),
                                        radius = radius * 0.82f,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color(0xFF383B45),
                                        radius = radius * 0.64f,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                    )
                                    // Center label
                                    drawCircle(
                                        color = DeepRose,
                                        radius = radius * 0.38f
                                    )
                                    // Spindle hole
                                    drawCircle(
                                        color = Color.White,
                                        radius = radius * 0.12f
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Song Metadata
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = audio.currentSong.title,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkSlate,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (audio.currentSong.rawResId != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFFEFF1), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "MP3",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepRose
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = audio.currentSong.artist,
                                    fontSize = 12.sp,
                                    color = DarkSlate.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = audio.currentSong.vibe,
                                    fontSize = 10.5.sp,
                                    color = DarkSlate.copy(alpha = 0.45f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Playback Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    onEnableAudio?.invoke()
                                    audio.isEnabled = true
                                    audio.previousSong()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Song",
                                    tint = DarkSlate.copy(alpha = 0.75f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            // Play/Pause circular accent button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(DeepRose)
                                    .clickable {
                                        onEnableAudio?.invoke()
                                        audio.isEnabled = true
                                        when (audio.musicBoxState) {
                                            MusicBoxState.PLAYING -> audio.pauseMusic()
                                            MusicBoxState.PAUSED -> audio.resumeMusic()
                                            MusicBoxState.STOPPED -> audio.playSong(audio.currentSong)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            IconButton(
                                onClick = {
                                    onEnableAudio?.invoke()
                                    audio.isEnabled = true
                                    audio.nextSong()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Song",
                                    tint = DarkSlate.copy(alpha = 0.75f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Track List Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAYLIST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate.copy(alpha = 0.45f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${audio.playlist.size} songs",
                        fontSize = 11.sp,
                        color = DarkSlate.copy(alpha = 0.4f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Minimalist Tracks List (Clean, borderless, airy)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(audio.playlist) { song ->
                        val isSelected = song.id == audio.currentSong.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFFFFEFF2) else Color.Transparent
                                )
                                .clickable {
                                    onEnableAudio?.invoke()
                                    audio.isEnabled = true
                                    audio.playSong(song)
                                }
                                .padding(horizontal = 10.dp, vertical = 9.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected && isPlaying) Icons.Default.PlayArrow else Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = if (isSelected) DeepRose else DarkSlate.copy(alpha = 0.25f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = song.title,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) DeepRose else DarkSlate,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (song.rawResId != null) {
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "MP3",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) DeepRose.copy(alpha = 0.8f) else DarkSlate.copy(alpha = 0.35f)
                                                )
                                            }
                                        }
                                        Text(
                                            text = song.artist,
                                            fontSize = 10.5.sp,
                                            color = DarkSlate.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Text(
                                        text = when (audio.musicBoxState) {
                                            MusicBoxState.PLAYING -> "Playing"
                                            MusicBoxState.PAUSED -> "Paused"
                                            MusicBoxState.STOPPED -> "Selected"
                                        },
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepRose
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getIconForType(type: String): ImageVector = when (type) {
    "flower" -> Icons.Default.LocalFlorist
    "tree" -> Icons.Default.Park
    "cooking" -> Icons.Default.Restaurant
    "couch" -> Icons.Default.Weekend
    "stars" -> Icons.Default.Nightlight
    else -> Icons.Default.Favorite
}

private fun getSceneIcon(sc: SceneType): ImageVector = when (sc) {
    SceneType.FLOWER -> Icons.Default.LocalFlorist
    SceneType.UNDER_TREE -> Icons.Default.Park
    SceneType.COOKING -> Icons.Default.Restaurant
    SceneType.SLEEP -> Icons.Default.Weekend
    SceneType.WALK -> Icons.Default.Nightlight
    SceneType.LOOKING -> Icons.Default.Favorite
    SceneType.MOMO_STALL -> Icons.Default.Restaurant
    SceneType.EVENING_RIDE -> Icons.Default.TwoWheeler
    SceneType.COZY_LOFT -> Icons.Default.Weekend
    SceneType.RAINY_CAFE -> Icons.Default.Restaurant
    SceneType.SUNROOM -> Icons.Default.LocalFlorist
}

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
private fun CalendarPixelMarker(type: SpecialMemoryType) {
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
private fun LegendItem(icon: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, fontSize = 10.sp, color = DarkSlate.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
    }
}

data class WardrobeItem(
    val id: Int,
    val name: String,
    val tag: String,
    val description: String,
    val primaryColor: Color,
    val accentColor: Color,
    val isHoodie: Boolean = false
)

data class AccessoryItem(
    val id: Int,
    val name: String,
    val description: String,
    val iconType: String
)

@Composable
fun WardrobeDialog(
    currentDressIndex: Int,
    girlfriendName: String,
    boyfriendName: String,
    onSelectDress: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    WardrobeDialog(
        currentGirlOutfitIndex = currentDressIndex,
        currentGirlAccessoryIndex = 0,
        currentBoyOutfitIndex = 0,
        currentBoyAccessoryIndex = 0,
        girlfriendName = girlfriendName,
        boyfriendName = boyfriendName,
        onSelectGirlOutfit = onSelectDress,
        onSelectGirlAccessory = {},
        onSelectBoyOutfit = {},
        onSelectBoyAccessory = {},
        onDismiss = onDismiss
    )
}

@Composable
fun WardrobeDialog(
    currentGirlOutfitIndex: Int,
    currentGirlAccessoryIndex: Int,
    currentBoyOutfitIndex: Int,
    currentBoyAccessoryIndex: Int,
    girlfriendName: String,
    boyfriendName: String,
    onSelectGirlOutfit: (Int) -> Unit,
    onSelectGirlAccessory: (Int) -> Unit,
    onSelectBoyOutfit: (Int) -> Unit,
    onSelectBoyAccessory: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Girl, 1: Boy
    var filterHoodiesOnly by remember { mutableStateOf(false) }

    var selectedGirlOutfit by remember(currentGirlOutfitIndex) { mutableStateOf(currentGirlOutfitIndex) }
    var selectedGirlAccessory by remember(currentGirlAccessoryIndex) { mutableStateOf(currentGirlAccessoryIndex) }
    var selectedBoyOutfit by remember(currentBoyOutfitIndex) { mutableStateOf(currentBoyOutfitIndex) }
    var selectedBoyAccessory by remember(currentBoyAccessoryIndex) { mutableStateOf(currentBoyAccessoryIndex) }

    val girlDresses = remember(girlfriendName, boyfriendName) {
        listOf(
            WardrobeItem(
                id = 0,
                name = "Strawberry Cream Sundress",
                tag = "Signature Classic",
                description = "Soft blush-pink sweater and rosy pleated skirt. The timeless outfit $boyfriendName fell in love with.",
                primaryColor = Color(0xFFF5CAC3),
                accentColor = Color(0xFFD6587A)
            ),
            WardrobeItem(
                id = 1,
                name = "Lavender Dream Wrap Dress",
                tag = "Garden Stroll",
                description = "Delicate lilac petals woven into flowing silk. Smells like blooming lavender and gentle morning breezes.",
                primaryColor = Color(0xFFE8D7F1),
                accentColor = Color(0xFF9D4EDD)
            ),
            WardrobeItem(
                id = 2,
                name = "Emerald Velvet Romance",
                tag = "Candlelit Evening",
                description = "Deep forest emerald velvet with shimmering dark green folds. Made for rooftop starlight and warm slow dances.",
                primaryColor = Color(0xFF74C69D),
                accentColor = Color(0xFF2D6A4F)
            ),
            WardrobeItem(
                id = 3,
                name = "Lemon Sunshine Picnic Dress",
                tag = "Meadow Picnic",
                description = "Cheerful lemon-yellow linen with sweet honey accents. Brings warm golden sunshine wherever $girlfriendName walks.",
                primaryColor = Color(0xFFFFF3B0),
                accentColor = Color(0xFFE9C46A)
            ),
            WardrobeItem(
                id = 4,
                name = "Midnight Starlight Gown",
                tag = "Midnight Date",
                description = "Deep midnight blue with celestial starlight highlights. For whispering secrets under constellations.",
                primaryColor = Color(0xFF4A6FA5),
                accentColor = Color(0xFF1E3A8A)
            ),
            WardrobeItem(
                id = 5,
                name = "Mint Macaron Tea Dress",
                tag = "Cozy Cafe",
                description = "Sweet pastel mint chiffon as light as a daydream. Perfect for sipping steaming tea by the cottage window.",
                primaryColor = Color(0xFFC3DBD0),
                accentColor = Color(0xFF6B9080)
            ),
            WardrobeItem(
                id = 6,
                name = "$boyfriendName's Stolen Oversized Flannel",
                tag = "Stolen with Love",
                description = "Comfortable deep blue flannel shirt. Originally belonged to $boyfriendName, but $girlfriendName claimed it forever because it smells like him.",
                primaryColor = Color(0xFF457B9D),
                accentColor = Color(0xFF1D3557)
            ),
            WardrobeItem(
                id = 7,
                name = "Blush Rose Cropped Hoodie",
                tag = "Cozy Streetwear",
                description = "Soft blush-rose cropped knit hoodie with a flared pleated skirt. Wonderfully cozy for cool afternoon strolls.",
                primaryColor = Color(0xFFF4ACB7),
                accentColor = Color(0xFFFFCAD4),
                isHoodie = true
            ),
            WardrobeItem(
                id = 8,
                name = "Sage & Cream Colorblock Hoodie",
                tag = "Forest Breeze",
                description = "Earthy sage and fresh cream colorblock hoodie with a linen skirt. Feels like a quiet walk through misty pines.",
                primaryColor = Color(0xFF84A98C),
                accentColor = Color(0xFF52796F),
                isHoodie = true
            ),
            WardrobeItem(
                id = 9,
                name = "Lavender Cloud Oversized Hoodie",
                tag = "Cloud Cozy",
                description = "Fluffy lavender fleece oversized hoodie with denim skirt. Like wrapping up inside a warm, sweet-scented cloud.",
                primaryColor = Color(0xFFD8BBFF),
                accentColor = Color(0xFF3D5A80),
                isHoodie = true
            ),
            WardrobeItem(
                id = 10,
                name = "Buttercream Star Shimmer Hoodie",
                tag = "Golden Glow",
                description = "Sunny buttercream hoodie with warm honey shimmer and golden accents. Radiates gentle warmth and smiles.",
                primaryColor = Color(0xFFFFF1C5),
                accentColor = Color(0xFFFFD166),
                isHoodie = true
            )
        )
    }

    val boyOutfits = remember(girlfriendName, boyfriendName) {
        listOf(
            WardrobeItem(
                id = 0,
                name = "Classic Spruce Knit & Navy Pants",
                tag = "Signature Everyday",
                description = "$boyfriendName's iconic spruce green sweater paired with relaxed navy trousers. Trusty, warm, and familiar.",
                primaryColor = Color(0xFF2D6A4F),
                accentColor = Color(0xFF1B4332),
                isHoodie = false
            ),
            WardrobeItem(
                id = 1,
                name = "White & Emerald Varsity Hoodie",
                tag = "Varsity Campus",
                description = "Clean white hoodie with deep emerald varsity stripes, kangaroo pouch, and khaki chinos. Sharp and sporty.",
                primaryColor = Color(0xFFF8F9FA),
                accentColor = Color(0xFF2D6A4F),
                isHoodie = true
            ),
            WardrobeItem(
                id = 2,
                name = "Charcoal Streetwear Zip Hoodie",
                tag = "Urban Evening",
                description = "Heavy charcoal zip-up hoodie over dark indigo denim. Modern, comfortable, and effortlessly cool.",
                primaryColor = Color(0xFF343A40),
                accentColor = Color(0xFF495057),
                isHoodie = true
            ),
            WardrobeItem(
                id = 3,
                name = "Oatmeal Cloud Oversized Hoodie",
                tag = "Weekend Comfort",
                description = "Ultra-soft oatmeal heather oversized hoodie with comfortable slate cargo trousers. Maximum cozy lounging.",
                primaryColor = Color(0xFFEDE0D4),
                accentColor = Color(0xFFB08968),
                isHoodie = true
            ),
            WardrobeItem(
                id = 4,
                name = "Midnight Starlight Graphic Hoodie",
                tag = "Stargazing Date",
                description = "Deep starlight navy hoodie with celestial accents over black denim. Matches $girlfriendName's evening starlight look.",
                primaryColor = Color(0xFF1E293B),
                accentColor = Color(0xFF64748B),
                isHoodie = true
            )
        )
    }

    val accessories = remember {
        listOf(
            AccessoryItem(
                id = 0,
                name = "Natural Look",
                description = "No headwear or neck accessory.",
                iconType = "none"
            ),
            AccessoryItem(
                id = 1,
                name = "Cozy Beanie",
                description = "Ribbed knit beanie with fluffy pom-pom.",
                iconType = "beanie"
            ),
            AccessoryItem(
                id = 2,
                name = "Wool Scarf",
                description = "Warm chunky wool scarf with gentle fringe.",
                iconType = "scarf"
            ),
            AccessoryItem(
                id = 3,
                name = "Baseball Cap",
                description = "Casual streetwear twill cap with forward visor.",
                iconType = "cap"
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("wardrobe_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = CozyCream,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Cottage Wardrobe",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFFC9184A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Outfits & accessories for both of you",
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Serif,
                    color = DarkSlate.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // His / Hers Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEDE0D4).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                            .testTag("wardrobe_tab_girl"),
                        shape = RoundedCornerShape(11.dp),
                        color = if (selectedTab == 0) DeepRose else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$girlfriendName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color.White else DarkSlate.copy(alpha = 0.75f)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                            .testTag("wardrobe_tab_boy"),
                        shape = RoundedCornerShape(11.dp),
                        color = if (selectedTab == 1) DeepRose else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$boyfriendName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) Color.White else DarkSlate.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable main content
                val currentAccessory = if (selectedTab == 0) selectedGirlAccessory else selectedBoyAccessory
                val currentOutfit = if (selectedTab == 0) selectedGirlOutfit else selectedBoyOutfit
                val rawOutfitList = if (selectedTab == 0) girlDresses else boyOutfits
                val outfitList = if (filterHoodiesOnly) rawOutfitList.filter { it.isHoodie } else rawOutfitList

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Accessories Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Accessories",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Text(
                            text = if (selectedTab == 1) "Glasses stay on" else "Layers over outfits",
                            fontSize = 10.sp,
                            color = DarkSlate.copy(alpha = 0.6f)
                        )
                    }

                    // Accessories Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accessories.forEach { acc ->
                            val isAccWearing = (currentAccessory == acc.id)
                            Surface(
                                onClick = {
                                    if (selectedTab == 0) {
                                        selectedGirlAccessory = acc.id
                                        onSelectGirlAccessory(acc.id)
                                    } else {
                                        selectedBoyAccessory = acc.id
                                        onSelectBoyAccessory(acc.id)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAccWearing) Color(0xFFFFECEF) else Color.White,
                                border = BorderStroke(
                                    width = if (isAccWearing) 1.5.dp else 1.dp,
                                    color = if (isAccWearing) DeepRose else Color(0xFFE9ECEF)
                                ),
                                modifier = Modifier.testTag("wardrobe_accessory_${acc.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Canvas(modifier = Modifier.size(18.dp)) {
                                        val p = size.width / 12f
                                        when (acc.iconType) {
                                            "beanie" -> {
                                                val pomCol = if (selectedTab == 0) Color(0xFFFFF0F3) else Color(0xFFE9C46A)
                                                val beanCol = if (selectedTab == 0) Color(0xFFE8998D) else Color(0xFF264653)
                                                val brimCol = if (selectedTab == 0) Color(0xFFD6587A) else Color(0xFF1B4332)
                                                drawRect(pomCol, Offset(5 * p, p), Size(2 * p, 2 * p))
                                                drawRect(beanCol, Offset(3 * p, 3 * p), Size(6 * p, 4 * p))
                                                drawRect(brimCol, Offset(2 * p, 7 * p), Size(8 * p, 2 * p))
                                            }
                                            "scarf" -> {
                                                val scCol = if (selectedTab == 0) Color(0xFFFFB5A7) else Color(0xFFE9C46A)
                                                val scDk = if (selectedTab == 0) Color(0xFFFFF0F3) else Color(0xFFD4A373)
                                                drawRect(scCol, Offset(2 * p, 3 * p), Size(8 * p, 3 * p))
                                                drawRect(scDk, Offset(6 * p, 6 * p), Size(3 * p, 4 * p))
                                            }
                                            "cap" -> {
                                                val capCol = if (selectedTab == 0) Color(0xFF6B9080) else Color(0xFF1D3557)
                                                val brimCol = if (selectedTab == 0) Color(0xFF4E6E60) else Color(0xFF0F172A)
                                                drawRect(capCol, Offset(3 * p, 3 * p), Size(6 * p, 4 * p))
                                                drawRect(brimCol, Offset(6 * p, 7 * p), Size(5 * p, 2 * p))
                                            }
                                            else -> {
                                                drawCircle(
                                                    color = Color(0xFFADB5BD),
                                                    radius = 4.5f * p,
                                                    center = Offset(6 * p, 6 * p),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f * p)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = acc.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isAccWearing) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAccWearing) DeepRose else DarkSlate
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Outfits Section Header with filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Dresses & Hoodies" else "Sweaters & Hoodies",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = { filterHoodiesOnly = false },
                                shape = RoundedCornerShape(8.dp),
                                color = if (!filterHoodiesOnly) DeepRose.copy(alpha = 0.15f) else Color.White,
                                border = BorderStroke(1.dp, if (!filterHoodiesOnly) DeepRose else Color(0xFFE9ECEF))
                            ) {
                                Text(
                                    text = "All",
                                    fontSize = 10.sp,
                                    fontWeight = if (!filterHoodiesOnly) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!filterHoodiesOnly) DeepRose else DarkSlate.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                onClick = { filterHoodiesOnly = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (filterHoodiesOnly) DeepRose.copy(alpha = 0.15f) else Color.White,
                                border = BorderStroke(1.dp, if (filterHoodiesOnly) DeepRose else Color(0xFFE9ECEF))
                            ) {
                                Text(
                                    text = "Hoodies Only",
                                    fontSize = 10.sp,
                                    fontWeight = if (filterHoodiesOnly) FontWeight.Bold else FontWeight.Normal,
                                    color = if (filterHoodiesOnly) DeepRose else DarkSlate.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Outfits List
                    outfitList.forEach { item ->
                        val isWearing = (currentOutfit == item.id)
                        val onWearThisOutfit = {
                            if (selectedTab == 0) {
                                selectedGirlOutfit = item.id
                                onSelectGirlOutfit(item.id)
                            } else {
                                selectedBoyOutfit = item.id
                                onSelectBoyOutfit(item.id)
                            }
                        }
                        Surface(
                            onClick = onWearThisOutfit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wardrobe_item_${item.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isWearing) Color(0xFFFFECEF) else Color.White,
                            border = BorderStroke(
                                width = if (isWearing) 1.5.dp else 1.dp,
                                color = if (isWearing) DeepRose else Color(0xFFE9ECEF)
                            ),
                            shadowElevation = if (isWearing) 2.dp else 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pixel Mini Preview Icon
                                Surface(
                                    modifier = Modifier.size(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = item.primaryColor.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.4f))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val p = size.width / 16f
                                        // Hanger
                                        drawRect(Color(0xFFFFD166), Offset(6 * p, 2 * p), Size(4 * p, p))
                                        drawRect(Color(0xFF8C6D37), Offset(7.5f * p, p), Size(p, p))

                                        if (selectedTab == 0) {
                                            // Girl outfit / dress / hoodie preview
                                            drawRect(item.primaryColor, Offset(5 * p, 3.5f * p), Size(6 * p, 4 * p))
                                            drawRect(item.accentColor, Offset(6 * p, 3.5f * p), Size(4 * p, 1.2f * p))
                                            if (item.isHoodie) {
                                                // Kangaroo pouch & drawstrings
                                                drawRect(item.accentColor, Offset(5.5f * p, 5.5f * p), Size(5 * p, 2 * p))
                                                drawRect(item.accentColor, Offset(6.2f * p, 4.5f * p), Size(0.7f * p, 2 * p))
                                                drawRect(item.accentColor, Offset(9.1f * p, 4.5f * p), Size(0.7f * p, 2 * p))
                                                // Pleated skirt below
                                                drawRect(item.accentColor, Offset(4 * p, 7.5f * p), Size(8 * p, 6.5f * p))
                                            } else {
                                                // Flared Skirt
                                                drawRect(item.accentColor, Offset(4 * p, 7.5f * p), Size(8 * p, 6.5f * p))
                                                drawRect(item.primaryColor, Offset(4 * p, 13 * p), Size(8 * p, 1.2f * p))
                                            }
                                        } else {
                                            // Boy outfit / sweater / hoodie preview
                                            drawRect(item.primaryColor, Offset(4.5f * p, 3.5f * p), Size(7 * p, 6 * p))
                                            drawRect(item.accentColor, Offset(6 * p, 3.5f * p), Size(4 * p, 1.2f * p))
                                            if (item.isHoodie) {
                                                drawRect(item.accentColor, Offset(5.5f * p, 6.5f * p), Size(5 * p, 2.5f * p))
                                                drawRect(item.accentColor, Offset(6.2f * p, 4.5f * p), Size(0.7f * p, 2.5f * p))
                                                drawRect(item.accentColor, Offset(9.1f * p, 4.5f * p), Size(0.7f * p, 2.5f * p))
                                            }
                                            // Trousers
                                            drawRect(item.accentColor, Offset(5 * p, 9.5f * p), Size(3 * p, 5 * p))
                                            drawRect(item.accentColor, Offset(8 * p, 9.5f * p), Size(3 * p, 5 * p))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif,
                                            color = DarkSlate,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (item.isHoodie) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = DeepRose.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.5.dp, DeepRose.copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        text = "HOODIE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DeepRose,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = item.accentColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = item.tag,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = item.accentColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = item.description,
                                        fontSize = 11.sp,
                                        color = DarkSlate.copy(alpha = 0.7f),
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isWearing) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DeepRose.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, DeepRose.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "Wearing Now",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepRose,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = onWearThisOutfit,
                                            colors = ButtonDefaults.buttonColors(containerColor = item.accentColor),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Wear Outfit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wardrobe_close_button")
                ) {
                    Text("Close Wardrobe", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// POLAROID CAPTURE OVERLAY
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen overlay shown immediately after the heart button captures a frame.
 *
 * 1. Shutter Flash (0–180 ms): Brief white camera flash.
 * 2. Polaroid Landing: Card drops smoothly from above with spring physics.
 * 3. Keepsake View: Stays visible until the user taps "Keep", taps outside,
 *    or clicks "View Collection".
 */
@Composable
fun PolaroidCaptureOverlay(
    bitmap: Bitmap,
    memory: PolaroidMemory,
    polaroidManager: PolaroidManager,
    audio: AmbientAudio? = null,
    onDismiss: () -> Unit,
    onOpenGallery: () -> Unit
) {
    var hasFlashed by remember { mutableStateOf(false) }
    var cardVisible by remember { mutableStateOf(false) }
    var savedToDevice by remember { mutableStateOf(false) }

    // Flash alpha
    val flashAlpha by animateFloatAsState(
        targetValue = if (!hasFlashed) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "flash_alpha"
    )

    // Card drop offset
    val cardOffsetDp by animateIntAsState(
        targetValue = if (cardVisible) 0 else -520,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_offset"
    )

    LaunchedEffect(Unit) {
        delay(120)
        hasFlashed = true
        delay(60)
        cardVisible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Darkened backdrop (visible when card is dropped)
            if (cardVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                )
            }

            // ── 1. Shutter Flash ─────────────────────────────────────────────────
            if (flashAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = flashAlpha }
                        .background(Color.White)
                )
            }

            // ── 2. Polaroid Card & Actions ───────────────────────────────────────
            if (cardVisible) {
                Column(
                    modifier = Modifier
                        .offset(y = cardOffsetDp.dp)
                        .fillMaxWidth(0.84f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume click to prevent background dismiss */ }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Complete Polaroid Card
                    PolaroidCard(
                        bitmap = bitmap,
                        memory = memory,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons row below Polaroid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Save to Device option (prominent primary button)
                        Surface(
                            onClick = {
                                val success = polaroidManager.saveToDeviceGallery(bitmap, memory.title)
                                if (success) {
                                    savedToDevice = true
                                    audio?.playHeartChime()
                                }
                            },
                            shape = RoundedCornerShape(22.dp),
                            color = if (savedToDevice) Color(0xFF2D6A4F) else DeepRose,
                            shadowElevation = 4.dp,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (savedToDevice) Icons.Default.Check else Icons.Default.Download,
                                    contentDescription = "Save to Device",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (savedToDevice) "Saved to Photos ♥" else "Save to Device",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        // 2. All Memories button
                        Surface(
                            onClick = onOpenGallery,
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White.copy(alpha = 0.94f),
                            shadowElevation = 3.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Memories",
                                    tint = DeepRose,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Memories",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkSlate,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Done / dismiss button
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single Polaroid card composable – authentic instant film proportions,
 * clean white frame, scene photograph, and cozy handwritten-style caption area.
 */
@Composable
fun PolaroidCard(
    bitmap: Bitmap,
    memory: PolaroidMemory,
    modifier: Modifier = Modifier,
    showDeleteButton: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val isCompleteCard = remember(bitmap) {
        val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
        ratio in 1.2f..1.55f
    }

    Box(modifier = modifier) {
        // Realistic Polaroid multi-layer shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 6.dp)
                .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
        )

        if (isCompleteCard) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1080f / 1440f),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFAF8F5),
                shadowElevation = 2.dp
            ) {
                Image(
                    painter = BitmapPainter(imageBitmap),
                    contentDescription = memory.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // White Polaroid frame for raw screenshot fallbacks
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFCFBF9), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(6.dp))
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Photo Area: Classic 4:5 aspect ratio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, Color(0xFFE8E5DF), RoundedCornerShape(3.dp))
                ) {
                    Image(
                        painter = BitmapPainter(imageBitmap),
                        contentDescription = memory.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Autogenerated Title — Warm serif aesthetic
                Text(
                    text = memory.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Scene name + Date line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${memory.sceneName}  •  ${memory.date}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                if (memory.time.isNotBlank()) {
                    Text(
                        text = memory.time,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Delete button (shown when in gallery full view)
        if (showDeleteButton && onDelete != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DeepRose)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete memory",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// POLAROID GALLERY DIALOG
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Shows all saved Polaroid memories in a scrollable 2-column grid.
 * Tapping a card opens it full-screen with options to Save to Device, Go Back, or Delete.
 */
@Composable
fun PolaroidGalleryDialog(
    polaroidManager: PolaroidManager,
    audio: AmbientAudio? = null,
    onDismiss: () -> Unit
) {
    var polaroids by remember { mutableStateOf(polaroidManager.getPolaroids()) }
    var selectedMemory by remember { mutableStateOf<PolaroidMemory?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Full-screen single card inspection
    if (selectedMemory != null && selectedBitmap != null) {
        Dialog(onDismissRequest = { selectedMemory = null; selectedBitmap = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { selectedMemory = null; selectedBitmap = null }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume click to prevent background dismiss */ }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PolaroidCard(
                        bitmap = selectedBitmap!!,
                        memory = selectedMemory!!,
                        showDeleteButton = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    var savedInInspector by remember(selectedMemory!!.id) { mutableStateOf(false) }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Back button
                        Surface(
                            onClick = {
                                selectedMemory = null
                                selectedBitmap = null
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.95f),
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = DarkSlate,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Back",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkSlate
                                )
                            }
                        }

                        // 2. Save to Device button
                        Surface(
                            onClick = {
                                val success = polaroidManager.saveToDeviceGallery(
                                    selectedBitmap!!,
                                    selectedMemory!!.title
                                )
                                if (success) {
                                    savedInInspector = true
                                    audio?.playHeartChime()
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (savedInInspector) Color(0xFF2D6A4F) else DeepRose,
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (savedInInspector) Icons.Default.Check else Icons.Default.Download,
                                    contentDescription = "Save to Device",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (savedInInspector) "Saved ♥" else "Save to Device",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // 3. Delete button
                        Surface(
                            onClick = {
                                polaroidManager.deletePolaroid(selectedMemory!!.id)
                                polaroids = polaroidManager.getPolaroids()
                                selectedMemory = null
                                selectedBitmap = null
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.95f),
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFB00020),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(24.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Polaroid Memories",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = DarkSlate
                        )
                        Text(
                            text = if (polaroids.isEmpty()) "No moments captured yet" else "${polaroids.size} moments captured",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.55f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (polaroids.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = DeepRose.copy(alpha = 0.35f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your album is waiting",
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkSlate.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the heart button anytime to\ncapture a cozy Polaroid moment",
                                fontSize = 12.sp,
                                color = DarkSlate.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(polaroids, key = { it.id }) { memory ->
                            val bmp = remember(memory.imagePath) {
                                polaroidManager.loadPolaroidBitmap(memory)
                            }
                            if (bmp != null) {
                                PolaroidGridTile(
                                    bitmap = bmp,
                                    memory = memory,
                                    onTap = {
                                        selectedMemory = memory
                                        selectedBitmap = bmp
                                    },
                                    onDelete = {
                                        polaroidManager.deletePolaroid(memory.id)
                                        polaroids = polaroidManager.getPolaroids()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolaroidGridTile(
    bitmap: Bitmap,
    memory: PolaroidMemory,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val isCompleteCard = remember(bitmap) {
        val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
        ratio in 1.2f..1.55f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
    ) {
        if (isCompleteCard) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1080f / 1440f),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFAF8F5),
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E0D6))
            ) {
                Image(
                    painter = BitmapPainter(imageBitmap),
                    contentDescription = memory.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFCFBF9), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFE2E0DB), RoundedCornerShape(4.dp))
                    .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Image(
                        painter = BitmapPainter(imageBitmap),
                        contentDescription = memory.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = memory.title,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = memory.date,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Delete button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(DeepRose.copy(alpha = 0.90f))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
