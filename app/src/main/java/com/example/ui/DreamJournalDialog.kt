package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PreferencesManager
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelIcons
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource

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
    val emptyDreamError = stringResource(Res.string.ui_dream_empty_error)

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dream_journal_dialog"),
        widthFraction = 0.94f
    ) {
        TinyDialogHeader(
            title = stringResource(Res.string.ui_shared_dream_journal),
            subtitle = stringResource(Res.string.ui_type_a_dream_and_watch_it_come_alive),
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
                label = { Text(stringResource(Res.string.ui_describe_your_dream)) },
                placeholder = { Text(stringResource(Res.string.ui_e_g_we_walked_under_cherry_blossoms_in_j), style = TinyType.Caption) },
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
                text = stringResource(Res.string.ui_visualize_dream),
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
                TinySectionHeader(title = stringResource(Res.string.ui_past_dreams))

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
                                            "JAPAN" -> stringResource(Res.string.ui_dream_theme_japan)
                                            "NORWAY" -> stringResource(Res.string.ui_dream_theme_aurora)
                                            "OCEAN" -> stringResource(Res.string.ui_dream_theme_ocean)
                                            "FLYING" -> stringResource(Res.string.ui_dream_theme_flying)
                                            "STARS" -> stringResource(Res.string.ui_dream_theme_stars)
                                            "FOREST" -> stringResource(Res.string.ui_dream_theme_forest)
                                            "HOME" -> stringResource(Res.string.ui_dream_theme_cozy)
                                            "RAIN" -> stringResource(Res.string.ui_dream_theme_rain)
                                            "CITY" -> stringResource(Res.string.ui_dream_theme_city)
                                            "SWEET" -> stringResource(Res.string.ui_dream_theme_sweet)
                                            else -> stringResource(Res.string.ui_dream_theme_dream)
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
                                            contentDescription = stringResource(Res.string.ui_relive_dream),
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
                                            contentDescription = stringResource(Res.string.ui_delete_dream),
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
                text = stringResource(Res.string.ui_close),
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = TinyButtonStyle.Ghost
            )
        }
    }
}
