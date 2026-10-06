package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LongDistanceSignalType
import com.example.data.SharedMoodType
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource

/*
 * Shared UI chrome. Every dialog, sheet and button builds on these so the app keeps one look.
 * Tokens live in ui/theme/TinyTokens.kt.
 */

/**
 * Standard dialog frame: Paper surface, 24dp corners, hairline border, soft shadow and a short
 * fade + scale-in on open. [widthFraction] null keeps the platform default width.
 */
@Composable
fun TinyDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float? = null,
    maxHeight: Dp? = null,
    dismissOnClickOutside: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(TinySpace.xl),
    verticalSpacing: Dp = TinySpace.lg,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = widthFraction == null
        )
    ) {
        TinyEnterTransition {
            TinySurface(
                modifier = modifier
                    .then(if (widthFraction != null) Modifier.fillMaxWidth(widthFraction) else Modifier.fillMaxWidth())
                    .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier)
            ) {
                Column(
                    modifier = Modifier.padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                    horizontalAlignment = horizontalAlignment,
                    content = content
                )
            }
        }
    }
}

/** The dialog surface on its own, for frames that need their own Dialog setup. */
@Composable
fun TinySurface(
    modifier: Modifier = Modifier,
    shape: Shape = TinyRadius.Dialog,
    color: Color = TinyColors.Paper,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = TinyColors.Ink,
        border = BorderStroke(1.dp, TinyColors.Line),
        shadowElevation = 8.dp,
        content = content
    )
}

/** One-shot fade + gentle scale used when a dialog opens. Exit is instant, so dismissal timing never changes. */
@Composable
fun TinyEnterTransition(content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f)
    ) { content() }
}

/** A screen-reader label from strings.xml (semantics blocks can't look strings up themselves). */
@Composable
fun Modifier.describedAs(label: StringResource): Modifier {
    val text = stringResource(label)
    return this.semantics { contentDescription = text }
}

/** Dialog header: optional icon badge, Serif title, muted subtitle, 48dp close button. */
@Composable
fun TinyDialogHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accent: Color = TinyColors.Rose,
    accentSoft: Color = TinyColors.RoseSoft,
    onClose: (() -> Unit)? = null,
    closeTestTag: String? = null,
    closeDescription: String = stringResource(Res.string.ui_close)
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            TinyIconBadge(icon = icon, tint = accent, background = accentSoft)
            Spacer(Modifier.width(TinySpace.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TinyType.Title, modifier = Modifier.semantics { heading() })
            if (subtitle != null) {
                Text(subtitle, style = TinyType.Caption, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (onClose != null) {
            TinyCloseButton(onClick = onClose, testTag = closeTestTag, contentDescription = closeDescription)
        }
    }
}

@Composable
fun TinyCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier, testTag: String? = null, contentDescription: String = "Close") {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp).then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Icon(PixelIcons.Close, contentDescription = contentDescription, tint = TinyColors.InkMuted, modifier = Modifier.size(22.dp))
    }
}

