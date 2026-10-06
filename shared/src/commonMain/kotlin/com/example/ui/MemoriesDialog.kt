package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MemoryItem
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Weekend
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun MemoriesDialog(
    memories: List<MemoryItem>,
    onDismiss: () -> Unit,
    onAddMemory: (title: String, note: String, date: String, iconType: String) -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("heart") }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .padding(vertical = TinySpace.lg)
            .testTag("memories_dialog")
    ) {
        TinyDialogHeader(
            title = stringResource(Res.string.ui_our_keepsakes),
            subtitle = stringResource(Res.string.ui_memories_captured_in_our_tiny_world),
            icon = TinyIcons.Heart,
            onClose = onDismiss,
            closeTestTag = "close_memories"
        )

        if (!showAddSheet) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
            ) {
                items(memories) { mem ->
                    MemoryCard(mem)
                }
            }

            TinyButton(
                text = stringResource(Res.string.ui_add_our_memory),
                onClick = { showAddSheet = true },
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Primary,
                icon = PixelIcons.Add,
                testTag = "add_memory_button"
            )
        } else {
            // Add memory form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TinySpace.md)
            ) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text(stringResource(Res.string.ui_memory_title)) },
                    placeholder = { Text(stringResource(Res.string.ui_e_g_rainy_day_cocoa)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = TinyFieldShape,
                    colors = tinyTextFieldColors()
                )
                OutlinedTextField(
                    value = newDate,
                    onValueChange = { newDate = it },
                    label = { Text(stringResource(Res.string.ui_date_season)) },
                    placeholder = { Text(stringResource(Res.string.ui_e_g_autumn_afternoon)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = TinyFieldShape,
                    colors = tinyTextFieldColors()
                )
                OutlinedTextField(
                    value = newNote,
                    onValueChange = { newNote = it },
                    label = { Text(stringResource(Res.string.ui_sweet_note)) },
                    placeholder = { Text(stringResource(Res.string.ui_what_made_this_moment_special)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = TinyFieldShape,
                    colors = tinyTextFieldColors()
                )

                Text(stringResource(Res.string.ui_select_icon), style = TinyType.Label)
                Row(modifier = Modifier.fillMaxWidth()) {
                    val icons = listOf("heart", "flower", "tree", "cooking", "couch", "stars")
                    icons.forEach { ic ->
                        val isSelected = selectedIcon == ic
                        // Each cell is a full 48dp-tall touch target; the visible circle stays 40dp.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(PixelCircleShape)
                                .clickable { selectedIcon = ic },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (isSelected) TinyColors.Rose else TinyColors.Muted, PixelCircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForType(ic),
                                    contentDescription = ic,
                                    tint = if (isSelected) Color.White else TinyColors.Ink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TinySpace.xs),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TinyButton(
                        text = stringResource(Res.string.ui_cancel),
                        onClick = { showAddSheet = false },
                        style = TinyButtonStyle.Ghost
                    )
                    Spacer(modifier = Modifier.width(TinySpace.sm))
                    TinyButton(
                        text = stringResource(Res.string.ui_save),
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                onAddMemory(newTitle, newNote, newDate, selectedIcon)
                                showAddSheet = false
                                newTitle = ""
                                newNote = ""
                                newDate = ""
                            }
                        },
                        style = TinyButtonStyle.Primary
                    )
                }
            }
        }
    }
}

@Composable
internal fun MemoryCard(mem: MemoryItem) {
    TinyCard(padding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            TinyIconBadge(icon = getIconForType(mem.iconType))
            Spacer(modifier = Modifier.width(TinySpace.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mem.title,
                        style = TinyType.BodyStrong,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(TinySpace.sm))
                    Text(
                        text = mem.date,
                        style = TinyType.Micro,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(TinySpace.xs))
                Text(
                    text = mem.note,
                    style = TinyType.Body.copy(color = TinyColors.InkMuted)
                )
            }
        }
    }
}

internal fun getIconForType(type: String): ImageVector = when (type) {
    "flower" -> PixelIcons.LocalFlorist
    "tree" -> PixelIcons.Park
    "cooking" -> PixelIcons.Restaurant
    "couch" -> PixelIcons.Weekend
    "stars" -> PixelIcons.Nightlight
    else -> PixelIcons.Favorite
}
