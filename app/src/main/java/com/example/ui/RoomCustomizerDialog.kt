package com.example.ui

import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.engine.RoomTheme
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelCornerShape
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons

@Composable
fun RoomCustomizerDialog(
    selectedTheme: RoomTheme,
    isLoft: Boolean,
    onSelectTheme: (RoomTheme) -> Unit,
    onDismiss: () -> Unit
) {
    TinyDialog(
        onDismissRequest = onDismiss,
        contentPadding = PaddingValues(0.dp),
        verticalSpacing = 0.dp
    ) {
        TinyDialogHeader(
            title = stringResource(R.string.ui_room_customizer),
            subtitle = if (isLoft) stringResource(R.string.ui_room_details_loft) else stringResource(R.string.ui_room_details_living_room),
            icon = PixelIcons.Weekend,
            modifier = Modifier.padding(start = TinySpace.xl, end = TinySpace.xl, top = TinySpace.xl, bottom = TinySpace.md)
        )

        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = TinySpace.xl, end = TinySpace.xl, bottom = TinySpace.lg),
            verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
        ) {
            RoomThemePreview(selectedTheme)
            RoomTheme.values().forEach { theme ->
                val selected = theme == selectedTheme
                TinyCard(
                    onClick = { onSelectTheme(theme) },
                    selected = selected,
                    padding = TinySpace.md
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Theme swatch is data; it shows the rug colours.
                        Box(Modifier.size(34.dp).background(theme.rug, PixelCircleShape).border(2.dp, theme.rugTrim, PixelCircleShape))
                        Column(Modifier.weight(1f).padding(start = TinySpace.md)) {
                            Text(theme.title, style = TinyType.BodyStrong)
                            Text(theme.description, style = TinyType.Caption)
                        }
                        if (selected) {
                            Icon(
                                PixelIcons.Check,
                                contentDescription = null,
                                tint = TinyColors.Rose,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.ui_room_details_list),
                style = TinyType.Caption,
                modifier = Modifier.padding(top = TinySpace.xs)
            )
        }

        TinyDivider()

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = TinySpace.md, vertical = TinySpace.sm),
            horizontalArrangement = Arrangement.End
        ) {
            TinyButton(text = stringResource(R.string.ui_done), onClick = onDismiss, style = TinyButtonStyle.Ghost)
        }
    }
}

@Composable
private fun RoomThemePreview(theme: RoomTheme) {
    Box(
        Modifier.fillMaxWidth().height(92.dp).background(theme.wall, PixelCornerShape(14.dp)).border(1.dp, theme.panel, PixelCornerShape(14.dp))
    ) {
        Row(Modifier.align(Alignment.TopCenter).padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) { Box(Modifier.size(7.dp).background(theme.light, PixelCircleShape)) }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(22.dp).background(theme.panel))
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).fillMaxWidth(0.56f).height(15.dp).background(theme.rug, PixelCornerShape(3.dp)).border(1.dp, theme.rugTrim, PixelCornerShape(3.dp)))
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).size(width = 62.dp, height = 20.dp).background(theme.bedding, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
        Box(Modifier.align(Alignment.BottomCenter).padding(start = 36.dp, bottom = 15.dp).size(width = 24.dp, height = 7.dp).background(theme.beddingAccent, PixelCornerShape(4.dp)))
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 17.dp).size(width = 9.dp, height = 10.dp).background(theme.mug, PixelCornerShape(2.dp)))
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 24.dp).size(width = 6.dp, height = 2.dp).background(theme.mugAccent))
    }
}
