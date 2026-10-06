package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.engine.GameText
import com.example.games.CozySprites
import com.example.games.GardenPlots
import com.example.games.PlotOwner
import com.example.games.PlotStage
import com.example.games.Recipes
import com.example.games.Seeds
import com.example.resources.*
import com.example.scene.CozyGames
import com.example.ui.theme.PixelCornerShape
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import org.jetbrains.compose.resources.stringResource

/*
 * The small cards for garden to kitchen (plan 09, E): what to cook (with what the pantry allows),
 * what to plant, and who is watering the shared plot. Shown over the bottom of the scene.
 */

/** A pixel sprite, centred and drawn in whole pixels in a [size] box. */
@Composable
private fun SpriteIcon(sprite: CozySprites.Sprite, size: Dp, petals: List<Color> = CozySprites.BOUQUET_COLORS, center: Color = Color(0xFFFFD166)) {
    Canvas(Modifier.size(size)) {
        val p = kotlin.math.floor(this.size.minDimension / (maxOf(sprite.width, sprite.height) + 1))
        CozySprites.draw(this, sprite, (this.size.width - sprite.width * p) / 2f, (this.size.height - sprite.height * p) / 2f, p, petals, center)
    }
}

/** One choice: a sprite over a short name, dimmed when it can't be chosen yet. */
@Composable
private fun ChoiceTile(
    name: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = PixelCornerShape(10.dp),
        color = TinyColors.Muted,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .semantics {
                role = Role.Button
                contentDescription = name
            }
            .testTag(testTag)
    ) {
        Column(Modifier.padding(vertical = 6.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            // Capitalised for the tile, and free to wrap after a hyphen ("forget-me-not").
            val label = name.replaceFirstChar { it.uppercaseChar() }.replace("-", "-\u200B")
            Text(label, style = TinyType.Caption, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

/** "What shall we cook?": the starters are always open; garden recipes need the pantry. */
@Composable
fun RecipePickerCard(cozy: CozyGames, onLocked: (String) -> Unit, modifier: Modifier = Modifier) {
    val pantry = cozy.pantry
    TinyCard(modifier = modifier.fillMaxWidth().testTag("recipe_picker"), padding = TinySpace.md) {
        Text(stringResource(Res.string.recipe_picker_title), style = TinyType.BodyStrong)
        Recipes.ALL.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.xs)) {
                row.forEach { recipe ->
                    val name = stringResource(CozyGames.recipeName(recipe.id))
                    val open = recipe.canCook(pantry)
                    ChoiceTile(
                        name = name,
                        enabled = open,
                        onClick = {
                            if (open) cozy.chooseRecipe(recipe)
                            else onLocked(GameText.get(Res.string.recipe_needs, recipe.pantry.joinToString(", ") { GameText.get(CozyGames.ingredientName(it)) }))
                        },
                        testTag = "recipe_${recipe.id}",
                        modifier = Modifier.weight(1f)
                    ) { SpriteIcon(CozySprites.DISHES.getValue(recipe.id), 34.dp) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text(
            if (pantry.isEmpty()) stringResource(Res.string.pantry_empty)
            else stringResource(Res.string.pantry_line, pantry.entries.joinToString(", ") { (i, n) -> "${GameText.get(CozyGames.ingredientName(i))} x$n" }),
            style = TinyType.Caption.copy(color = TinyColors.InkMuted)
        )
        TinyButton(
            text = stringResource(Res.string.garden_not_now),
            onClick = { cozy.closeRecipePicker() },
            icon = PixelIcons.Close,
            compact = true,
            testTag = "recipe_picker_close"
        )
    }
}

/** "What shall we plant?": the seeds for this season (or for the sunroom pots). */
@Composable
fun SeedPickerCard(cozy: CozyGames, spot: Int, modifier: Modifier = Modifier) {
    val pot = GardenPlots.isPot(spot)
    val seeds = if (pot) Seeds.forPots() else Seeds.forMeadow(cozy.season)
    TinyCard(modifier = modifier.fillMaxWidth().testTag("seed_picker"), padding = TinySpace.md) {
        Text(stringResource(if (pot) Res.string.seed_picker_pot_title else Res.string.seed_picker_title), style = TinyType.BodyStrong)
        seeds.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.xs)) {
                row.forEach { seed ->
                    val (petal, center) = CozySprites.flowerColors(seed.id)
                    ChoiceTile(
                        name = stringResource(CozyGames.seedName(seed.id)),
                        enabled = true,
                        onClick = { cozy.chooseSeed(seed) },
                        testTag = "seed_${seed.id}",
                        modifier = Modifier.weight(1f)
                    ) { SpriteIcon(CozySprites.spot(PlotStage.BLOOM, seed.id, pot), 34.dp, listOf(petal), center) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        TinyButton(
            text = stringResource(Res.string.garden_not_now),
            onClick = { cozy.closeGardenCards() },
            icon = PixelIcons.Close,
            compact = true,
            testTag = "seed_picker_close"
        )
    }
}

/** "Who is watering?" for the shared plot, which wants both of them each day. */
@Composable
fun WhoWatersCard(cozy: CozyGames, boyName: String, girlName: String, modifier: Modifier = Modifier) {
    TinyCard(modifier = modifier.fillMaxWidth().testTag("who_waters"), padding = TinySpace.md) {
        Text(stringResource(Res.string.who_waters_title), style = TinyType.BodyStrong)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
            TinyButton(
                text = boyName,
                onClick = { cozy.waterAs(PlotOwner.BOY) },
                compact = true,
                testTag = "who_waters_boy",
                modifier = Modifier.weight(1f)
            )
            TinyButton(
                text = girlName,
                onClick = { cozy.waterAs(PlotOwner.GIRL) },
                compact = true,
                testTag = "who_waters_girl",
                modifier = Modifier.weight(1f)
            )
        }
        TinyButton(
            text = stringResource(Res.string.garden_not_now),
            onClick = { cozy.closeGardenCards() },
            icon = PixelIcons.Close,
            style = TinyButtonStyle.Outline,
            compact = true,
            testTag = "who_waters_close"
        )
    }
}
