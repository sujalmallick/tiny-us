package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PreferencesManager
import com.example.scene.SceneType
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepRose
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons

@Composable
internal fun SettingsCategoryHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    TinySectionHeader(title = title, subtitle = subtitle, icon = icon)
}

@Composable
internal fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    TinyCard(modifier = modifier, content = content)
}

/** One tappable settings row: icon badge, label, optional caption and a trailing chevron. */
@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(TinyRadius.Medium)
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(vertical = TinySpace.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TinyIconBadge(icon = icon)
        Spacer(modifier = Modifier.width(TinySpace.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = TinyType.BodyStrong)
            if (subtitle != null) {
                Text(text = subtitle, style = TinyType.Caption, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(modifier = Modifier.width(TinySpace.sm))
        Icon(
            imageVector = PixelIcons.ChevronRight,
            contentDescription = null,
            tint = TinyColors.InkMuted
        )
    }
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
    onOpenOurStory: () -> Unit = {},
    onOpenLoveNotes: () -> Unit = {},
    onOpenPolaroids: () -> Unit = {},
    onOpenDreamJournal: () -> Unit = {},
    onOpenWardrobe: () -> Unit = {},
    onOpenAvatarCustomizer: () -> Unit = {},
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
        containerColor = TinyColors.Paper,
        contentColor = TinyColors.Ink,
        modifier = Modifier.testTag("settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = TinySpace.xl, end = TinySpace.xl, bottom = TinySpace.md),
            verticalArrangement = Arrangement.spacedBy(TinySpace.md)
        ) {
            // Header: back, title, close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(TinyRadius.Medium)
                        .clickable { onDismiss() }
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = PixelIcons.ArrowBack,
                            contentDescription = stringResource(R.string.ui_back),
                            tint = TinyColors.Ink
                        )
                    }
                    Spacer(modifier = Modifier.width(TinySpace.xs))
                    Text(
                        text = stringResource(R.string.ui_tiny_us_settings),
                        style = TinyType.Title,
                        modifier = Modifier
                            .padding(end = TinySpace.sm)
                            .semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TinyCloseButton(
                    onClick = onDismiss,
                    testTag = "settings_close_button",
                    contentDescription = stringResource(R.string.ui_close)
                )
            }

            // -- 1. Our World --
            SettingsCategoryHeader(
                icon = PixelIcons.Favorite,
                title = stringResource(R.string.ui_our_world),
                subtitle = stringResource(R.string.ui_names_and_special_dates_for_your_story_t)
            )
            SettingsSectionCard {
                // Names (Max 10 chars)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.md)) {
                    OutlinedTextField(
                        value = boyName,
                        onValueChange = {
                            val trimmed = it.take(10)
                            boyName = trimmed
                            prefs.boyfriendName = trimmed
                            onSettingsChanged()
                        },
                        label = { Text(stringResource(R.string.label_your_name)) },
                        modifier = Modifier.weight(1f).testTag("input_boy_name"),
                        singleLine = true,
                        shape = TinyFieldShape,
                        colors = tinyTextFieldColors()
                    )
                    OutlinedTextField(
                        value = girlName,
                        onValueChange = {
                            val trimmed = it.take(10)
                            girlName = trimmed
                            prefs.girlfriendName = trimmed
                            onSettingsChanged()
                        },
                        label = { Text(stringResource(R.string.label_partner_name)) },
                        modifier = Modifier.weight(1f).testTag("input_girl_name"),
                        singleLine = true,
                        shape = TinyFieldShape,
                        colors = tinyTextFieldColors()
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
                    label = { Text(stringResource(R.string.ui_anniversary_date_yyyy_mm_dd)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_anniversary_date"),
                    singleLine = true,
                    shape = TinyFieldShape,
                    colors = tinyTextFieldColors()
                )

                // Birthdays (recurring yearly)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.md)) {
                    OutlinedTextField(
                        value = boyBirthday,
                        onValueChange = {
                            boyBirthday = it
                            prefs.boyfriendBirthday = it
                            onSettingsChanged()
                        },
                        label = { Text(stringResource(R.string.label_your_birthday)) },
                        placeholder = { Text(stringResource(R.string.ui_yyyy_mm_dd)) },
                        modifier = Modifier.weight(1f).testTag("input_boy_bday"),
                        singleLine = true,
                        shape = TinyFieldShape,
                        colors = tinyTextFieldColors()
                    )
                    OutlinedTextField(
                        value = girlBirthday,
                        onValueChange = {
                            girlBirthday = it
                            prefs.girlfriendBirthday = it
                            onSettingsChanged()
                        },
                        label = { Text(stringResource(R.string.label_partner_birthday)) },
                        placeholder = { Text(stringResource(R.string.ui_yyyy_mm_dd)) },
                        modifier = Modifier.weight(1f).testTag("input_girl_bday"),
                        singleLine = true,
                        shape = TinyFieldShape,
                        colors = tinyTextFieldColors()
                    )
                }

                // Secret Gift Box Easter Egg (Tap 5 times to reveal!)
                GiftBoxEasterEgg(
                    onJumpToMomoStall = {
                        onJumpToScene(SceneType.MOMO_STALL)
                    }
                )
            }

            // -- 2. Characters & Wardrobe --
            SettingsCategoryHeader(
                icon = PixelIcons.Checkroom,
                title = stringResource(R.string.ui_characters_wardrobe),
                subtitle = stringResource(R.string.ui_sweaters_hoodies_cute_ribbons_for_both_c)
            )
            SettingsSectionCard {
                Column {
                    SettingsNavRow(
                        icon = PixelIcons.Checkroom,
                        title = stringResource(R.string.ui_cottage_wardrobe),
                        subtitle = stringResource(R.string.ui_hoodies_outfits),
                        onClick = {
                            onDismiss()
                            onOpenWardrobe()
                        },
                        testTag = "settings_open_wardrobe_button"
                    )
                    TinyDivider()
                    SettingsNavRow(
                        icon = PixelIcons.Face,
                        title = stringResource(R.string.avatar_entry),
                        onClick = {
                            onDismiss()
                            onOpenAvatarCustomizer()
                        },
                        testTag = "settings_open_avatar_button"
                    )
                }
            }

            // -- 3. Atmosphere & Sky --
            SettingsCategoryHeader(
                icon = PixelIcons.WbSunny,
                title = stringResource(R.string.ui_atmosphere_sky),
                subtitle = stringResource(R.string.ui_sync_with_the_real_sky_or_set_an_intimat)
            )
            SettingsSectionCard {
                Text(stringResource(R.string.ui_sky_atmosphere), style = TinyType.Label)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)
                ) {
                    val modes = listOf("AUTO", "DAY", "SUNSET", "NIGHT")
                    modes.forEach { m ->
                        TinyChip(
                            text = m,
                            selected = atmosphere == m,
                            onClick = {
                                atmosphere = m
                                prefs.atmosphereMode = m
                                onSettingsChanged()
                            }
                        )
                    }
                }
            }

            // -- 4. Sounds & Music --
            SettingsCategoryHeader(
                icon = PixelIcons.VolumeUp,
                title = stringResource(R.string.ui_sounds_music),
                subtitle = stringResource(R.string.ui_gentle_music_box_lullabies_peaceful_natu)
            )
            SettingsSectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(TinyRadius.Medium)
                        .clickable {
                            soundEnabled = !soundEnabled
                            prefs.soundEnabled = soundEnabled
                            onSettingsChanged()
                        }
                        .padding(vertical = TinySpace.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TinyIconBadge(
                        icon = if (soundEnabled) PixelIcons.VolumeUp else PixelIcons.VolumeOff,
                        tint = if (soundEnabled) TinyColors.Rose else TinyColors.InkMuted,
                        background = if (soundEnabled) TinyColors.RoseSoft else TinyColors.Muted
                    )
                    Spacer(modifier = Modifier.width(TinySpace.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_cozy_ambient_lullaby_audio), style = TinyType.BodyStrong)
                        Text(
                            if (soundEnabled) stringResource(R.string.ui_sounds_on) else stringResource(R.string.ui_sounds_muted),
                            style = TinyType.Caption,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(TinySpace.sm))
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            prefs.soundEnabled = it
                            onSettingsChanged()
                        },
                        colors = tinySwitchColors()
                    )
                }
            }

            // -- 5. Memories & Keepsakes --
            SettingsCategoryHeader(
                icon = TinyIcons.Sparkle,
                title = stringResource(R.string.ui_memories_keepsakes),
                subtitle = stringResource(R.string.ui_your_love_notes_special_days_polaroid_mo)
            )
            SettingsSectionCard {
                Column {
                    SettingsNavRow(
                        icon = TinyIcons.OurStory,
                        title = stringResource(R.string.story_entry),
                        onClick = {
                            onDismiss()
                            onOpenOurStory()
                        },
                        testTag = "settings_our_story_button"
                    )
                    TinyDivider()
                    SettingsNavRow(
                        icon = TinyIcons.Heart,
                        title = stringResource(R.string.ui_our_keepsakes),
                        onClick = {
                            onDismiss()
                            onOpenMemories()
                        }
                    )
                    TinyDivider()
                    SettingsNavRow(
                        icon = PixelIcons.Drafts,
                        title = stringResource(R.string.ui_love_notes),
                        onClick = {
                            onDismiss()
                            onOpenLoveNotes()
                        }
                    )
                    TinyDivider()
                    SettingsNavRow(
                        icon = PixelIcons.PhotoLibrary,
                        title = stringResource(R.string.ui_tiny_moments_gallery),
                        onClick = {
                            onDismiss()
                            onOpenPolaroids()
                        }
                    )
                    TinyDivider()
                    SettingsNavRow(
                        icon = PixelIcons.Bedtime,
                        title = stringResource(R.string.ui_shared_dream_journal),
                        onClick = onOpenDreamJournal,
                        testTag = "settings_open_dream_journal_button"
                    )
                }
            }

            // -- Couple Activities & Connection --
            SettingsCategoryHeader(
                icon = PixelIcons.Favorite,
                title = stringResource(R.string.ui_couple_activities_connection),
                subtitle = stringResource(R.string.ui_adventures_reflections_mini_games_long_d)
            )
            SettingsSectionCard {
                Column {
                    SettingsNavRow(
                        icon = TinyIcons.DateAdventures,
                        title = stringResource(R.string.ui_date_adventures),
                        onClick = {
                            onDismiss()
                            onOpenDateAdventures()
                        },
                        testTag = "settings_date_adventures_button"
                    )
                    TinyDivider()
                    SettingsNavRow(
                        icon = TinyIcons.DailyMoment,
                        title = stringResource(R.string.ui_daily_moment),
                        onClick = {
                            onDismiss()
                            onOpenDailyMoment()
                        },
                        testTag = "settings_daily_moment_button"
                    )
                    TinyDivider()
                    SettingsNavRow(
                        icon = TinyIcons.MiniGames,
                        title = stringResource(R.string.ui_mini_games),
                        onClick = {
                            onDismiss()
                            onOpenMiniGames()
                        },
                        testTag = "settings_mini_games_button"
                    )
                    TinyDivider()
                    SettingsNavRow(
                        icon = TinyIcons.SharedMood,
                        title = stringResource(R.string.ui_shared_mood),
                        onClick = {
                            onDismiss()
                            onOpenSharedMood()
                        },
                        testTag = "settings_shared_mood_button"
                    )

                    if (com.example.FeatureFlags.PARTNER_SYNC) {
                        TinyDivider()
                        SettingsNavRow(
                            icon = TinyIcons.LongDistance,
                            title = stringResource(R.string.ui_long_distance_signals),
                            onClick = {
                                onDismiss()
                                onOpenLongDistance()
                            },
                            testTag = "settings_long_distance_button"
                        )
                    }
                }
            }

            // -- 6. Tiny Care Notifications --
            SettingsCategoryHeader(
                icon = PixelIcons.VolunteerActivism,
                title = stringResource(R.string.ui_tiny_care),
                subtitle = stringResource(R.string.ui_gentle_wholesome_offline_check_ins_for_e)
            )
            // Tiny Care: Wholesome Offline Reminders
            SettingsSectionCard(modifier = Modifier.testTag("tiny_care_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TinyIconBadge(icon = TinyIcons.Heart)
                    Spacer(modifier = Modifier.width(TinySpace.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_tiny_care), style = TinyType.BodyStrong)
                        Text(
                            stringResource(R.string.ui_tiny_care_description),
                            style = TinyType.Caption,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(TinySpace.sm))
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
                        colors = tinySwitchColors(),
                        modifier = Modifier.testTag("tiny_care_switch")
                    )
                }

                if (showPermissionExplanation) {
                    Text(
                        text = stringResource(R.string.ui_notification_permission_is_needed_so_tin),
                        style = TinyType.Caption.copy(color = TinyColors.Rose)
                    )
                }

                if (tinyCareEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TinyColors.Muted, TinyRadius.Small)
                            .padding(horizontal = TinySpace.md, vertical = TinySpace.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PixelIcons.Nightlight,
                            contentDescription = null,
                            tint = TinyColors.InkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(TinySpace.sm))
                        Text(
                            text = stringResource(R.string.ui_quiet_hours_11_00_pm_7_00_am_sleep_windo),
                            style = TinyType.Caption
                        )
                    }

                    Text(
                        text = stringResource(R.string.ui_remind_me_about),
                        style = TinyType.Label
                    )

                    val categories = com.example.care.TinyCareCategory.values()
                    Column(verticalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                        for (chunk in categories.toList().chunked(2)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)
                            ) {
                                for (cat in chunk) {
                                    val isSelected = cat.id in enabledCategories
                                    TinyChip(
                                        text = cat.title,
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
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(TinySpace.xs)) {
                        TinyButton(
                            text = stringResource(R.string.ui_send_preview_reminder),
                            onClick = {
                                com.example.care.TinyCareScheduler.sendTestNotification(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = TinyButtonStyle.Secondary,
                            icon = TinyIcons.Sparkle,
                            testTag = "tiny_care_test_button"
                        )
                        Text(
                            text = stringResource(R.string.ui_preview_reminder_does_not_count_toward_d),
                            style = TinyType.Micro,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // -- 7. Appearance --
            SettingsCategoryHeader(
                icon = PixelIcons.Palette,
                title = stringResource(R.string.ui_appearance_controls),
                subtitle = stringResource(R.string.ui_frosted_glassmorphism_intensity_for_butt)
            )
            // Button Glassmorphism Setting
            SettingsSectionCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = TinyIcons.Sparkle,
                            contentDescription = null,
                            tint = TinyColors.Rose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(TinySpace.sm))
                        Text(
                            text = stringResource(R.string.ui_button_glassmorphism),
                            style = TinyType.BodyStrong,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(glassIntensity * 100).toInt()}%",
                            style = TinyType.Label.copy(color = TinyColors.Rose)
                        )
                    }
                    Text(
                        text = stringResource(R.string.ui_adjust_the_frosted_translucency_and_shin),
                        style = TinyType.Caption,
                        modifier = Modifier.padding(top = TinySpace.xs)
                    )
                }

                // Live Preview Row on night-sky backdrop (mirrors the dark top bar on purpose)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TinyRadius.Medium)
                        .background(Color(0xFF2B3044))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ui_live_preview),
                        style = TinyType.Micro.copy(color = Color.White.copy(alpha = 0.75f))
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
                                .clip(PixelCircleShape)
                                .background(previewFill)
                                .border(previewBorder, PixelCircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewClear > 0.15f) {
                                Icon(PixelIcons.AutoAwesome, contentDescription = null, tint = Color.Black.copy(alpha = (0.75f * previewClear).coerceIn(0f, 0.85f)), modifier = Modifier.size(16.dp).offset(0.6.dp, 0.6.dp))
                            }
                            Icon(PixelIcons.AutoAwesome, contentDescription = null, tint = previewSparkleTint, modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(PixelCircleShape)
                                .background(previewFill)
                                .border(previewBorder, PixelCircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewClear > 0.15f) {
                                Icon(PixelIcons.Favorite, contentDescription = null, tint = Color.Black.copy(alpha = (0.75f * previewClear).coerceIn(0f, 0.85f)), modifier = Modifier.size(16.dp).offset(0.6.dp, 0.6.dp))
                            }
                            Icon(PixelIcons.Favorite, contentDescription = null, tint = previewHeartTint, modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(PixelCircleShape)
                                .background(previewFill)
                                .border(previewBorder, PixelCircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewClear > 0.15f) {
                                Icon(PixelIcons.Settings, contentDescription = null, tint = Color.Black.copy(alpha = (0.75f * previewClear).coerceIn(0f, 0.85f)), modifier = Modifier.size(16.dp).offset(0.6.dp, 0.6.dp))
                            }
                            Icon(PixelIcons.Settings, contentDescription = null, tint = previewSettingsTint, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Column {
                    Slider(
                        value = glassIntensity,
                        onValueChange = {
                            glassIntensity = it
                            prefs.buttonGlassIntensity = it
                            onSettingsChanged()
                        },
                        valueRange = 0.10f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = TinyColors.Rose,
                            activeTrackColor = TinyColors.Rose,
                            inactiveTrackColor = TinyColors.Rose.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("button_glassmorphism_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.ui_clear_subtle_10), style = TinyType.Micro)
                        Text(stringResource(R.string.ui_frosted_100), style = TinyType.Micro)
                    }
                }
            }

            // -- Privacy Lock & Discreet Mode --
            SettingsCategoryHeader(
                icon = PixelIcons.Lock,
                title = stringResource(R.string.privacy_section_title),
                subtitle = stringResource(R.string.privacy_section_subtitle)
            )
            SettingsSectionCard {
                PrivacyLockSettings()
            }

            SettingsCategoryHeader(
                icon = PixelIcons.Backup,
                title = stringResource(R.string.backup_section_title),
                subtitle = stringResource(R.string.backup_section_subtitle)
            )
            SettingsSectionCard {
                BackupRestoreSettings()
            }

            // -- 8. World Exploration & Privacy --
            SettingsCategoryHeader(
                icon = PixelIcons.Landscape,
                title = stringResource(R.string.ui_world_privacy),
                subtitle = stringResource(R.string.ui_scenes_exploration_and_offline_privacy_g)
            )
            SettingsSectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                    TinyButton(
                        text = stringResource(R.string.ui_select_scene),
                        onClick = {
                            onOpenScenePicker()
                        },
                        modifier = Modifier.weight(1f),
                        style = TinyButtonStyle.Secondary,
                        icon = PixelIcons.Shuffle,
                        testTag = "choose_scene_button"
                    )
                    TinyButton(
                        text = stringResource(R.string.ui_replay_scene),
                        onClick = {
                            onReplayScene()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        style = TinyButtonStyle.Outline,
                        icon = PixelIcons.Replay,
                        testTag = "replay_button"
                    )
                }

                // Wholesome offline assurance card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TinyColors.SageSoft, TinyRadius.Medium)
                        .padding(horizontal = TinySpace.md, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PixelIcons.Lock,
                        contentDescription = null,
                        tint = TinyColors.Sage,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(TinySpace.sm))
                    Text(
                        text = stringResource(R.string.privacy_footer),
                        style = TinyType.Caption.copy(color = TinyColors.Ink)
                    )
                }
            }

            Spacer(modifier = Modifier.height(TinySpace.lg))
        }
    }
}
