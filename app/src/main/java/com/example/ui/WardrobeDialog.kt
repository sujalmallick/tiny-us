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

data class WardrobeItem(
    val id: Int,
    val name: String,
    val tag: String,
    val description: String,
    val primaryColor: Color,
    val accentColor: Color,
    val isHoodie: Boolean = false
)

data class AccessoryItem(
    val id: Int,
    val name: String,
    val description: String,
    val iconType: String
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
    boyWearsDress: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Girl, 1: Boy
    var filterHoodiesOnly by remember { mutableStateOf(false) }

    var selectedGirlOutfit by remember(currentGirlOutfitIndex) { mutableStateOf(currentGirlOutfitIndex) }
    var selectedGirlAccessory by remember(currentGirlAccessoryIndex) { mutableStateOf(currentGirlAccessoryIndex) }
    var selectedBoyOutfit by remember(currentBoyOutfitIndex) { mutableStateOf(currentBoyOutfitIndex) }
    var selectedBoyAccessory by remember(currentBoyAccessoryIndex) { mutableStateOf(currentBoyAccessoryIndex) }

    val girlDresses = remember(girlfriendName, boyfriendName) {
        listOf(
            WardrobeItem(
                id = 0,
                name = "Strawberry Cream Sundress",
                tag = "Signature Classic",
                description = "Soft blush-pink sweater and rosy pleated skirt. The timeless outfit $boyfriendName fell in love with.",
                primaryColor = Color(0xFFF5CAC3),
                accentColor = Color(0xFFD6587A)
            ),
            WardrobeItem(
                id = 1,
                name = "Lavender Dream Wrap Dress",
                tag = "Garden Stroll",
                description = "Delicate lilac petals woven into flowing silk. Smells like blooming lavender and gentle morning breezes.",
                primaryColor = Color(0xFFE8D7F1),
                accentColor = Color(0xFF9D4EDD)
            ),
            WardrobeItem(
                id = 2,
                name = "Emerald Velvet Romance",
                tag = "Candlelit Evening",
                description = "Deep forest emerald velvet with shimmering dark green folds. Made for rooftop starlight and warm slow dances.",
                primaryColor = Color(0xFF74C69D),
                accentColor = Color(0xFF2D6A4F)
            ),
            WardrobeItem(
                id = 3,
                name = "Lemon Sunshine Picnic Dress",
                tag = "Meadow Picnic",
                description = "Cheerful lemon-yellow linen with sweet honey accents. Brings warm golden sunshine wherever $girlfriendName walks.",
                primaryColor = Color(0xFFFFF3B0),
                accentColor = Color(0xFFE9C46A)
            ),
            WardrobeItem(
                id = 4,
                name = "Midnight Starlight Gown",
                tag = "Midnight Date",
                description = "Deep midnight blue with celestial starlight highlights. For whispering secrets under constellations.",
                primaryColor = Color(0xFF4A6FA5),
                accentColor = Color(0xFF1E3A8A)
            ),
            WardrobeItem(
                id = 5,
                name = "Mint Macaron Tea Dress",
                tag = "Cozy Cafe",
                description = "Sweet pastel mint chiffon as light as a daydream. Perfect for sipping steaming tea by the cottage window.",
                primaryColor = Color(0xFFC3DBD0),
                accentColor = Color(0xFF6B9080)
            ),
            WardrobeItem(
                id = 6,
                name = "$boyfriendName's Stolen Oversized Flannel",
                tag = "Stolen with Love",
                description = "Comfortable deep blue flannel shirt. Originally belonged to $boyfriendName, but $girlfriendName claimed it forever because it smells like home.",
                primaryColor = Color(0xFF457B9D),
                accentColor = Color(0xFF1D3557)
            ),
            WardrobeItem(
                id = 7,
                name = "Blush Rose Cropped Hoodie",
                tag = "Cozy Streetwear",
                description = "Soft blush-rose cropped knit hoodie with a flared pleated skirt. Wonderfully cozy for cool afternoon strolls.",
                primaryColor = Color(0xFFF4ACB7),
                accentColor = Color(0xFFFFCAD4),
                isHoodie = true
            ),
            WardrobeItem(
                id = 8,
                name = "Sage & Cream Colorblock Hoodie",
                tag = "Forest Breeze",
                description = "Earthy sage and fresh cream colorblock hoodie with a linen skirt. Feels like a quiet walk through misty pines.",
                primaryColor = Color(0xFF84A98C),
                accentColor = Color(0xFF52796F),
                isHoodie = true
            ),
            WardrobeItem(
                id = 9,
                name = "Lavender Cloud Oversized Hoodie",
                tag = "Cloud Cozy",
                description = "Fluffy lavender fleece oversized hoodie with denim skirt. Like wrapping up inside a warm, sweet-scented cloud.",
                primaryColor = Color(0xFFD8BBFF),
                accentColor = Color(0xFF3D5A80),
                isHoodie = true
            ),
            WardrobeItem(
                id = 10,
                name = "Buttercream Star Shimmer Hoodie",
                tag = "Golden Glow",
                description = "Sunny buttercream hoodie with warm honey shimmer and golden accents. Radiates gentle warmth and smiles.",
                primaryColor = Color(0xFFFFF1C5),
                accentColor = Color(0xFFFFD166),
                isHoodie = true
            )
        )
    }

    val boyOutfits = remember(girlfriendName, boyfriendName) {
        listOf(
            WardrobeItem(
                id = 0,
                name = "Classic Spruce Knit & Navy Pants",
                tag = "Signature Everyday",
                description = "$boyfriendName's iconic spruce green sweater paired with relaxed navy trousers. Trusty, warm, and familiar.",
                primaryColor = Color(0xFF2D6A4F),
                accentColor = Color(0xFF1B4332),
                isHoodie = false
            ),
            WardrobeItem(
                id = 1,
                name = "White & Emerald Varsity Hoodie",
                tag = "Varsity Campus",
                description = "Clean white hoodie with deep emerald varsity stripes, kangaroo pouch, and khaki chinos. Sharp and sporty.",
                primaryColor = Color(0xFFF8F9FA),
                accentColor = Color(0xFF2D6A4F),
                isHoodie = true
            ),
            WardrobeItem(
                id = 2,
                name = "Charcoal Streetwear Zip Hoodie",
                tag = "Urban Evening",
                description = "Heavy charcoal zip-up hoodie over dark indigo denim. Modern, comfortable, and effortlessly cool.",
                primaryColor = Color(0xFF343A40),
                accentColor = Color(0xFF495057),
                isHoodie = true
            ),
            WardrobeItem(
                id = 3,
                name = "Oatmeal Cloud Oversized Hoodie",
                tag = "Weekend Comfort",
                description = "Ultra-soft oatmeal heather oversized hoodie with comfortable slate cargo trousers. Maximum cozy lounging.",
                primaryColor = Color(0xFFEDE0D4),
                accentColor = Color(0xFFB08968),
                isHoodie = true
            ),
            WardrobeItem(
                id = 4,
                name = "Midnight Starlight Graphic Hoodie",
                tag = "Stargazing Date",
                description = "Deep starlight navy hoodie with celestial accents over black denim. Matches $girlfriendName's evening starlight look.",
                primaryColor = Color(0xFF1E293B),
                accentColor = Color(0xFF64748B),
                isHoodie = true
            )
        )
    }

    val accessories = remember {
        listOf(
            AccessoryItem(
                id = 0,
                name = "Natural Look",
                description = "No headwear or neck accessory.",
                iconType = "none"
            ),
            AccessoryItem(
                id = 1,
                name = "Cozy Beanie",
                description = "Ribbed knit beanie with fluffy pom-pom.",
                iconType = "beanie"
            ),
            AccessoryItem(
                id = 2,
                name = "Wool Scarf",
                description = "Warm chunky wool scarf with gentle fringe.",
                iconType = "scarf"
            ),
            AccessoryItem(
                id = 3,
                name = "Baseball Cap",
                description = "Casual streetwear twill cap with forward visor.",
                iconType = "cap"
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("wardrobe_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = CozyCream,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Cottage Wardrobe",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFFC9184A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Outfits & accessories for both of you",
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Serif,
                    color = DarkSlate.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // His / Hers Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEDE0D4).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                            .testTag("wardrobe_tab_girl"),
                        shape = RoundedCornerShape(11.dp),
                        color = if (selectedTab == 0) DeepRose else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$girlfriendName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color.White else DarkSlate.copy(alpha = 0.75f)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                            .testTag("wardrobe_tab_boy"),
                        shape = RoundedCornerShape(11.dp),
                        color = if (selectedTab == 1) DeepRose else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$boyfriendName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) Color.White else DarkSlate.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Accessories Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Accessories",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Text(
                            text = if (selectedTab == 1) "Glasses stay on" else "Layers over outfits",
                            fontSize = 10.sp,
                            color = DarkSlate.copy(alpha = 0.6f)
                        )
                    }

                    // Accessories Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accessories.forEach { acc ->
                            val isAccWearing = (currentAccessory == acc.id)
                            Surface(
                                onClick = {
                                    if (selectedTab == 0) {
                                        selectedGirlAccessory = acc.id
                                        onSelectGirlAccessory(acc.id)
                                    } else {
                                        selectedBoyAccessory = acc.id
                                        onSelectBoyAccessory(acc.id)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAccWearing) Color(0xFFFFECEF) else Color.White,
                                border = BorderStroke(
                                    width = if (isAccWearing) 1.5.dp else 1.dp,
                                    color = if (isAccWearing) DeepRose else Color(0xFFE9ECEF)
                                ),
                                modifier = Modifier.testTag("wardrobe_accessory_${acc.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
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
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = acc.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isAccWearing) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAccWearing) DeepRose else DarkSlate
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Outfits Section Header with filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Dresses & Hoodies" else "Sweaters & Hoodies",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = { filterHoodiesOnly = false },
                                shape = RoundedCornerShape(8.dp),
                                color = if (!filterHoodiesOnly) DeepRose.copy(alpha = 0.15f) else Color.White,
                                border = BorderStroke(1.dp, if (!filterHoodiesOnly) DeepRose else Color(0xFFE9ECEF))
                            ) {
                                Text(
                                    text = "All",
                                    fontSize = 10.sp,
                                    fontWeight = if (!filterHoodiesOnly) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!filterHoodiesOnly) DeepRose else DarkSlate.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                onClick = { filterHoodiesOnly = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (filterHoodiesOnly) DeepRose.copy(alpha = 0.15f) else Color.White,
                                border = BorderStroke(1.dp, if (filterHoodiesOnly) DeepRose else Color(0xFFE9ECEF))
                            ) {
                                Text(
                                    text = "Hoodies Only",
                                    fontSize = 10.sp,
                                    fontWeight = if (filterHoodiesOnly) FontWeight.Bold else FontWeight.Normal,
                                    color = if (filterHoodiesOnly) DeepRose else DarkSlate.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Outfits List
                    outfitList.forEach { item ->
                        val isWearing = (currentOutfit == item.id)
                        val onWearThisOutfit = {
                            if (selectedTab == 0) {
                                selectedGirlOutfit = item.id
                                onSelectGirlOutfit(item.id)
                            } else {
                                selectedBoyOutfit = item.id
                                onSelectBoyOutfit(item.id)
                            }
                        }
                        Surface(
                            onClick = onWearThisOutfit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wardrobe_item_${item.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isWearing) Color(0xFFFFECEF) else Color.White,
                            border = BorderStroke(
                                width = if (isWearing) 1.5.dp else 1.dp,
                                color = if (isWearing) DeepRose else Color(0xFFE9ECEF)
                            ),
                            shadowElevation = if (isWearing) 2.dp else 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pixel Mini Preview Icon
                                Surface(
                                    modifier = Modifier.size(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = item.primaryColor.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.4f))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val p = size.width / 16f
                                        // Hanger
                                        drawRect(Color(0xFFFFD166), Offset(6 * p, 2 * p), Size(4 * p, p))
                                        drawRect(Color(0xFF8C6D37), Offset(7.5f * p, p), Size(p, p))

                                        if (selectedTab == 0) {
                                            // Girl outfit / dress / hoodie preview
                                            drawRect(item.primaryColor, Offset(5 * p, 3.5f * p), Size(6 * p, 4 * p))
                                            drawRect(item.accentColor, Offset(6 * p, 3.5f * p), Size(4 * p, 1.2f * p))
                                            if (item.isHoodie) {
                                                // Kangaroo pouch & drawstrings
                                                drawRect(item.accentColor, Offset(5.5f * p, 5.5f * p), Size(5 * p, 2 * p))
                                                drawRect(item.accentColor, Offset(6.2f * p, 4.5f * p), Size(0.7f * p, 2 * p))
                                                drawRect(item.accentColor, Offset(9.1f * p, 4.5f * p), Size(0.7f * p, 2 * p))
                                                // Pleated skirt below
                                                drawRect(item.accentColor, Offset(4 * p, 7.5f * p), Size(8 * p, 6.5f * p))
                                            } else {
                                                // Flared Skirt
                                                drawRect(item.accentColor, Offset(4 * p, 7.5f * p), Size(8 * p, 6.5f * p))
                                                drawRect(item.primaryColor, Offset(4 * p, 13 * p), Size(8 * p, 1.2f * p))
                                            }
                                        } else {
                                            // Boy outfit / sweater / hoodie preview
                                            drawRect(item.primaryColor, Offset(4.5f * p, 3.5f * p), Size(7 * p, 6 * p))
                                            drawRect(item.accentColor, Offset(6 * p, 3.5f * p), Size(4 * p, 1.2f * p))
                                            if (item.isHoodie) {
                                                drawRect(item.accentColor, Offset(5.5f * p, 6.5f * p), Size(5 * p, 2.5f * p))
                                                drawRect(item.accentColor, Offset(6.2f * p, 4.5f * p), Size(0.7f * p, 2.5f * p))
                                                drawRect(item.accentColor, Offset(9.1f * p, 4.5f * p), Size(0.7f * p, 2.5f * p))
                                            }
                                            // Trousers
                                            drawRect(item.accentColor, Offset(5 * p, 9.5f * p), Size(3 * p, 5 * p))
                                            drawRect(item.accentColor, Offset(8 * p, 9.5f * p), Size(3 * p, 5 * p))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif,
                                            color = DarkSlate,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (item.isHoodie) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = DeepRose.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.5.dp, DeepRose.copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        text = "HOODIE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DeepRose,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = item.accentColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = item.tag,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = item.accentColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = item.description,
                                        fontSize = 11.sp,
                                        color = DarkSlate.copy(alpha = 0.7f),
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isWearing) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DeepRose.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, DeepRose.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "Wearing Now",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepRose,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = onWearThisOutfit,
                                            colors = ButtonDefaults.buttonColors(containerColor = item.accentColor),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Wear Outfit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wardrobe_close_button")
                ) {
                    Text("Close Wardrobe", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
