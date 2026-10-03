package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.RoomTheme

@Composable
fun RoomCustomizerDialog(
    selectedTheme: RoomTheme,
    isLoft: Boolean,
    onSelectTheme: (RoomTheme) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9F1)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Room Customizer", color = Color(0xFF553D36), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isLoft) "Choose the little details in your loft." else "Choose the little details in your living room.",
                    color = Color(0xFF755F56), fontSize = 14.sp
                )
                RoomThemePreview(selectedTheme)
                RoomTheme.values().forEach { theme ->
                    val selected = theme == selectedTheme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFFD76A7C) else Color(0xFFE8D8C9), RoundedCornerShape(16.dp))
                            .background(if (selected) Color(0xFFFFEFF1) else Color(0xFFFFFCF8), RoundedCornerShape(16.dp))
                            .clickable { onSelectTheme(theme) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(34.dp).background(theme.rug, CircleShape).border(2.dp, theme.rugTrim, CircleShape))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(theme.title, color = Color(0xFF493832), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(theme.description, color = Color(0xFF816E62), fontSize = 12.sp)
                        }
                        if (selected) Text("✓", color = Color(0xFFB74C65), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Rugs · bedding · fairy lights · coffee mugs", color = Color(0xFF90776A), fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                Text("Done", modifier = Modifier.align(Alignment.End).clickable(onClick = onDismiss).padding(8.dp), color = Color(0xFFB74C65), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RoomThemePreview(theme: RoomTheme) {
    Box(
        Modifier.fillMaxWidth().height(92.dp).background(theme.wall, RoundedCornerShape(14.dp)).border(1.dp, theme.panel, RoundedCornerShape(14.dp))
    ) {
        Row(Modifier.align(Alignment.TopCenter).padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) { Box(Modifier.size(7.dp).background(theme.light, CircleShape)) }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(22.dp).background(theme.panel))
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).fillMaxWidth(0.56f).height(15.dp).background(theme.rug, RoundedCornerShape(3.dp)).border(1.dp, theme.rugTrim, RoundedCornerShape(3.dp)))
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).size(width = 62.dp, height = 20.dp).background(theme.bedding, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
        Box(Modifier.align(Alignment.BottomCenter).padding(start = 36.dp, bottom = 15.dp).size(width = 24.dp, height = 7.dp).background(theme.beddingAccent, RoundedCornerShape(4.dp)))
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 17.dp).size(width = 9.dp, height = 10.dp).background(theme.mug, RoundedCornerShape(2.dp)))
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 24.dp).size(width = 6.dp, height = 2.dp).background(theme.mugAccent))
    }
}
