package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.engine.GameText
import com.example.games.CookingGame
import com.example.games.CozySprites
import com.example.games.Ingredient
import com.example.resources.*
import com.example.scene.CozyGames
import com.example.ui.theme.PixelCornerShape
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import org.jetbrains.compose.resources.stringResource

/**
 * The recipe card (plan 07, C3): the dish, its ingredients to tap in order (the next one is
 * outlined, the ones already in fade), what's happening in the pot, and a way to put it away.
 */
@Composable
fun CookingCard(cozy: CozyGames, modifier: Modifier = Modifier) {
    // Read so the card redraws on every change in the game.
    cozy.cookingTick
    val game = cozy.cooking
    val recipe = game.recipe ?: return
    TinyCard(modifier = modifier.fillMaxWidth().testTag("cooking_card"), padding = TinySpace.md) {
        Text(stringResource(Res.string.cooking_card_title, stringResource(CozyGames.recipeName(recipe.id))), style = TinyType.BodyStrong)
        when (game.phase) {
            CookingGame.Phase.GATHERING -> {
                val done = recipe.ingredients.take(game.added).toSet()
                // One short row, so the card leaves the kitchen in view.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.xs)) {
                        cozy.cookingChoices.forEach { ingredient ->
                            IngredientButton(
                                ingredient = ingredient,
                                isNext = ingredient == game.next,
                                isDone = ingredient in done,
                                wiggle = if (ingredient == game.wiggling) game.wiggleTimer else 0f,
                                onClick = { cozy.tapIngredient(ingredient) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                }
                Text(stringResource(Res.string.cooking_hint), style = TinyType.Caption.copy(color = TinyColors.InkMuted))
            }
            CookingGame.Phase.COOKING -> Text(stringResource(Res.string.cooking_simmering), style = TinyType.Body)
            else -> Text(
                stringResource(Res.string.cooking_served, stringResource(CozyGames.recipeName(recipe.id))),
                style = TinyType.Body
            )
        }
        TinyButton(
            text = stringResource(Res.string.cooking_stop),
            onClick = { cozy.stopCooking() },
            icon = PixelIcons.Close,
            compact = true,
            testTag = "cooking_stop_button"
        )
    }
}

@Composable
private fun IngredientButton(
    ingredient: Ingredient,
    isNext: Boolean,
    isDone: Boolean,
    wiggle: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = GameText.get(CozyGames.ingredientName(ingredient))
    // A wrong pick shakes side to side while its wiggle runs down.
    val shake = if (wiggle > 0f) (if ((wiggle * 20f).toInt() % 2 == 0) 3 else -3) else 0
    Surface(
        onClick = onClick,
        shape = PixelCornerShape(10.dp),
        color = if (isNext) TinyColors.RoseSoft else TinyColors.Muted,
        border = if (isNext) BorderStroke(2.dp, TinyColors.Rose) else null,
        modifier = modifier
            .offset(x = shake.dp)
            .alpha(if (isDone) 0.4f else 1f)
            .semantics {
                role = Role.Button
                contentDescription = name
                selected = isDone
            }
            .testTag("ingredient_${ingredient.name}")
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(32.dp)) {
                val sprite = CozySprites.INGREDIENTS.getValue(ingredient)
                val p = kotlin.math.floor(size.minDimension / 9f)
                CozySprites.draw(this, sprite, (size.width - sprite.width * p) / 2f, (size.height - sprite.height * p) / 2f, p)
            }
        }
    }
}
