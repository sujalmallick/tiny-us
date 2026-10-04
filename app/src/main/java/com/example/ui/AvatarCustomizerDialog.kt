package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AvatarAppearance
import com.example.data.AvatarPalette
import com.example.data.AvatarSwatch
import com.example.engine.AvatarLook
import com.example.engine.CharacterEmotion
import com.example.engine.Direction
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons

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

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("avatar_customizer"),
        contentPadding = PaddingValues(0.dp),
        verticalSpacing = 0.dp
    ) {
        TinyDialogHeader(
            title = stringResource(R.string.avatar_title),
            subtitle = stringResource(R.string.avatar_subtitle),
            icon = PixelIcons.Face,
            modifier = Modifier.padding(start = TinySpace.xl, end = TinySpace.xl, top = TinySpace.xl, bottom = TinySpace.md)
        )

        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = TinySpace.xl, end = TinySpace.xl, bottom = TinySpace.lg),
            verticalArrangement = Arrangement.spacedBy(TinySpace.md)
        ) {
            CouplePreview(lookA, lookB, outfitIndexA, outfitIndexB, highlightB = editingB)

            Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                ToggleChip(nameA, selected = !editingB, modifier = Modifier.testTag("avatar_tab_a")) { tab = 0 }
                ToggleChip(nameB, selected = editingB, modifier = Modifier.testTag("avatar_tab_b")) { tab = 1 }
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
            Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                ToggleChip(stringResource(R.string.avatar_hair_short), !current.longHair) {
                    update(editingB, current.copy(longHair = false))
                }
                ToggleChip(stringResource(R.string.avatar_hair_long), current.longHair) {
                    update(editingB, current.copy(longHair = true))
                }
            }

            SectionLabel(stringResource(R.string.avatar_outfit_style))
            Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                ToggleChip(stringResource(R.string.avatar_outfit_trousers), !current.wearsDress) {
                    update(editingB, current.copy(wearsDress = false))
                }
                ToggleChip(stringResource(R.string.avatar_outfit_dress), current.wearsDress) {
                    update(editingB, current.copy(wearsDress = true))
                }
            }
            Text(stringResource(R.string.avatar_wardrobe_hint), style = TinyType.Caption)
        }

        TinyDivider()

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = TinySpace.md, vertical = TinySpace.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TinyButton(
                text = stringResource(R.string.avatar_reset),
                onClick = { update(editingB, AvatarAppearance.defaultFor(editingB)) },
                style = TinyButtonStyle.Ghost
            )
            TinyButton(
                text = stringResource(R.string.action_done),
                onClick = onDismiss,
                style = TinyButtonStyle.Ghost,
                testTag = "avatar_done"
            )
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
            .clip(TinyRadius.Large)
            .background(Color(0xFFFDE8E4), TinyRadius.Large)
            .border(1.dp, TinyColors.Line, TinyRadius.Large)
            .describedAs(R.string.ui_preview_of_both_characters)
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
    Text(text, style = TinyType.Label, modifier = Modifier.padding(top = TinySpace.xs))
}

@Composable
private fun SwatchRow(swatches: List<AvatarSwatch>, selected: Int, tagPrefix: String, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TinySpace.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches.forEachIndexed { index, swatch ->
            val isSelected = index == selected
            // 48dp touch target around a 36dp swatch; the swatch colour itself is data.
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(PixelCircleShape)
                    .clickable { onPick(index) }
                    .semantics { contentDescription = swatch.name + if (isSelected) ", selected" else "" }
                    .testTag("${tagPrefix}_$index"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) TinyColors.Rose else TinyColors.Line, PixelCircleShape)
                        .padding(4.dp)
                        .background(Color(swatch.base), PixelCircleShape)
                )
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TinyChip(text = label, selected = selected, onClick = onClick, modifier = modifier)
}
