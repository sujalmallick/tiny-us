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

import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType

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

    TinyDialog(onDismissRequest = onDismiss) {
        // 1. Header: Title + Shared Earphones pill + Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Music Box",
                    style = TinyType.Title,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "listening together",
                    style = TinyType.Caption,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Earphones pill toggle
            val earphonesOn = engine.earphonesActive
            Surface(
                onClick = {
                    val next = !engine.earphonesActive
                    engine.setEarphones(next)
                    if (next) {
                        onEnableAudio?.invoke()
                        audio.isEnabled = true
                        audio.playHeartChime()
                    }
                },
                shape = TinyRadius.Pill,
                color = if (earphonesOn) TinyColors.RoseSoft else TinyColors.Muted,
                contentColor = if (earphonesOn) TinyColors.Rose else TinyColors.Ink,
                border = if (earphonesOn) BorderStroke(1.dp, TinyColors.Rose.copy(alpha = 0.55f)) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = TinySpace.md, vertical = TinySpace.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = "Earphones",
                        tint = if (earphonesOn) TinyColors.Rose else TinyColors.InkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (earphonesOn) "Shared" else "Earphones",
                        style = TinyType.Label.copy(color = if (earphonesOn) TinyColors.Rose else TinyColors.Ink),
                        maxLines = 1
                    )
                }
            }

            TinyCloseButton(onClick = onDismiss)
        }

        // 2. Now Playing card
        TinyCard(spacing = TinySpace.md) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spinning Vinyl graphic
                Box(
                    modifier = Modifier
                        .size(56.dp)
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

                Spacer(modifier = Modifier.width(TinySpace.md))

                // Song Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = audio.currentSong.title,
                            style = TinyType.BodyStrong,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (audio.currentSong.rawResId != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TinyTag(text = "MP3", color = TinyColors.Rose, background = TinyColors.RoseSoft)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = audio.currentSong.artist,
                        style = TinyType.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = audio.currentSong.vibe,
                        style = TinyType.Micro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous Song",
                        tint = TinyColors.Ink,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(TinySpace.lg))

                // Play/Pause circular accent button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(TinyColors.Rose)
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
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(TinySpace.lg))

                IconButton(
                    onClick = {
                        onEnableAudio?.invoke()
                        audio.isEnabled = true
                        audio.nextSong()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next Song",
                        tint = TinyColors.Ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 3. Playlist
        Column(verticalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TinySpace.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLAYLIST",
                    style = TinyType.Micro.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Text(
                    text = "${audio.playlist.size} songs",
                    style = TinyType.Micro
                )
            }

            // 4. Track list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(audio.playlist) { song ->
                    val isSelected = song.id == audio.currentSong.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(TinyRadius.Medium)
                            .background(
                                if (isSelected) TinyColors.RoseSoft else Color.Transparent
                            )
                            .clickable {
                                onEnableAudio?.invoke()
                                audio.isEnabled = true
                                audio.playSong(song)
                            }
                            .padding(horizontal = TinySpace.md, vertical = TinySpace.sm),
                        contentAlignment = Alignment.CenterStart
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
                                    imageVector = if (isSelected && isPlaying) Icons.Rounded.PlayArrow else Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = if (isSelected) TinyColors.Rose else TinyColors.Blush,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(TinySpace.md))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = song.title,
                                            style = TinyType.Label.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) TinyColors.Rose else TinyColors.Ink
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (song.rawResId != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            TinyTag(
                                                text = "MP3",
                                                color = if (isSelected) TinyColors.Rose else TinyColors.InkMuted,
                                                background = if (isSelected) TinyColors.Card else TinyColors.Muted
                                            )
                                        }
                                    }
                                    Text(
                                        text = song.artist,
                                        style = TinyType.Caption,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(TinySpace.sm))
                                TinyTag(
                                    text = when (audio.musicBoxState) {
                                        MusicBoxState.PLAYING -> "Playing"
                                        MusicBoxState.PAUSED -> "Paused"
                                        MusicBoxState.STOPPED -> "Selected"
                                    },
                                    color = TinyColors.Rose,
                                    background = TinyColors.Card
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
