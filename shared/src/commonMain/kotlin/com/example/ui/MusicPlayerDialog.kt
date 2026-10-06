package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import com.example.engine.AmbientAudio
import com.example.engine.MusicBoxState
import com.example.scene.SceneEngine
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepRose
import androidx.compose.foundation.lazy.grid.items
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons

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
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource

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
        label = stringResource(Res.string.ui_vinyl_rotation)
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
                    text = stringResource(Res.string.ui_music_box),
                    style = TinyType.Title,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(Res.string.ui_listening_together),
                    style = TinyType.Caption,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Earphones pill toggle
            val earphonesOn = engine.earphonesActive
            Surface(
                selected = earphonesOn,
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
                        imageVector = PixelIcons.Headphones,
                        contentDescription = stringResource(Res.string.ui_earphones),
                        tint = if (earphonesOn) TinyColors.Rose else TinyColors.InkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (earphonesOn) stringResource(Res.string.ui_shared) else stringResource(Res.string.ui_earphones),
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
                            TinyTag(text = stringResource(Res.string.ui_mp3), color = TinyColors.Rose, background = TinyColors.RoseSoft)
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
                        imageVector = PixelIcons.SkipPrevious,
                        contentDescription = stringResource(Res.string.ui_previous_song),
                        tint = TinyColors.Ink,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(TinySpace.lg))

                // Play/Pause circular accent button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(PixelCircleShape)
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
                        imageVector = if (isPlaying) PixelIcons.Pause else PixelIcons.PlayArrow,
                        contentDescription = stringResource(Res.string.ui_play_pause),
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
                        imageVector = PixelIcons.SkipNext,
                        contentDescription = stringResource(Res.string.ui_next_song),
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
                    text = stringResource(Res.string.ui_playlist),
                    style = TinyType.Micro.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Text(
                    text = pluralStringResource(Res.plurals.ui_song_count, audio.playlist.size, audio.playlist.size),
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
                                    imageVector = if (isSelected && isPlaying) PixelIcons.PlayArrow else PixelIcons.Favorite,
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
                                                text = stringResource(Res.string.ui_mp3),
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
                                        MusicBoxState.PLAYING -> stringResource(Res.string.ui_playing)
                                        MusicBoxState.PAUSED -> stringResource(Res.string.ui_paused)
                                        MusicBoxState.STOPPED -> stringResource(Res.string.ui_selected)
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
