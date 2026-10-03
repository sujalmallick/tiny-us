package com.example.ui

import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Lock
import com.example.R
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
internal fun SettingsCategoryHeader(
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
internal fun SettingsSectionCard(
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
                        label = { Text(stringResource(R.string.label_your_name)) },
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
                        label = { Text(stringResource(R.string.label_partner_name)) },
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
                        label = { Text(stringResource(R.string.label_your_birthday)) },
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
                        label = { Text(stringResource(R.string.label_partner_birthday)) },
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
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onOpenAvatarCustomizer()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("settings_open_avatar_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.avatar_entry), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
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
                Button(
                    onClick = {
                        onDismiss()
                        onOpenOurStory()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("settings_our_story_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E6C88)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.story_entry), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

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

                if (com.example.FeatureFlags.PARTNER_SYNC) {
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

            // ── Privacy Lock & Discreet Mode ──
            SettingsCategoryHeader(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.privacy_section_title),
                subtitle = stringResource(R.string.privacy_section_subtitle)
            )
            SettingsSectionCard {
                PrivacyLockSettings()
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsCategoryHeader(
                icon = Icons.Default.Favorite,
                title = stringResource(R.string.backup_section_title),
                subtitle = stringResource(R.string.backup_section_subtitle)
            )
            SettingsSectionCard {
                BackupRestoreSettings()
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        text = stringResource(R.string.privacy_footer),
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
