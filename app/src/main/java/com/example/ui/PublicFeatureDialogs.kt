package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AdventureStatus
import com.example.data.DailyMomentResponse
import com.example.data.DailyPromptCatalog
import com.example.data.DateAdventure
import com.example.data.DateAdventureCatalog
import com.example.data.LongDistanceSignal
import com.example.data.LongDistanceSignalType
import com.example.data.MiniGameCatalog
import com.example.data.MiniGameRound
import com.example.data.MiniGameType
import com.example.data.PreferencesManager
import com.example.data.RelationshipTimeManager
import com.example.data.SharedMoodType
import com.example.data.WorldEvent
import com.example.data.WorldEventBus
import com.example.ui.theme.BlushPink
import com.example.ui.theme.CozyCream
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepRose
import com.example.ui.theme.PeachMuted
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftRose
import com.example.ui.theme.WarmSand
import com.example.widget.TinyUsWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// 1. TINY DATE ADVENTURES DIALOG
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DateAdventuresDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var savedAdventures by remember { mutableStateOf(prefs.getDateAdventures()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(savedAdventures) {
        savedAdventures.map { it.category }.distinct()
    }

    val filteredList = remember(savedAdventures, selectedCategory) {
        if (selectedCategory == null) savedAdventures
        else savedAdventures.filter { it.category == selectedCategory }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("date_adventures_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🧺", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tiny Date Adventures",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                        }
                        Text(
                            text = "Real-world moments to experience together",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_date_adventures")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        CategoryChip(
                            label = "All",
                            isSelected = selectedCategory == null,
                            onClick = { selectedCategory = null }
                        )
                    }
                    items(categories) { cat ->
                        CategoryChip(
                            label = cat.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Adventures List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { adventure ->
                        DateAdventureCard(
                            adventure = adventure,
                            boyfriendName = prefs.boyfriendName,
                            girlfriendName = prefs.girlfriendName,
                            onUpdate = { updated ->
                                val list = savedAdventures.toMutableList()
                                val idx = list.indexOfFirst { it.id == updated.id }
                                if (idx != -1) {
                                    list[idx] = updated
                                } else {
                                    list.add(updated)
                                }
                                prefs.saveDateAdventures(list)
                                savedAdventures = prefs.getDateAdventures()

                                if (updated.isFullyCompleted) {
                                    WorldEventBus.post(
                                        WorldEvent.DateAdventureCompleted(
                                            id = UUID.randomUUID().toString(),
                                            timestamp = System.currentTimeMillis(),
                                            adventureId = updated.id,
                                            title = updated.title,
                                            completedBy = if (updated.completedByBoy && updated.completedByGirl) "both" else if (updated.completedByBoy) "boy" else "girl"
                                        )
                                    )
                                }
                                TinyUsWidgetProvider.updateAllWidgets(context)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) DeepRose else WarmSand,
        border = BorderStroke(1.dp, if (isSelected) DeepRose else Color(0xFFE2D6CA))
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else DarkSlate,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DateAdventureCard(
    adventure: DateAdventure,
    boyfriendName: String,
    girlfriendName: String,
    onUpdate: (DateAdventure) -> Unit
) {
    val isCompleted = adventure.isFullyCompleted
    val isAccepted = adventure.status == AdventureStatus.ACCEPTED || adventure.status == AdventureStatus.IN_PROGRESS

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF3FAF6) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) SageGreen.copy(alpha = 0.6f) else Color(0xFFE8E0D8)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = adventure.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isCompleted -> SageGreen.copy(alpha = 0.2f)
                        isAccepted -> BlushPink.copy(alpha = 0.35f)
                        adventure.status == AdventureStatus.SKIPPED -> Color.LightGray.copy(alpha = 0.3f)
                        else -> WarmSand
                    }
                ) {
                    Text(
                        text = when {
                            isCompleted -> "Completed 🌸"
                            isAccepted -> "Active 💫"
                            adventure.status == AdventureStatus.SKIPPED -> "Skipped"
                            else -> "Available"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isCompleted -> SageGreen
                            isAccepted -> DeepRose
                            else -> DarkSlate.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = adventure.description,
                fontSize = 13.sp,
                color = DarkSlate.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            val artifact = adventure.unlockedArtifact
            if (!artifact.isNullOrEmpty()) {
                Text(
                    text = "🎁 Unlocks: ${artifact.replace("_", " ")} in Tiny Home",
                    fontSize = 11.sp,
                    color = DeepRose.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Action / Lifecycle buttons
            if (isCompleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = SageGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cherished memory unlocked in our Tiny Us world",
                        fontSize = 12.sp,
                        color = SageGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (isAccepted) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Asynchronous progress:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkSlate.copy(alpha = 0.7f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Partner A completion (Boy)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (adventure.completedByBoy) SageGreen.copy(alpha = 0.15f) else WarmSand,
                                border = BorderStroke(1.dp, if (adventure.completedByBoy) SageGreen else Color.LightGray),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val nextBoy = !adventure.completedByBoy
                                        val completeBoth = nextBoy && adventure.completedByGirl
                                        onUpdate(
                                            adventure.copy(
                                                completedByBoy = nextBoy,
                                                status = if (completeBoth) AdventureStatus.COMPLETED else AdventureStatus.IN_PROGRESS,
                                                completedTimestamp = if (completeBoth) System.currentTimeMillis() else null
                                            )
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (adventure.completedByBoy) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SageGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text(
                                        text = boyfriendName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (adventure.completedByBoy) SageGreen else DarkSlate
                                    )
                                }
                            }

                            // Partner B completion (Girl)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (adventure.completedByGirl) SageGreen.copy(alpha = 0.15f) else WarmSand,
                                border = BorderStroke(1.dp, if (adventure.completedByGirl) SageGreen else Color.LightGray),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val nextGirl = !adventure.completedByGirl
                                        val completeBoth = adventure.completedByBoy && nextGirl
                                        onUpdate(
                                            adventure.copy(
                                                completedByGirl = nextGirl,
                                                status = if (completeBoth) AdventureStatus.COMPLETED else AdventureStatus.IN_PROGRESS,
                                                completedTimestamp = if (completeBoth) System.currentTimeMillis() else null
                                            )
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (adventure.completedByGirl) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SageGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text(
                                        text = girlfriendName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (adventure.completedByGirl) SageGreen else DarkSlate
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onUpdate(
                                    adventure.copy(
                                        completedByBoy = true,
                                        completedByGirl = true,
                                        status = AdventureStatus.COMPLETED,
                                        completedTimestamp = System.currentTimeMillis()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Complete Together ✨", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                onUpdate(adventure.copy(status = AdventureStatus.SKIPPED))
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Skip", fontSize = 12.sp, color = DarkSlate.copy(alpha = 0.6f))
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        onUpdate(
                            adventure.copy(status = AdventureStatus.ACCEPTED)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRose),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Accept Adventure 💌", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. DAILY TINY MOMENT (DAILY REFLECTION) DIALOG
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DailyMomentPromptDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val dayIndex = remember { RelationshipTimeManager.calculateTinyUsDay().toInt() }
    val currentPrompt = remember { DailyPromptCatalog.getPromptForDay(dayIndex) }

    var savedResponse by remember { mutableStateOf(prefs.getDailyMomentResponseForDate(todayStr, currentPrompt.id)) }
    var answerA by remember { mutableStateOf(savedResponse.boyAnswer ?: "") }
    var answerB by remember { mutableStateOf(savedResponse.girlAnswer ?: "") }
    var revealPartnerAnswers by remember {
        mutableStateOf(savedResponse.isRevealed || savedResponse.isBothAnswered)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("daily_moment_prompt_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💭", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Daily Tiny Moment",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                        }
                        Text(
                            text = "One gentle reflection for both of you today",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prompt Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFFCCD5))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BlushPink.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = currentPrompt.category.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepRose,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentPrompt.question,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Partner A Response (Boy)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${prefs.boyfriendName}'s reflection:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkSlate
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = answerA,
                        onValueChange = { answerA = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write your thoughts...", fontSize = 13.sp) },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepRose,
                            unfocusedBorderColor = Color(0xFFDDD2C6),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Partner B Response (Girl)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${prefs.girlfriendName}'s reflection:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkSlate
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = answerB,
                        onValueChange = { answerB = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write your thoughts...", fontSize = 13.sp) },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepRose,
                            unfocusedBorderColor = Color(0xFFDDD2C6),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save Controls
                Button(
                    onClick = {
                        val hasBoth = answerA.isNotBlank() && answerB.isNotBlank()
                        val updated = DailyMomentResponse(
                            promptId = currentPrompt.id,
                            dateString = todayStr,
                            boyAnswer = answerA.ifBlank { null },
                            girlAnswer = answerB.ifBlank { null },
                            isRevealed = revealPartnerAnswers || hasBoth,
                            completedTimestamp = if (hasBoth) System.currentTimeMillis() else null
                        )
                        prefs.saveDailyMomentResponse(updated)
                        savedResponse = updated
                        revealPartnerAnswers = updated.isRevealed
                        TinyUsWidgetProvider.updateAllWidgets(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save Our Daily Moment ✨", fontWeight = FontWeight.SemiBold)
                }

                if (savedResponse.isAnsweredByBoy || savedResponse.isAnsweredByGirl) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (revealPartnerAnswers) "Both reflections shared & unlocked on Bedside Notepad 📝" else "Reflections preserved privately until both share 🔒",
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        color = if (revealPartnerAnswers) SageGreen else DarkSlate.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. TWO-PERSON MINI-GAMES DIALOG
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TwoPersonMiniGameDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(MiniGameType.WOULD_YOU_RATHER) }
    var questionIndex by remember { mutableIntStateOf(0) }

    val currentQuestions = remember(selectedType) {
        MiniGameCatalog.questions.filter { it.type == selectedType }
    }
    val currentQuestion = currentQuestions.getOrElse(questionIndex % currentQuestions.size.coerceAtLeast(1)) {
        MiniGameCatalog.questions.first()
    }

    var choiceAIndex by remember { mutableStateOf<Int?>(null) }
    var choiceBIndex by remember { mutableStateOf<Int?>(null) }
    var isRevealed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("mini_games_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎲", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Two-Person Mini-Games",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                        }
                        Text(
                            text = "Playful 1-minute relationship moments",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Game Type Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MiniGameType.values()) { type ->
                        CategoryChip(
                            label = type.title,
                            isSelected = selectedType == type,
                            onClick = {
                                selectedType = type
                                questionIndex = 0
                                choiceAIndex = null
                                choiceBIndex = null
                                isRevealed = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Question Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE8E0D8))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentQuestion.prompt,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Choices Section for Partner A (Boy)
                        Text(
                            text = "${prefs.boyfriendName}'s choice:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkSlate.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentQuestion.options.forEachIndexed { idx, opt ->
                                val selected = choiceAIndex == idx
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { choiceAIndex = idx },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) DeepRose else WarmSand,
                                    border = BorderStroke(1.dp, if (selected) DeepRose else Color(0xFFDDD2C6))
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) Color.White else DarkSlate,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Choices Section for Partner B (Girl)
                        Text(
                            text = "${prefs.girlfriendName}'s choice:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkSlate.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentQuestion.options.forEachIndexed { idx, opt ->
                                val selected = choiceBIndex == idx
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { choiceBIndex = idx },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) SoftRose else WarmSand,
                                    border = BorderStroke(1.dp, if (selected) SoftRose else Color(0xFFDDD2C6))
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) Color.White else DarkSlate,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Reveal results
                        if (isRevealed) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val isMatch = choiceAIndex != null && choiceAIndex == choiceBIndex
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isMatch) SageGreen.copy(alpha = 0.15f) else PeachMuted.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (isMatch) SageGreen else PeachMuted),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isMatch) "Match Made in Heaven! 💕" else "Playful Perspectives! 🌟",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isMatch) SageGreen else DarkSlate
                                    )
                                    val optA = choiceAIndex?.let { currentQuestion.options.getOrNull(it) } ?: "—"
                                    val optB = choiceBIndex?.let { currentQuestion.options.getOrNull(it) } ?: "—"
                                    Text(
                                        text = "${prefs.boyfriendName}: \"$optA\" • ${prefs.girlfriendName}: \"$optB\"",
                                        fontSize = 11.5.sp,
                                        color = DarkSlate.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isRevealed) {
                        Button(
                            onClick = {
                                isRevealed = true
                                val round = MiniGameRound(
                                    id = UUID.randomUUID().toString(),
                                    questionId = currentQuestion.id,
                                    type = selectedType,
                                    prompt = currentQuestion.prompt,
                                    options = currentQuestion.options,
                                    boyChosenIndex = choiceAIndex,
                                    girlChosenIndex = choiceBIndex,
                                    isRevealed = true,
                                    timestamp = System.currentTimeMillis()
                                )
                                prefs.saveMiniGameRound(round)
                                TinyUsWidgetProvider.updateAllWidgets(context)
                            },
                            enabled = choiceAIndex != null || choiceBIndex != null,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reveal Answers 💖", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = {
                                questionIndex = (questionIndex + 1) % currentQuestions.size.coerceAtLeast(1)
                                choiceAIndex = null
                                choiceBIndex = null
                                isRevealed = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Next Question", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. SHARED MOOD DIALOG
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SharedMoodDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentMoodState = remember { prefs.getPartnerMoodState() }

    var selectedMoodA by remember { mutableStateOf(currentMoodState.boyMood) }
    var isSharedA by remember { mutableStateOf(currentMoodState.boyMoodShared) }

    var selectedMoodB by remember { mutableStateOf(currentMoodState.girlMood) }
    var isSharedB by remember { mutableStateOf(currentMoodState.girlMoodShared) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("shared_mood_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌸", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shared Mood",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                        }
                        Text(
                            text = "A gentle whisper of how you feel today",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Partner A Mood Section (Boy)
                PartnerMoodSection(
                    partnerName = prefs.boyfriendName,
                    selectedMood = selectedMoodA,
                    onSelectMood = { selectedMoodA = it },
                    isShared = isSharedA,
                    onToggleShared = { isSharedA = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Partner B Mood Section (Girl)
                PartnerMoodSection(
                    partnerName = prefs.girlfriendName,
                    selectedMood = selectedMoodB,
                    onSelectMood = { selectedMoodB = it },
                    isShared = isSharedB,
                    onToggleShared = { isSharedB = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        prefs.setPartnerMood("boy", selectedMoodA, isSharedA)
                        prefs.setPartnerMood("girl", selectedMoodB, isSharedB)
                        TinyUsWidgetProvider.updateAllWidgets(context)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (com.example.FeatureFlags.PARTNER_SYNC) "Share With Each Other 💌" else "Save Our Moods 🌸", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PartnerMoodSection(
    partnerName: String,
    selectedMood: SharedMoodType,
    onSelectMood: (SharedMoodType) -> Unit,
    isShared: Boolean,
    onToggleShared: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8E0D8))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$partnerName's feeling:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )
                if (com.example.FeatureFlags.PARTNER_SYNC) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isShared) "Shared" else "Private",
                        fontSize = 11.sp,
                        color = if (isShared) SageGreen else DarkSlate.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isShared,
                        onCheckedChange = onToggleShared,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepRose,
                            checkedTrackColor = BlushPink
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5 Mood Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SharedMoodType.values().forEach { mood ->
                    val isSelected = selectedMood == mood
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectMood(mood) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) BlushPink.copy(alpha = 0.4f) else WarmSand,
                        border = BorderStroke(1.dp, if (isSelected) DeepRose else Color(0xFFE2D6CA))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Icon(TinyIcons.mood(mood), contentDescription = null, tint = if (isSelected) DeepRose else DarkSlate, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mood.displayName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) DeepRose else DarkSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. LONG-DISTANCE MODE SIGNALS DIALOG
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LongDistanceSheet(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var savedSignals by remember { mutableStateOf(prefs.getLongDistanceSignals()) }
    var customNote by remember { mutableStateOf("") }
    var selectedSignalType by remember { mutableStateOf(LongDistanceSignalType.SEND_HEART) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("long_distance_sheet"),
            shape = RoundedCornerShape(26.dp),
            color = CozyCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💌", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Long-Distance Signals",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                        }
                        Text(
                            text = "Send tender asynchronous love across the miles",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6 Signal Types
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE8E0D8))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Choose a signal to send:",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkSlate
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val signalsList = LongDistanceSignalType.values()
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            signalsList.take(3).forEach { sig ->
                                SignalTypeChip(
                                    sig = sig,
                                    isSelected = selectedSignalType == sig,
                                    onClick = { selectedSignalType = sig },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            signalsList.drop(3).forEach { sig ->
                                SignalTypeChip(
                                    sig = sig,
                                    isSelected = selectedSignalType == sig,
                                    onClick = { selectedSignalType = sig },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customNote,
                            onValueChange = { customNote = it },
                            placeholder = { Text("Add a warm note (optional)...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepRose,
                                unfocusedBorderColor = Color(0xFFDDD2C6),
                                focusedContainerColor = CozyCream,
                                unfocusedContainerColor = CozyCream
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val newSignal = LongDistanceSignal(
                                    id = UUID.randomUUID().toString(),
                                    sender = "boy",
                                    type = selectedSignalType,
                                    note = customNote.ifBlank { null },
                                    timestamp = System.currentTimeMillis()
                                )
                                prefs.sendLongDistanceSignal(newSignal)
                                savedSignals = prefs.getLongDistanceSignals()
                                customNote = ""
                                TinyUsWidgetProvider.updateAllWidgets(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Signal ✨", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Recent Signals History:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (savedSignals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No signals yet. Send your first signal to unlock the Origami Heart! 🦢",
                            fontSize = 12.sp,
                            color = DarkSlate.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedSignals) { signal ->
                            val senderDisplayName = if (signal.sender == "boy") prefs.boyfriendName else prefs.girlfriendName
                            SignalHistoryCard(signal, senderDisplayName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalTypeChip(
    sig: LongDistanceSignalType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) BlushPink.copy(alpha = 0.4f) else WarmSand,
        border = BorderStroke(1.dp, if (isSelected) DeepRose else Color(0xFFDDD2C6))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(TinyIcons.signal(sig), contentDescription = null, tint = if (isSelected) DeepRose else DarkSlate, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = sig.title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) DeepRose else DarkSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SignalHistoryCard(signal: LongDistanceSignal, senderDisplayName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEDE5DC))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TinyIconBadge(TinyIcons.signal(signal.type), size = 36.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${signal.type.title} from $senderDisplayName",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )
                if (!signal.note.isNullOrBlank()) {
                    Text(
                        text = "\"${signal.note}\"",
                        fontSize = 11.5.sp,
                        fontStyle = FontStyle.Italic,
                        color = DarkSlate.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
