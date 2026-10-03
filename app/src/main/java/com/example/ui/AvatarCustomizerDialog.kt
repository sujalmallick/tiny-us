package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.AvatarAppearance
import com.example.data.AvatarPalette
import com.example.data.AvatarSwatch
import com.example.engine.AvatarLook
import com.example.engine.CharacterEmotion
import com.example.engine.Direction
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter

private val CardCream = Color(0xFFFFF9F1)
private val InkBrown = Color(0xFF553D36)
private val MutedBrown = Color(0xFF816E62)
private val AccentRose = Color(0xFFD76A7C)
private val ChipBorder = Color(0xFFE8D8C9)

/**
 * "Make Us": each partner picks skin tone, hair colour, hair length and outfit style.
 * Changes are reported live through [onChange] so the world updates behind the dialog.
 */
@Composable
fun AvatarCustomizerDialog(
    nameA: String,
    nameB: String,
    outfitIndexA: Int,
    outfitIndexB: Int,
    initialA: AvatarAppearance,
    initialB: AvatarAppearance,
    onChange: (isSlotB: Boolean, appearance: AvatarAppearance) -> Unit,
    onDismiss: () -> Unit
) {
    var lookA by remember { mutableStateOf(initialA) }
    var lookB by remember { mutableStateOf(initialB) }
    var tab by remember { mutableIntStateOf(0) }

    fun update(isSlotB: Boolean, appearance: AvatarAppearance) {
        if (isSlotB) lookB = appearance else lookA = appearance
        onChange(isSlotB, appearance)
    }

    val editingB = tab == 1
    val current = if (editingB) lookB else lookA

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("avatar_customizer"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardCream),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.avatar_title), color = InkBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.avatar_subtitle), color = MutedBrown, fontSize = 13.sp)

                CouplePreview(lookA, lookB, outfitIndexA, outfitIndexB, highlightB = editingB)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChip(nameA, selected = !editingB, modifier = Modifier.weight(1f).testTag("avatar_tab_a")) { tab = 0 }
                    ToggleChip(nameB, selected = editingB, modifier = Modifier.weight(1f).testTag("avatar_tab_b")) { tab = 1 }
                }

                SectionLabel(stringResource(R.string.avatar_skin))
                SwatchRow(AvatarPalette.skinTones, current.skinTone, "avatar_skin") {
                    update(editingB, current.copy(skinTone = it))
                }

                SectionLabel(stringResource(R.string.avatar_hair_color))
                SwatchRow(AvatarPalette.hairColors, current.hairColor, "avatar_hair") {
                    update(editingB, current.copy(hairColor = it))
                }

                SectionLabel(stringResource(R.string.avatar_hair_length))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChip(stringResource(R.string.avatar_hair_short), !current.longHair, Modifier.weight(1f)) {
                        update(editingB, current.copy(longHair = false))
                    }
                    ToggleChip(stringResource(R.string.avatar_hair_long), current.longHair, Modifier.weight(1f)) {
                        update(editingB, current.copy(longHair = true))
                    }
                }

                SectionLabel(stringResource(R.string.avatar_outfit_style))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChip(stringResource(R.string.avatar_outfit_trousers), !current.wearsDress, Modifier.weight(1f)) {
                        update(editingB, current.copy(wearsDress = false))
                    }
                    ToggleChip(stringResource(R.string.avatar_outfit_dress), current.wearsDress, Modifier.weight(1f)) {
                        update(editingB, current.copy(wearsDress = true))
                    }
                }
                Text(stringResource(R.string.avatar_wardrobe_hint), color = MutedBrown, fontSize = 11.sp)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.avatar_reset),
                        modifier = Modifier.clickable { update(editingB, AvatarAppearance.defaultFor(editingB)) }.padding(8.dp),
                        color = MutedBrown, fontSize = 14.sp
                    )
                    Text(
                        stringResource(R.string.action_done),
                        modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp).testTag("avatar_done"),
                        color = Color(0xFFB74C65), fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CouplePreview(
    lookA: AvatarAppearance,
    lookB: AvatarAppearance,
    outfitIndexA: Int,
    outfitIndexB: Int,
    highlightB: Boolean
) {
    val charA = remember(lookA, outfitIndexA) {
        PixelCharacter(isGirl = false, name = "", worldX = 0f, worldY = 0f, direction = Direction.RIGHT,
            emotion = CharacterEmotion.HAPPY, outfitIndex = outfitIndexA, look = AvatarLook.of(lookA))
    }
    val charB = remember(lookB, outfitIndexB) {
        PixelCharacter(isGirl = true, name = "", worldX = 0f, worldY = 0f, direction = Direction.LEFT,
            emotion = CharacterEmotion.HAPPY, outfitIndex = outfitIndexB, look = AvatarLook.of(lookB))
    }
    Box(
        Modifier.fillMaxWidth().height(150.dp)
            .background(Color(0xFFFDE8E4), RoundedCornerShape(16.dp))
            .border(1.dp, ChipBorder, RoundedCornerShape(16.dp))
            .semantics { contentDescription = "Preview of both characters" }
    ) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val ground = size.height * 0.88f
            drawRect(Color(0xFFB7D99B), topLeft = androidx.compose.ui.geometry.Offset(0f, ground),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - ground))
            val p = size.height / 34f
            // A soft spotlight under whoever is being edited
            val focusX = if (highlightB) size.width * 0.62f else size.width * 0.38f
            drawOval(Color(0x55FFFFFF), topLeft = androidx.compose.ui.geometry.Offset(focusX - 12 * p, ground - 2 * p),
                size = androidx.compose.ui.geometry.Size(24 * p, 4 * p))
            PixelArtRenderer.drawCharacter(this, charA, size.width * 0.38f, ground, p)
            PixelArtRenderer.drawCharacter(this, charB, size.width * 0.62f, ground, p)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = InkBrown, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SwatchRow(swatches: List<AvatarSwatch>, selected: Int, tagPrefix: String, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        swatches.forEachIndexed { index, swatch ->
            val isSelected = index == selected
            Box(
                Modifier
                    .size(36.dp)
                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) AccentRose else ChipBorder, CircleShape)
                    .padding(4.dp)
                    .background(Color(swatch.base), CircleShape)
                    .clickable { onPick(index) }
                    .semantics { contentDescription = swatch.name + if (isSelected) ", selected" else "" }
                    .testTag("${tagPrefix}_$index")
            )
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .border(if (selected) 2.dp else 1.dp, if (selected) AccentRose else ChipBorder, RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFFFEFF1) else Color(0xFFFFFCF8), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color(0xFFB74C65) else InkBrown, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