/** Circular tinted badge holding a vector icon. Replaces every decorative emoji in chrome. */
@Composable
fun TinyIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = TinyColors.Rose,
    background: Color = TinyColors.RoseSoft,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = PixelCircleShape
) {
    Box(modifier = modifier.size(size).background(background, shape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

enum class TinyButtonStyle { Primary, Secondary, Outline, Ghost, Success }

/**
 * The one button. Primary for the main action, Secondary/Outline for the rest, Ghost for quiet text
 * actions. Always at least 48dp tall (40dp visual when [compact], touch target stays 48dp).
 */
@Composable
fun TinyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TinyButtonStyle = TinyButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
    testTag: String? = null
) {
    val m = modifier
        .heightIn(min = if (compact) 40.dp else 48.dp)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    val padding = PaddingValues(horizontal = if (compact) 14.dp else 18.dp, vertical = 8.dp)
    val label: @Composable () -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(TinySpace.sm))
        }
        Text(text, style = TinyType.Label.copy(color = Color.Unspecified), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    when (style) {
        TinyButtonStyle.Primary, TinyButtonStyle.Success, TinyButtonStyle.Secondary -> {
            val (container, content) = when (style) {
                TinyButtonStyle.Primary -> TinyColors.Rose to Color.White
                TinyButtonStyle.Success -> TinyColors.Sage to Color.White
                else -> TinyColors.Muted to TinyColors.Ink
            }
            Button(
                onClick = onClick,
                modifier = m,
                enabled = enabled,
                shape = TinyRadius.Medium,
                contentPadding = padding,
                elevation = null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = content,
                    disabledContainerColor = container.copy(alpha = 0.4f),
                    disabledContentColor = content.copy(alpha = 0.8f)
                )
            ) { label() }
        }
        TinyButtonStyle.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = m,
            enabled = enabled,
            shape = TinyRadius.Medium,
            contentPadding = padding,
            border = BorderStroke(1.dp, TinyColors.Line),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TinyColors.Ink, disabledContentColor = TinyColors.InkMuted)
        ) { label() }
        TinyButtonStyle.Ghost -> TextButton(
            onClick = onClick,
            modifier = m,
            enabled = enabled,
            shape = TinyRadius.Medium,
            contentPadding = padding,
            colors = ButtonDefaults.textButtonColors(contentColor = TinyColors.Rose, disabledContentColor = TinyColors.InkMuted)
        ) { label() }
    }
}

/** Card on Paper: Card fill, hairline border, 16dp corners. Clickable when [onClick] is set. */
@Composable
fun TinyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    padding: Dp = TinySpace.lg,
    spacing: Dp = TinySpace.md,
    color: Color = if (selected) TinyColors.RoseSoft else TinyColors.Card,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = BorderStroke(1.dp, if (selected) TinyColors.Rose.copy(alpha = 0.55f) else TinyColors.Line)
    val inner: @Composable () -> Unit = {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = TinyRadius.Large, color = color, contentColor = TinyColors.Ink, border = border, content = inner)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = TinyRadius.Large, color = color, contentColor = TinyColors.Ink, border = border, content = inner)
    }
}

/** Section heading used inside sheets and long dialogs. */
@Composable
fun TinySectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null) {
    Column(modifier = modifier.padding(top = TinySpace.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = TinyColors.Rose, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(TinySpace.sm))
            }
            Text(title, style = TinyType.Section, modifier = Modifier.semantics { heading() })
        }
        if (subtitle != null) {
            Text(subtitle, style = TinyType.Caption, modifier = Modifier.padding(start = if (icon != null) 26.dp else 0.dp, top = 2.dp))
        }
    }
}

/** Selectable pill. Visual height ~36dp; Material's minimum interactive size keeps the touch target at 48dp. */
@Composable
fun TinyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = TinyRadius.Pill,
        color = if (selected) TinyColors.Rose else TinyColors.Muted,
        contentColor = if (selected) Color.White else TinyColors.Ink
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = TinyType.Label.copy(color = Color.Unspecified), maxLines = 1)
        }
    }
}

/** Small non-interactive status pill ("Completed", "Active", "MP3"). */
@Composable
fun TinyTag(text: String, modifier: Modifier = Modifier, color: Color = TinyColors.InkMuted, background: Color = TinyColors.Muted) {
    Text(
        text,
        style = TinyType.Micro.copy(color = color),
        modifier = modifier.background(background, TinyRadius.Pill).padding(horizontal = 8.dp, vertical = 3.dp),
        maxLines = 1
    )
}

@Composable
fun TinyDivider(modifier: Modifier = Modifier) = HorizontalDivider(modifier = modifier, thickness = 1.dp, color = TinyColors.Line)

