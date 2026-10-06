package com.example.ui

import com.example.data.CoupleCalendar
import com.example.data.CoupleDates
import com.example.engine.WorldAudio
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TinyUsMemory
import com.example.data.SpecialMemoryType
import com.example.ui.theme.PixelIcons

import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource

@Composable
fun SpecialCalendarDialog(
    onDismiss: () -> Unit,
    audio: WorldAudio? = null,
    boyfriendName: String = com.example.data.ProfileManager.getProfile().boyName,
    girlfriendName: String = com.example.data.ProfileManager.getProfile().girlName
) {
    var displayedYearMonth by remember { mutableStateOf(CoupleDates.today().firstOfMonth()) }
    var selectedMemory by remember { mutableStateOf<TinyUsMemory?>(null) }
    val today = remember { CoupleDates.today() }
    val todayMemories = remember(displayedYearMonth) { CoupleCalendar.getMemoriesForDate(today) }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("special_calendar_dialog"),
        widthFraction = 0.92f,
        contentPadding = PaddingValues(0.dp),
        verticalSpacing = 0.dp
    ) {
        TinyDialogHeader(
            title = stringResource(Res.string.ui_our_special_calendar),
            subtitle = stringResource(Res.string.ui_calendar_subtitle, boyfriendName, girlfriendName),
            icon = TinyIcons.Heart,
            onClose = onDismiss,
            modifier = Modifier.padding(start = TinySpace.xl, end = TinySpace.sm, top = TinySpace.lg, bottom = TinySpace.sm)
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = TinySpace.lg, end = TinySpace.lg, bottom = TinySpace.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Today's Special Event Banner (if today has an event!)
            if (todayMemories.isNotEmpty()) {
                val firstToday = todayMemories.first()
                Surface(
                    shape = TinyRadius.Medium,
                    color = TinyColors.RoseSoft,
                    border = BorderStroke(1.dp, TinyColors.Rose.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TinySpace.sm)
                        .clip(TinyRadius.Medium)
                        .clickable {
                            audio?.playHeartChime()
                            selectedMemory = firstToday
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = TinySpace.md, vertical = TinySpace.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = TinyIcons.Heart,
                            contentDescription = null,
                            tint = TinyColors.Rose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(TinySpace.sm))
                        Column {
                            Text(
                                text = stringResource(Res.string.ui_today_prefix, firstToday.title.uppercase()),
                                style = TinyType.Label.copy(color = TinyColors.Rose)
                            )
                            Text(
                                text = stringResource(Res.string.ui_tap_to_view_today_s_memory),
                                style = TinyType.Caption
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(TinySpace.sm))

            // Month Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        audio?.playBubblePop()
                        displayedYearMonth = displayedYearMonth.minus(1, DateTimeUnit.MONTH)
                    },
                    modifier = Modifier.testTag("calendar_prev_month")
                ) {
                    Icon(
                        imageVector = PixelIcons.ArrowBack,
                        contentDescription = stringResource(Res.string.ui_previous_month),
                        tint = TinyColors.Rose,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val monthTitle = remember(displayedYearMonth) {
                        DateText.format(displayedYearMonth, "MMMM yyyy")
                    }
                    Text(
                        text = monthTitle,
                        style = TinyType.Section.copy(fontSize = 16.sp),
                        textAlign = TextAlign.Center
                    )
                    if (displayedYearMonth != today.firstOfMonth()) {
                        TinyButton(
                            text = stringResource(Res.string.ui_jump_to_today),
                            onClick = {
                                audio?.playBubblePop()
                                displayedYearMonth = today.firstOfMonth()
                            },
                            style = TinyButtonStyle.Ghost,
                            compact = true
                        )
                    }
                }

                IconButton(
                    onClick = {
                        audio?.playBubblePop()
                        displayedYearMonth = displayedYearMonth.plus(1, DateTimeUnit.MONTH)
                    },
                    modifier = Modifier.testTag("calendar_next_month")
                ) {
                    Icon(
                        imageVector = PixelIcons.ArrowForward,
                        contentDescription = stringResource(Res.string.ui_next_month),
                        tint = TinyColors.Rose,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(TinySpace.sm))

            // Days of Week Header
            // Short weekday names in the phone's language, Monday first.
            val daysOfWeek = remember { DateText.shortWeekdays() }
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
                            style = TinyType.Micro,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(TinySpace.sm))

            // Calendar Days Grid
            val firstDayOfWeek = displayedYearMonth.dayOfWeek.isoDayNumber // 1=Mon, 7=Sun
            val daysInMonth = displayedYearMonth.daysUntil(displayedYearMonth.plus(1, DateTimeUnit.MONTH))
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
                            val cellDate = displayedYearMonth.plus(dayNum - 1, DateTimeUnit.DAY)
                            val isCellToday = (cellDate == today)
                            val cellMemories = CoupleCalendar.getMemoriesForDate(cellDate)
                            val hasEvent = cellMemories.isNotEmpty()
                            val memoryType = cellMemories.firstOrNull()?.type

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .heightIn(min = 40.dp)
                                    .clip(TinyRadius.Small)
                                    .background(
                                        when {
                                            isCellToday -> TinyColors.RoseSoft
                                            hasEvent -> TinyColors.Blush.copy(alpha = 0.18f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isCellToday) 1.5.dp else if (hasEvent) 1.dp else 0.dp,
                                        color = if (isCellToday) TinyColors.Rose else if (hasEvent) TinyColors.Blush.copy(alpha = 0.7f) else Color.Transparent,
                                        shape = TinyRadius.Small
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
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = TinySpace.xs)
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = TinyType.Body.copy(
                                            fontSize = 13.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = if (isCellToday || hasEvent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCellToday) TinyColors.Rose else TinyColors.Ink
                                        ),
                                        maxLines = 1
                                    )

                                    if (hasEvent) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        CalendarPixelMarker(type = memoryType ?: SpecialMemoryType.RELATIONSHIP)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).heightIn(min = 40.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Spacer(modifier = Modifier.height(TinySpace.md))

            // Scrapbook Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(icon = TinyIcons.Heart, label = stringResource(Res.string.ui_moments))
                LegendItem(icon = TinyIcons.Birthday, label = stringResource(Res.string.ui_birthdays))
                LegendItem(icon = TinyIcons.Sparkle, label = stringResource(Res.string.ui_next_meet))
                LegendItem(icon = TinyIcons.Flower, label = stringResource(Res.string.ui_special))
            }

            Spacer(modifier = Modifier.height(TinySpace.lg))

            TinyButton(
                text = stringResource(Res.string.ui_close_scrapbook),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Secondary,
                testTag = "calendar_close_button"
            )
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
        mutableStateOf(CoupleCalendar.calculateCountdown(memory.date))
    }

    if (memory.isFuture) {
        LaunchedEffect(memory.date) {
            while (isActive) {
                countdown = CoupleCalendar.calculateCountdown(memory.date)
                delay(1000)
            }
        }
    }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("memory_detail_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TinyIconBadge(
                icon = calendarTypeIcon(memory.type),
                size = 56.dp,
                iconSize = 28.dp
            )

            Spacer(modifier = Modifier.height(TinySpace.md))

            Text(
                text = memory.title,
                style = TinyType.Title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(TinySpace.xs))

            val formattedDate = remember(memory.date) {
                DateText.format(memory.date, "dd MMMM yyyy")
            }
            Text(
                text = formattedDate,
                style = TinyType.Caption
            )
        }

        TinyDivider(modifier = Modifier.width(48.dp))

        if (memory.isFuture) {
            if (countdown.isToday) {
                TinyCard(selected = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.ui_today),
                            style = TinyType.Title.copy(color = TinyColors.Rose)
                        )
                        Spacer(modifier = Modifier.height(TinySpace.xs))
                        Text(
                            text = stringResource(Res.string.ui_you_re_finally_together_again),
                            style = TinyType.Body.copy(fontFamily = FontFamily.Serif),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (countdown.isPassed) {
                Text(
                    text = memory.description,
                    style = TinyType.Body,
                    textAlign = TextAlign.Center
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(Res.string.ui_coming_in),
                        style = TinyType.Caption
                    )
                    Spacer(modifier = Modifier.height(TinySpace.sm))
                    TinyCard(color = TinyColors.Muted, padding = TinySpace.md) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = pluralStringResource(Res.plurals.ui_countdown_days, countdown.days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), countdown.days),
                                style = TinyType.Title.copy(color = TinyColors.Rose)
                            )
                            Spacer(modifier = Modifier.height(TinySpace.xs))
                            Text(
                                text = "${two(countdown.hours)} HOURS  ${two(countdown.minutes)} MINUTES  ${two(countdown.seconds)} SECONDS",
                                style = TinyType.Micro.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TinyColors.Ink
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(TinySpace.md))
                    Text(
                        text = memory.description,
                        style = TinyType.Body,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = memory.description,
                style = TinyType.Body,
                textAlign = TextAlign.Center
            )
        }

        TinyButton(
            text = stringResource(Res.string.ui_close),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            style = TinyButtonStyle.Secondary,
            testTag = "memory_detail_close_button"
        )
    }
}

