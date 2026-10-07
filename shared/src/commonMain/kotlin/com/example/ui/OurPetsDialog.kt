package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.data.PetKind
import com.example.data.PetStore
import com.example.resources.*
import com.example.scene.CatState
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Plan 10, E: "Our pets". Every pet they've met, and who lives with them. The others are shadows
 * with a hint of where they might be met; Mochi, when someone else lives with them, is on a
 * sleepover at Grandpa Bao's and can come home any time.
 */

@Composable
fun OurPetsDialog(store: PetStore, current: PetKind, catName: String, onChoose: (PetKind) -> Unit, onDismiss: () -> Unit) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("our_pets_dialog")) {
        OurPetsPanel(store, current, catName, onChoose, onDismiss)
    }
}

@Composable
fun ColumnScope.OurPetsPanel(store: PetStore, current: PetKind, catName: String, onChoose: (PetKind) -> Unit, onDismiss: () -> Unit) {
    var living by remember { mutableStateOf(current) }
    val met = remember { store.met() }
    TinyDialogHeader(
        title = stringResource(Res.string.our_pets),
        subtitle = stringResource(Res.string.our_pets_sub),
        icon = PixelIcons.Favorite,
        onClose = onDismiss
    )
    Text(
        stringResource(Res.string.our_pets_met, met.size, PetKind.entries.size),
        style = TinyType.Caption
    )
    Column(
        Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
    ) {
        for (kind in PetKind.entries) {
            val isMet = kind in met
            PetCard(
                kind = kind,
                name = if (kind == PetKind.CAT) catName else kind.defaultName,
                isMet = isMet,
                lives = kind == living,
                onChoose = {
                    living = kind
                    onChoose(kind)
                }
            )
        }
    }
}

@Composable
private fun PetCard(kind: PetKind, name: String, isMet: Boolean, lives: Boolean, onChoose: () -> Unit) {
    TinyCard(padding = TinySpace.md, modifier = Modifier.testTag("pet_card_${kind.name.lowercase()}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // On a little patch of grass, so white bunnies and ducks show up
            val patch = when {
                !isMet -> TinyColors.Muted
                lives -> androidx.compose.ui.graphics.Color(0xFFC9E4B8)
                else -> androidx.compose.ui.graphics.Color(0xFFDCEBD0)
            }
            Canvas(Modifier.size(56.dp).background(patch, PixelCircleShape)) {
                val p = size.width / 26f
                val ground = size.height * 0.78f
                if (!isMet) {
                    // A shadow of someone not met yet
                    drawCircle(TinyColors.InkMuted.copy(alpha = 0.25f), radius = size.width * 0.18f, center = androidx.compose.ui.geometry.Offset(size.width / 2f, ground - 5f * p))
                } else if (kind == PetKind.CAT) {
                    com.example.engine.WorldSprites.drawCat(this, size.width / 2f, ground, p, catState = CatState.SITTING_PURR, isSleeping = false)
                } else {
                    PetSprites.drawPet(this, kind, size.width / 2f, ground, p, 0.3f, CatState.SITTING_PURR, facingLeft = false)
                }
            }
            Spacer(Modifier.width(TinySpace.md))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isMet) name else stringResource(Res.string.collection_mystery),
                    style = TinyType.BodyStrong.copy(color = if (isMet) TinyColors.Ink else TinyColors.InkMuted)
                )
                Text(
                    stringResource(if (isMet) petAbout(kind) else petHint(kind)),
                    style = TinyType.Caption,
                    modifier = Modifier.padding(top = 2.dp)
                )
                when {
                    lives -> Text(stringResource(Res.string.pets_lives_with_you), style = TinyType.Label.copy(color = TinyColors.Rose), modifier = Modifier.padding(top = TinySpace.xs))
                    kind == PetKind.CAT -> Text(stringResource(Res.string.pets_sleepover, name), style = TinyType.Caption, modifier = Modifier.padding(top = TinySpace.xs))
                    else -> Unit
                }
            }
            if (isMet && !lives) {
                Spacer(Modifier.width(TinySpace.sm))
                TinyButton(
                    text = stringResource(if (kind == PetKind.CAT) Res.string.pets_come_home else Res.string.pets_choose),
                    onClick = onChoose,
                    style = TinyButtonStyle.Outline,
                    compact = true,
                    modifier = Modifier.testTag("pet_choose_${kind.name.lowercase()}")
                )
            }
        }
    }
}

private fun petAbout(kind: PetKind): StringResource = when (kind) {
    PetKind.CAT -> Res.string.pet_about_cat
    PetKind.PUPPY -> Res.string.pet_about_puppy
    PetKind.BUNNY -> Res.string.pet_about_bunny
    PetKind.FOX -> Res.string.pet_about_fox
    PetKind.HEDGEHOG -> Res.string.pet_about_hedgehog
    PetKind.DUCK -> Res.string.pet_about_duck
    PetKind.OWL -> Res.string.pet_about_owl
}

private fun petHint(kind: PetKind): StringResource = when (kind) {
    PetKind.CAT -> Res.string.pet_about_cat
    PetKind.PUPPY -> Res.string.pet_hint_puppy
    PetKind.BUNNY -> Res.string.pet_hint_bunny
    PetKind.FOX -> Res.string.pet_hint_fox
    PetKind.HEDGEHOG -> Res.string.pet_hint_hedgehog
    PetKind.DUCK -> Res.string.pet_hint_duck
    PetKind.OWL -> Res.string.pet_hint_owl
}