@Composable
fun tinySwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = TinyColors.Rose,
    checkedBorderColor = TinyColors.Rose,
    uncheckedThumbColor = TinyColors.InkMuted,
    uncheckedTrackColor = TinyColors.Muted,
    uncheckedBorderColor = TinyColors.Line
)

@Composable
fun tinyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TinyColors.Rose,
    unfocusedBorderColor = TinyColors.Line,
    focusedLabelColor = TinyColors.Rose,
    unfocusedLabelColor = TinyColors.InkMuted,
    cursorColor = TinyColors.Rose,
    focusedTextColor = TinyColors.Ink,
    unfocusedTextColor = TinyColors.Ink,
    focusedContainerColor = TinyColors.Card,
    unfocusedContainerColor = TinyColors.Card,
    focusedPlaceholderColor = TinyColors.InkMuted,
    unfocusedPlaceholderColor = TinyColors.InkMuted
)

val TinyFieldShape = TinyRadius.Medium

/** Vector icons that replace the old emoji glyphs. Keep the mapping here so every screen agrees. */
object TinyIcons {
    val DateAdventures = PixelIcons.Explore
    val DailyMoment = PixelIcons.EditNote
    val MiniGames = PixelIcons.Casino
    val SharedMood = PixelIcons.Mood
    val LongDistance = PixelIcons.Mail
    val OurStory = PixelIcons.AutoStories
    val Gift = PixelIcons.CardGiftcard
    val Sparkle = PixelIcons.AutoAwesome
    val Heart = PixelIcons.Favorite
    val HeartOutline = PixelIcons.FavoriteBorder
    val Birthday = PixelIcons.Cake
    val Flower = PixelIcons.LocalFlorist

    fun mood(type: SharedMoodType): ImageVector = when (type) {
        SharedMoodType.GREAT -> PixelIcons.AutoAwesome
        SharedMoodType.GOOD -> PixelIcons.LocalFlorist
        SharedMoodType.TIRED -> PixelIcons.LocalCafe
        SharedMoodType.LOW -> PixelIcons.Cloud
        SharedMoodType.MISSING_YOU -> PixelIcons.Mail
    }

    fun signal(type: LongDistanceSignalType): ImageVector = when (type) {
        LongDistanceSignalType.THINKING_OF_YOU -> PixelIcons.ChatBubble
        LongDistanceSignalType.GOOD_MORNING -> PixelIcons.WbSunny
        LongDistanceSignalType.GOOD_NIGHT -> PixelIcons.Bedtime
        LongDistanceSignalType.SEND_HEART -> PixelIcons.Favorite
        LongDistanceSignalType.NEED_A_HUG -> PixelIcons.VolunteerActivism
        LongDistanceSignalType.TINY_GIFT -> PixelIcons.CardGiftcard
    }

    /** Maps StoryEntry.iconKey (and memory icon types) to an icon. */
    fun story(key: String): ImageVector = when (key) {
        "flower", "garden" -> PixelIcons.LocalFlorist
        "tree" -> PixelIcons.Park
        "cooking" -> PixelIcons.Restaurant
        "couch" -> PixelIcons.Weekend
        "stars" -> PixelIcons.Nightlight
        "letter" -> PixelIcons.Mail
        "photo" -> PixelIcons.PhotoCamera
        "dream" -> PixelIcons.Bedtime
        "adventure" -> PixelIcons.Explore
        "moment" -> PixelIcons.LocalCafe
        "golden" -> PixelIcons.AutoAwesome
        "milestone" -> PixelIcons.Celebration
        "first" -> PixelIcons.AutoAwesome
        "birthday" -> PixelIcons.Celebration
        "phones_down" -> PixelIcons.Bedtime
        "make_up" -> PixelIcons.Favorite
        "jar" -> PixelIcons.VolunteerActivism
        "festival" -> PixelIcons.Celebration
        "upcoming" -> PixelIcons.HourglassTop
        else -> PixelIcons.Favorite
    }
}