/** Vector icon for each special memory type (calendar markers, legend and detail card). */
private fun calendarTypeIcon(type: SpecialMemoryType): ImageVector = when (type) {
    SpecialMemoryType.BIRTHDAY -> TinyIcons.Birthday
    SpecialMemoryType.FUTURE_MEETING -> TinyIcons.Sparkle
    SpecialMemoryType.KISS -> TinyIcons.HeartOutline
    SpecialMemoryType.PRIVATE -> TinyIcons.Flower
    else -> TinyIcons.Heart
}

/** Tiny day-cell marker showing the memory type, named like the legend below the calendar. */
@Composable
internal fun CalendarPixelMarker(type: SpecialMemoryType) {
    Icon(
        imageVector = calendarTypeIcon(type),
        contentDescription = stringResource(
            when (type) {
                SpecialMemoryType.BIRTHDAY -> Res.string.ui_birthdays
                SpecialMemoryType.FUTURE_MEETING -> Res.string.ui_next_meet
                SpecialMemoryType.PRIVATE -> Res.string.ui_special
                else -> Res.string.ui_moments
            }
        ),
        tint = TinyColors.Rose,
        modifier = Modifier.size(11.dp)
    )
}

@Composable
internal fun LegendItem(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TinyColors.Rose,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(TinySpace.xs))
        Text(text = label, style = TinyType.Micro, maxLines = 1)
    }
}

/** The first day of this date's month (the calendar shows one month at a time). */
private fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

/** Two digits, as in a clock. */
private fun two(value: Long): String = value.toString().padStart(2, '0')
