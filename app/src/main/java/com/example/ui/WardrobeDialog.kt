package com.example.ui

import com.example.R
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.material.icons.rounded.Checkroom
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons

data class WardrobeItem(
    val id: Int,
    val name: String,
    val tag: String,
    val description: String,
    val primaryColor: Color,
    val accentColor: Color,
    val isHoodie: Boolean = false,
    /** Set for a reward item (plan 07): locked until its little first is earned. */
    val reward: String? = null
)

data class AccessoryItem(
    val id: Int,
    val name: String,
    val description: String,
    val iconType: String,
    /** Set for a reward item (plan 07): locked until its little first is earned. */
    val reward: String? = null
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
    onDismiss: () -> Unit,
    girlWearsDress: Boolean = true,
    boyWearsDress: Boolean = false,
    /** The couple's looks, so each card shows them in the outfit (skin, hair) rather than a stand-in. */
    girlLook: com.example.engine.AvatarLook = com.example.engine.AvatarLook.defaultFor(true),
    boyLook: com.example.engine.AvatarLook = com.example.engine.AvatarLook.defaultFor(false),
    /** Rewards the couple has unlocked (plan 07); reward items not in it show locked. */
    unlocked: Set<String> = emptySet()
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Girl, 1: Boy
    // The catalogue below is built inside remember { }, so it reads its text through resources.
    val res = LocalContext.current.resources
    var filterHoodiesOnly by remember { mutableStateOf(false) }

    var selectedGirlOutfit by remember(currentGirlOutfitIndex) { mutableStateOf(currentGirlOutfitIndex) }
    var selectedGirlAccessory by remember(currentGirlAccessoryIndex) { mutableStateOf(currentGirlAccessoryIndex) }
    var selectedBoyOutfit by remember(currentBoyOutfitIndex) { mutableStateOf(currentBoyOutfitIndex) }
    var selectedBoyAccessory by remember(currentBoyAccessoryIndex) { mutableStateOf(currentBoyAccessoryIndex) }

    val girlDresses = remember(girlfriendName, boyfriendName) {
        listOf(
            WardrobeItem(
                id = 0,
                name = res.getString(R.string.wardrobe_strawberry_cream_sundress),
                tag = res.getString(R.string.wardrobe_signature_classic),
                description = res.getString(R.string.wardrobe_soft_blush_pink_sweater_and_rosy_p, boyfriendName),
                primaryColor = Color(0xFFF5CAC3),
                accentColor = Color(0xFFD6587A)
            ),
            WardrobeItem(
                id = 1,
                name = res.getString(R.string.wardrobe_lavender_dream_wrap_dress),
                tag = res.getString(R.string.wardrobe_garden_stroll),
                description = res.getString(R.string.wardrobe_delicate_lilac_petals_woven_into_f),
                primaryColor = Color(0xFFE8D7F1),
                accentColor = Color(0xFF9D4EDD)
            ),
            WardrobeItem(
                id = 2,
                name = res.getString(R.string.wardrobe_emerald_velvet_romance),
                tag = res.getString(R.string.wardrobe_candlelit_evening),
                description = res.getString(R.string.wardrobe_deep_forest_emerald_velvet_with_sh),
                primaryColor = Color(0xFF74C69D),
                accentColor = Color(0xFF2D6A4F)
            ),
            WardrobeItem(
                id = 3,
                name = res.getString(R.string.wardrobe_lemon_sunshine_picnic_dress),
                tag = res.getString(R.string.wardrobe_meadow_picnic),
                description = res.getString(R.string.wardrobe_cheerful_lemon_yellow_linen_with_s, girlfriendName),
                primaryColor = Color(0xFFFFF3B0),
                accentColor = Color(0xFFE9C46A)
            ),
            WardrobeItem(
                id = 4,
                name = res.getString(R.string.wardrobe_midnight_starlight_gown),
                tag = res.getString(R.string.wardrobe_midnight_date),
                description = res.getString(R.string.wardrobe_deep_midnight_blue_with_celestial),
                primaryColor = Color(0xFF4A6FA5),
                accentColor = Color(0xFF1E3A8A)
            ),
            WardrobeItem(
                id = 5,
                name = res.getString(R.string.wardrobe_mint_macaron_tea_dress),
                tag = res.getString(R.string.wardrobe_cozy_cafe),
                description = res.getString(R.string.wardrobe_sweet_pastel_mint_chiffon_as_light),
                primaryColor = Color(0xFFC3DBD0),
                accentColor = Color(0xFF6B9080)
            ),
            WardrobeItem(
                id = 6,
                name = res.getString(R.string.wardrobe_s_stolen_oversized_flannel, boyfriendName),
                tag = res.getString(R.string.wardrobe_stolen_with_love),
                description = res.getString(R.string.wardrobe_comfortable_deep_blue_flannel_shir, boyfriendName, girlfriendName),
                primaryColor = Color(0xFF457B9D),
                accentColor = Color(0xFF1D3557)
            ),
            WardrobeItem(
                id = 7,
                name = res.getString(R.string.wardrobe_blush_rose_cropped_hoodie),
                tag = res.getString(R.string.wardrobe_cozy_streetwear),
                description = res.getString(R.string.wardrobe_soft_blush_rose_cropped_knit_hoodi),
                primaryColor = Color(0xFFF4ACB7),
                accentColor = Color(0xFFFFCAD4),
                isHoodie = true
            ),
            WardrobeItem(
                id = 8,
                name = res.getString(R.string.wardrobe_sage_cream_colorblock_hoodie),
                tag = res.getString(R.string.wardrobe_forest_breeze),
                description = res.getString(R.string.wardrobe_earthy_sage_and_fresh_cream_colorb),
                primaryColor = Color(0xFF84A98C),
                accentColor = Color(0xFF52796F),
                isHoodie = true
            ),
            WardrobeItem(
                id = 9,
                name = res.getString(R.string.wardrobe_lavender_cloud_oversized_hoodie),
                tag = res.getString(R.string.wardrobe_cloud_cozy),
                description = res.getString(R.string.wardrobe_fluffy_lavender_fleece_oversized_h),
                primaryColor = Color(0xFFD8BBFF),
                accentColor = Color(0xFF3D5A80),
                isHoodie = true
            ),
            WardrobeItem(
                id = 10,
                name = res.getString(R.string.wardrobe_buttercream_star_shimmer_hoodie),
                tag = res.getString(R.string.wardrobe_golden_glow),
                description = res.getString(R.string.wardrobe_sunny_buttercream_hoodie_with_warm),
                primaryColor = Color(0xFFFFF1C5),
                accentColor = Color(0xFFFFD166),
                isHoodie = true
            ),
            WardrobeItem(
                id = com.example.progress.RewardItems.STAR_HOODIE_DRESS_INDEX,
                name = res.getString(R.string.wardrobe_star_hoodie),
                tag = res.getString(R.string.wardrobe_star_hoodie_tag),
                description = res.getString(R.string.wardrobe_star_hoodie_desc),
                primaryColor = Color(0xFF2A3A6B),
                accentColor = Color(0xFFFFD166),
                isHoodie = true,
                reward = com.example.progress.Rewards.STAR_HOODIE
            )
        )
    }

    val boyOutfits = remember(girlfriendName, boyfriendName) {
        listOf(
            WardrobeItem(
                id = 0,
                name = res.getString(R.string.wardrobe_classic_spruce_knit_navy_pants),
                tag = res.getString(R.string.wardrobe_signature_everyday),
                description = res.getString(R.string.wardrobe_s_iconic_spruce_green_sweater_pair, boyfriendName),
                primaryColor = Color(0xFF2D6A4F),
                accentColor = Color(0xFF1B4332),
                isHoodie = false
            ),
            WardrobeItem(
                id = 1,
                name = res.getString(R.string.wardrobe_white_emerald_varsity_hoodie),
                tag = res.getString(R.string.wardrobe_varsity_campus),
                description = res.getString(R.string.wardrobe_clean_white_hoodie_with_deep_emera),
                primaryColor = Color(0xFFF8F9FA),
                accentColor = Color(0xFF2D6A4F),
                isHoodie = true
            ),
            WardrobeItem(
                id = 2,
                name = res.getString(R.string.wardrobe_charcoal_streetwear_zip_hoodie),
                tag = res.getString(R.string.wardrobe_urban_evening),
                description = res.getString(R.string.wardrobe_heavy_charcoal_zip_up_hoodie_over),
                primaryColor = Color(0xFF343A40),
                accentColor = Color(0xFF495057),
                isHoodie = true
            ),
            WardrobeItem(
                id = 3,
                name = res.getString(R.string.wardrobe_oatmeal_cloud_oversized_hoodie),
                tag = res.getString(R.string.wardrobe_weekend_comfort),
                description = res.getString(R.string.wardrobe_ultra_soft_oatmeal_heather_oversiz),
                primaryColor = Color(0xFFEDE0D4),
                accentColor = Color(0xFFB08968),
                isHoodie = true
            ),
            WardrobeItem(
                id = 4,
                name = res.getString(R.string.wardrobe_midnight_starlight_graphic_hoodie),
                tag = res.getString(R.string.wardrobe_stargazing_date),
                description = res.getString(R.string.wardrobe_deep_starlight_navy_hoodie_with_ce, girlfriendName),
                primaryColor = Color(0xFF1E293B),
                accentColor = Color(0xFF64748B),
                isHoodie = true
            ),
            WardrobeItem(
                id = com.example.progress.RewardItems.STAR_HOODIE_TROUSER_INDEX,
                name = res.getString(R.string.wardrobe_star_hoodie),
                tag = res.getString(R.string.wardrobe_star_hoodie_tag),
                description = res.getString(R.string.wardrobe_star_hoodie_desc),
                primaryColor = Color(0xFF2A3A6B),
                accentColor = Color(0xFFFFD166),
                isHoodie = true,
                reward = com.example.progress.Rewards.STAR_HOODIE
            )
        )
    }

    val accessories = remember {
        listOf(
            AccessoryItem(
                id = 0,
                name = res.getString(R.string.wardrobe_natural_look),
                description = res.getString(R.string.wardrobe_no_headwear_or_neck_accessory),
                iconType = "none"
            ),
            AccessoryItem(
                id = 1,
                name = res.getString(R.string.wardrobe_cozy_beanie),
                description = res.getString(R.string.wardrobe_ribbed_knit_beanie_with_fluffy_pom),
                iconType = "beanie"
            ),
            AccessoryItem(
                id = 2,
                name = res.getString(R.string.wardrobe_wool_scarf),
                description = res.getString(R.string.wardrobe_warm_chunky_wool_scarf_with_gentle),
                iconType = "scarf"
            ),
            AccessoryItem(
                id = 3,
                name = res.getString(R.string.wardrobe_baseball_cap),
                description = res.getString(R.string.wardrobe_casual_streetwear_twill_cap_with_f),
                iconType = "cap"
            ),
            AccessoryItem(
                id = com.example.progress.RewardItems.RAINBOW_SCARF_INDEX,
                name = res.getString(R.string.wardrobe_rainbow_scarf),
                description = res.getString(R.string.wardrobe_rainbow_scarf_desc),
                iconType = "scarf",
                reward = com.example.progress.Rewards.RAINBOW_SCARF
            ),
            AccessoryItem(
                id = com.example.progress.RewardItems.SNOWMAN_BEANIE_INDEX,
                name = res.getString(R.string.wardrobe_snowman_beanie),
                description = res.getString(R.string.wardrobe_snowman_beanie_desc),
                iconType = "beanie",
                reward = com.example.progress.Rewards.SNOWMAN_BEANIE
            )
        )
    }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("wardrobe_dialog")
    ) {
        TinyDialogHeader(
            title = stringResource(R.string.ui_cottage_wardrobe),
            subtitle = stringResource(R.string.ui_outfits_accessories_for_both_of_you),
            icon = PixelIcons.Checkroom
        )

        // His / Hers segmented switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TinyColors.Muted, TinyRadius.Medium)
                .padding(TinySpace.xs),
            horizontalArrangement = Arrangement.spacedBy(TinySpace.xs)
        ) {
            WardrobeSegment(
                text = "$girlfriendName",
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("wardrobe_tab_girl")
            )
            WardrobeSegment(
                text = "$boyfriendName",
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("wardrobe_tab_boy")
            )
        }

        // Scrollable main content
        val currentAccessory = if (selectedTab == 0) selectedGirlAccessory else selectedBoyAccessory
        val currentOutfit = if (selectedTab == 0) selectedGirlOutfit else selectedBoyOutfit
        val tabWearsDress = if (selectedTab == 0) girlWearsDress else boyWearsDress
        val rawOutfitList = if (tabWearsDress) girlDresses else boyOutfits
        val outfitList = if (filterHoodiesOnly) rawOutfitList.filter { it.isHoodie } else rawOutfitList

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TinySpace.md)
        ) {
            // Accessories Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.ui_accessories),
                    style = TinyType.Section
                )
                Text(
                    text = if (selectedTab == 1) stringResource(R.string.wardrobe_glasses_stay_on) else stringResource(R.string.wardrobe_layers_over_outfits),
                    style = TinyType.Caption
                )
            }

            // Accessories Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                accessories.forEach { acc ->
                    val isAccWearing = (currentAccessory == acc.id)
                    val accLocked = acc.reward != null && acc.reward !in unlocked
                    // TinyChip styling, with the pixel accessory preview (content) in a small Card-coloured disc
                    Surface(
                        selected = isAccWearing,
                        onClick = {
                            if (accLocked) return@Surface
                            if (selectedTab == 0) {
                                selectedGirlAccessory = acc.id
                                onSelectGirlAccessory(acc.id)
                            } else {
                                selectedBoyAccessory = acc.id
                                onSelectBoyAccessory(acc.id)
                            }
                        },
                        shape = TinyRadius.Pill,
                        color = if (isAccWearing) TinyColors.Rose else TinyColors.Muted,
                        contentColor = if (isAccWearing) Color.White else TinyColors.Ink,
                        modifier = Modifier.testTag("wardrobe_accessory_${acc.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(TinyColors.Card, PixelCircleShape),
                                contentAlignment = Alignment.Center
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
                            }
                            Spacer(modifier = Modifier.width(TinySpace.sm))
                            if (accLocked) {
                                Icon(PixelIcons.Lock, contentDescription = stringResource(R.string.reward_locked), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = acc.name,
                                style = TinyType.Label.copy(color = Color.Unspecified),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Outfits Section Header with filter
            Column(verticalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                Text(
                    text = if (selectedTab == 0) stringResource(R.string.wardrobe_dresses_and_hoodies) else stringResource(R.string.wardrobe_sweaters_and_hoodies),
                    style = TinyType.Section
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                    TinyChip(
                        text = stringResource(R.string.ui_all),
                        selected = !filterHoodiesOnly,
                        onClick = { filterHoodiesOnly = false }
                    )
                    TinyChip(
                        text = stringResource(R.string.ui_hoodies_only),
                        selected = filterHoodiesOnly,
                        onClick = { filterHoodiesOnly = true }
                    )
                }
            }

            // Outfits List
            outfitList.forEach { item ->
                val isWearing = (currentOutfit == item.id)
                val locked = item.reward != null && item.reward !in unlocked
                val onWearThisOutfit = onWear@{
                    if (locked) return@onWear
                    if (selectedTab == 0) {
                        selectedGirlOutfit = item.id
                        onSelectGirlOutfit(item.id)
                    } else {
                        selectedBoyOutfit = item.id
                        onSelectBoyOutfit(item.id)
                    }
                }
                TinyCard(
                    onClick = onWearThisOutfit,
                    selected = isWearing,
                    padding = TinySpace.md,
                    modifier = Modifier.testTag("wardrobe_item_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // The character in this outfit, drawn by the same renderer as the scene, so a
                        // hoodie shows as a hoodie (it used to be two colour blocks and a tag).
                        Surface(
                            modifier = Modifier.size(width = 52.dp, height = 64.dp)
                                .then(if (locked) Modifier.alpha(0.35f) else Modifier),
                            shape = TinyRadius.Medium,
                            color = item.primaryColor.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.4f))
                        ) {
                            val isGirlTab = selectedTab == 0
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val model = com.example.engine.PixelCharacter(
                                    isGirl = isGirlTab, name = "", worldX = 0f, worldY = 0f
                                ).apply {
                                    outfitIndex = item.id
                                    look = if (isGirlTab) girlLook else boyLook
                                }
                                // Whole pixels that fit the sprite (18 x 26 grid) in the card.
                                val fit = minOf(size.width / 18f, size.height * 0.92f / 26f) / com.example.engine.PixelArtRenderer.CHARACTER_SCALE_FACTOR
                                com.example.engine.PixelArtRenderer.drawCharacter(
                                    drawScope = this,
                                    char = model,
                                    centerX = size.width / 2f,
                                    bottomY = size.height * 0.97f,
                                    pixelSize = kotlin.math.floor(fit).coerceAtLeast(1f),
                                    snapToPixel = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(TinySpace.md))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(TinySpace.xs)
                        ) {
                            Text(
                                text = item.name,
                                style = TinyType.Section.copy(fontSize = 14.sp, lineHeight = 19.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(TinySpace.xs)
                            ) {
                                if (item.isHoodie) {
                                    TinyTag(
                                        text = stringResource(R.string.ui_hoodie),
                                        color = TinyColors.Rose,
                                        background = if (isWearing) TinyColors.Card else TinyColors.RoseSoft
                                    )
                                }
                                TinyTag(
                                    text = item.tag,
                                    background = if (isWearing) TinyColors.Card else TinyColors.Muted
                                )
                            }

                            if (locked) {
                                val first = item.reward?.let { com.example.progress.RewardItems.earnedBy(it) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(PixelIcons.Lock, contentDescription = null, tint = TinyColors.InkMuted, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.reward_unlock_with, first?.let { stringResource(it.title) } ?: ""),
                                        style = TinyType.Caption
                                    )
                                }
                            } else {
                                Text(
                                    text = item.description,
                                    style = TinyType.Caption
                                )
                            }

                            Spacer(modifier = Modifier.height(TinySpace.xs))

                            if (isWearing) {
                                TinyTag(
                                    text = stringResource(R.string.ui_wearing_now),
                                    color = TinyColors.Rose,
                                    background = TinyColors.Card
                                )
                            } else {
                                TinyButton(
                                    text = stringResource(R.string.ui_wear_outfit),
                                    onClick = onWearThisOutfit,
                                    style = TinyButtonStyle.Secondary,
                                    compact = true
                                )
                            }
                        }
                    }
                }
            }
        }

        TinyButton(
            text = stringResource(R.string.ui_close_wardrobe),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            style = TinyButtonStyle.Secondary,
            testTag = "wardrobe_close_button"
        )
    }
}

/** One half of the His / Hers segmented switcher. */
@Composable
private fun WardrobeSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        shape = TinyRadius.Small,
        color = if (selected) TinyColors.Card else Color.Transparent,
        contentColor = if (selected) TinyColors.Rose else TinyColors.InkMuted,
        border = if (selected) BorderStroke(1.dp, TinyColors.Line) else null
    ) {
        Box(
            modifier = Modifier.padding(horizontal = TinySpace.sm, vertical = TinySpace.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TinyType.Label.copy(color = Color.Unspecified),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
