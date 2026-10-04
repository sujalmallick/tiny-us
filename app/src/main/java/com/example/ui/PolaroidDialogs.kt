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
import com.example.ui.theme.PixelCornerShape
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons

import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType

// -----------------------------------------------------------------------------
// POLAROID CAPTURE OVERLAY
// -----------------------------------------------------------------------------

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
                        .background(TinyColors.Scrim.copy(alpha = 0.72f))
                )
            }

            // 1. Shutter Flash
            if (flashAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = flashAlpha }
                        .background(Color.White)
                )
            }

            // 2. Polaroid Card & Actions
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

                    Spacer(modifier = Modifier.height(TinySpace.lg))

                    // Action buttons row below Polaroid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Save to Device (primary action)
                        TinyButton(
                            text = if (savedToDevice) "Saved to Photos" else "Save to Device",
                            onClick = {
                                val success = polaroidManager.saveToDeviceGallery(bitmap, memory.title)
                                if (success) {
                                    savedToDevice = true
                                    audio?.playHeartChime()
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .semantics { contentDescription = "Save to Device" },
                            style = if (savedToDevice) TinyButtonStyle.Success else TinyButtonStyle.Primary,
                            icon = if (savedToDevice) PixelIcons.Check else PixelIcons.Download
                        )

                        // 2. All Memories button
                        TinyButton(
                            text = "Memories",
                            onClick = onOpenGallery,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Memories" },
                            style = TinyButtonStyle.Secondary,
                            icon = TinyIcons.Heart
                        )
                    }

                    Spacer(modifier = Modifier.height(TinySpace.sm))

                    // Done / dismiss button (ghost, white on the dark backdrop)
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = TinyRadius.Medium,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = "Done",
                            style = TinyType.Label.copy(color = Color.Unspecified)
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
                .background(Color.Black.copy(alpha = 0.28f), PixelCornerShape(10.dp))
        )

        if (isCompleteCard) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1080f / 1440f),
                shape = PixelCornerShape(8.dp),
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
                    .background(Color(0xFFFCFBF9), PixelCornerShape(6.dp))
                    .border(1.dp, Color(0xFFE5E5E5), PixelCornerShape(6.dp))
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Photo Area: Classic 4:5 aspect ratio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                        .clip(PixelCornerShape(3.dp))
                        .border(1.dp, Color(0xFFE8E5DF), PixelCornerShape(3.dp))
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

        // Delete button (shown when in gallery full view): 28dp visual inside a 48dp touch target
        if (showDeleteButton && onDelete != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp)
                    .size(48.dp)
                    .clip(PixelCircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(TinyColors.Rose, PixelCircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        PixelIcons.DeleteOutline,
                        contentDescription = "Delete memory",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// POLAROID GALLERY DIALOG
// -----------------------------------------------------------------------------

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

                    Spacer(modifier = Modifier.height(TinySpace.lg))

                    var savedInInspector by remember(selectedMemory!!.id) { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Back button
                        TinyButton(
                            text = "Back",
                            onClick = {
                                selectedMemory = null
                                selectedBitmap = null
                            },
                            modifier = Modifier.semantics { contentDescription = "Back" },
                            style = TinyButtonStyle.Secondary,
                            icon = PixelIcons.ArrowBack
                        )

                        // 2. Save to Device button
                        TinyButton(
                            text = if (savedInInspector) "Saved" else "Save to Device",
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
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Save to Device" },
                            style = if (savedInInspector) TinyButtonStyle.Success else TinyButtonStyle.Primary,
                            icon = if (savedInInspector) PixelIcons.Check else PixelIcons.Download
                        )

                        // 3. Delete button
                        Surface(
                            onClick = {
                                polaroidManager.deletePolaroid(selectedMemory!!.id)
                                polaroids = polaroidManager.getPolaroids()
                                selectedMemory = null
                                selectedBitmap = null
                            },
                            modifier = Modifier.size(48.dp),
                            shape = TinyRadius.Medium,
                            color = TinyColors.Muted,
                            contentColor = TinyColors.Rose
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = PixelIcons.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = TinyColors.Rose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    TinyDialog(
        onDismissRequest = onDismiss,
        maxHeight = 620.dp
    ) {
        TinyDialogHeader(
            title = "Polaroid Memories",
            subtitle = if (polaroids.isEmpty()) "No moments captured yet" else "${polaroids.size} moments captured",
            icon = PixelIcons.PhotoCamera,
            onClose = onDismiss
        )

        if (polaroids.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TinyIconBadge(
                        icon = TinyIcons.Heart,
                        size = 64.dp,
                        iconSize = 32.dp
                    )
                    Spacer(modifier = Modifier.height(TinySpace.md))
                    Text(
                        text = "Your album is waiting",
                        style = TinyType.Section,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(TinySpace.xs))
                    Text(
                        text = "Tap the heart button anytime to\ncapture a cozy Polaroid moment",
                        style = TinyType.Caption,
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
                horizontalArrangement = Arrangement.spacedBy(TinySpace.md),
                verticalArrangement = Arrangement.spacedBy(TinySpace.md),
                contentPadding = PaddingValues(top = TinySpace.sm, bottom = TinySpace.xs)
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

@Composable
internal fun PolaroidGridTile(
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
                shape = PixelCornerShape(6.dp),
                color = Color(0xFFFAF8F5),
                border = BorderStroke(1.dp, TinyColors.Line)
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
                    .background(Color(0xFFFCFBF9), PixelCornerShape(4.dp))
                    .border(1.dp, TinyColors.Line, PixelCornerShape(4.dp))
                    .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                        .clip(PixelCornerShape(2.dp))
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

        // Delete button: ~26dp visual inside a 48dp touch target, kept at the tile's top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 14.dp, y = (-14).dp)
                .size(48.dp)
                .clip(PixelCircleShape)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(TinyColors.Rose, PixelCircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    PixelIcons.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
