package com.example.ui

import com.example.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelIcons

// -----------------------------------------------------------------------------
// Shared Dream Journal - keyword engine + dialog
// -----------------------------------------------------------------------------

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

internal fun String.containsAny(vararg words: String): Boolean = words.any { word ->
    if (word.contains(' ')) this.contains(word)
    else Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(this)
}
internal fun String.matchingKeywords(vararg words: String): List<String> = words.filter { word ->
    if (word.contains(' ')) this.contains(word)
    else Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(this)
}

@Composable
fun DreamJournalDialog(
    prefs: com.example.data.PreferencesManager,
    onDismiss: () -> Unit,
    onVisualizeDream: (theme: String, text: String) -> Unit,
    /** A dream was written down (it counts toward the little firsts). */
    onDreamSaved: () -> Unit = {}
) {
    var dreamText by remember { mutableStateOf("") }
    var dreamsList by remember { mutableStateOf(prefs.getDreamEntries()) }
    var errorText by remember { mutableStateOf("") }
    val emptyDreamError = stringResource(R.string.ui_dream_empty_error)

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dream_journal_dialog"),
        widthFraction = 0.94f
    ) {
        TinyDialogHeader(
            title = stringResource(R.string.ui_shared_dream_journal),
            subtitle = stringResource(R.string.ui_type_a_dream_and_watch_it_come_alive),
            icon = PixelIcons.Bedtime,
            accent = TinyColors.Plum,
            accentSoft = TinyColors.PlumSoft,
            onClose = onDismiss
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TinySpace.md)
        ) {
            // Dream input
            OutlinedTextField(
                value = dreamText,
                onValueChange = {
                    dreamText = it
                    errorText = ""
                },
                label = { Text(stringResource(R.string.ui_describe_your_dream)) },
                placeholder = { Text(stringResource(R.string.ui_e_g_we_walked_under_cherry_blossoms_in_j), style = TinyType.Caption) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                shape = TinyFieldShape,
                colors = tinyTextFieldColors()
            )

            if (errorText.isNotEmpty()) {
                Text(errorText, style = TinyType.Caption.copy(color = TinyColors.Rose))
            }

            // Visualize button
            TinyButton(
                text = stringResource(R.string.ui_visualize_dream),
                onClick = {
                    val trimmed = dreamText.trim()
                    if (trimmed.isEmpty()) {
                        errorText = emptyDreamError
                    } else {
                        val (theme, keywords) = parseDreamTheme(trimmed)
                        val entry = com.example.data.DreamEntry(
                            id = java.util.UUID.randomUUID().toString(),
                            text = trimmed,
                            matchedKeywords = keywords,
                            dreamTheme = theme,
                            timestamp = System.currentTimeMillis()
                        )
                        prefs.addDreamEntry(entry)
                        onDreamSaved()
                        dreamsList = prefs.getDreamEntries()
                        onVisualizeDream(theme, trimmed)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Primary,
                icon = PixelIcons.AutoAwesome
            )

            // Dream history
            if (dreamsList.isNotEmpty()) {
                TinySectionHeader(title = stringResource(R.string.ui_past_dreams))

                Column(verticalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                    dreamsList.take(10).forEach { entry ->
                        TinyCard(padding = TinySpace.md, spacing = 0.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.text,
                                        style = TinyType.Body,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.xs)) {
                                        val themeLabel = when (entry.dreamTheme) {
                                            "JAPAN" -> stringResource(R.string.ui_dream_theme_japan)
                                            "NORWAY" -> stringResource(R.string.ui_dream_theme_aurora)
                                            "OCEAN" -> stringResource(R.string.ui_dream_theme_ocean)
                                            "FLYING" -> stringResource(R.string.ui_dream_theme_flying)
                                            "STARS" -> stringResource(R.string.ui_dream_theme_stars)
                                            "FOREST" -> stringResource(R.string.ui_dream_theme_forest)
                                            "HOME" -> stringResource(R.string.ui_dream_theme_cozy)
                                            "RAIN" -> stringResource(R.string.ui_dream_theme_rain)
                                            "CITY" -> stringResource(R.string.ui_dream_theme_city)
                                            "SWEET" -> stringResource(R.string.ui_dream_theme_sweet)
                                            else -> stringResource(R.string.ui_dream_theme_dream)
                                        }
                                        TinyTag(
                                            text = themeLabel,
                                            color = TinyColors.Plum,
                                            background = TinyColors.PlumSoft
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            onVisualizeDream(entry.dreamTheme, entry.text)
                                            onDismiss()
                                        }
                                    ) {
                                        Icon(
                                            PixelIcons.AutoAwesome,
                                            contentDescription = stringResource(R.string.ui_relive_dream),
                                            tint = TinyColors.Plum,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            prefs.deleteDreamEntry(entry.id)
                                            dreamsList = prefs.getDreamEntries()
                                        }
                                    ) {
                                        Icon(
                                            PixelIcons.Delete,
                                            contentDescription = stringResource(R.string.ui_delete_dream),
                                            tint = TinyColors.InkMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Close button
            TinyButton(
                text = stringResource(R.string.ui_close),
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = TinyButtonStyle.Ghost
            )
        }
    }
}
