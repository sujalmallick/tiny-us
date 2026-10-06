package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.scene.SceneType
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Sailing
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Weekend
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelIcons
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScenePickerDialog(
    currentScene: SceneType,
    onDismiss: () -> Unit,
    onSelectScene: (SceneType) -> Unit,
    onRandomScene: () -> Unit
) {
    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("scene_picker_dialog")
    ) {
        TinyDialogHeader(
            title = stringResource(Res.string.ui_choose_a_scene),
            icon = PixelIcons.Landscape,
            onClose = onDismiss,
            closeTestTag = "close_scenes"
        )

        TinyButton(
            text = stringResource(Res.string.ui_surprise_me_random_scene),
            onClick = {
                onRandomScene()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            style = TinyButtonStyle.Secondary,
            icon = PixelIcons.Shuffle,
            testTag = "random_scene_button"
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(TinySpace.sm),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            items(SceneType.values()) { sc ->
                val isSelected = sc == currentScene
                TinyCard(
                    onClick = {
                        onSelectScene(sc)
                        onDismiss()
                    },
                    selected = isSelected,
                    padding = TinySpace.md
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TinyIconBadge(
                            icon = getSceneIcon(sc),
                            tint = if (isSelected) Color.White else TinyColors.Rose,
                            background = if (isSelected) TinyColors.Rose else TinyColors.RoseSoft,
                            size = 36.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(TinySpace.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sc.title,
                                style = TinyType.BodyStrong
                            )
                            Text(
                                text = sc.subtitle,
                                style = TinyType.Caption
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun getSceneIcon(sc: SceneType): ImageVector = when (sc) {
    SceneType.FLOWER -> PixelIcons.LocalFlorist
    SceneType.UNDER_TREE -> PixelIcons.Park
    SceneType.COOKING -> PixelIcons.Restaurant
    SceneType.SLEEP -> PixelIcons.Weekend
    SceneType.WALK -> PixelIcons.Nightlight
    SceneType.LOOKING -> PixelIcons.Favorite
    SceneType.MOMO_STALL -> PixelIcons.Restaurant
    SceneType.EVENING_RIDE -> PixelIcons.TwoWheeler
    SceneType.COZY_LOFT -> PixelIcons.Weekend
    SceneType.RAINY_CAFE -> PixelIcons.Restaurant
    SceneType.SUNROOM -> PixelIcons.LocalFlorist
    SceneType.CAMPFIRE -> PixelIcons.AutoAwesome
    SceneType.SEASIDE_PIER -> PixelIcons.Sailing
}
