package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PrimaryRose = Color(0xFFFF5D8F)
private val DeepRose = Color(0xFFE63946)
private val SoftCream = Color(0xFFFFF9F5)
private val TextDark = Color(0xFF2B2D42)
private val TextMuted = Color(0xFF8D99AE)

private const val MAX_NAME_LENGTH = 10

/**
 * First-launch welcome & customization dialog.
 * Allows couple users to set their names, anniversary date, and optional secret code note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingDialog(
    initialBoyName: String = "Him",
    initialGirlName: String = "Her",
    initialAnniversaryDate: LocalDate = LocalDate.now(),
    initialSecretCode: String = "",
    initialSecretNote: String = "",
    onDismiss: () -> Unit,
    onComplete: (
        boyName: String,
        girlName: String,
        anniversaryDate: LocalDate,
        secretCode: String,
        secretNote: String
    ) -> Unit
) {
    var boyName by remember { mutableStateOf(if (initialBoyName == "Him") "" else initialBoyName) }
    var girlName by remember { mutableStateOf(if (initialGirlName == "Her") "" else initialGirlName) }
    var anniversaryDate by remember { mutableStateOf(initialAnniversaryDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    var showSecretFields by remember { mutableStateOf(initialSecretCode.isNotEmpty() || initialSecretNote.isNotEmpty()) }
    var secretCode by remember { mutableStateOf(initialSecretCode) }
    var secretNote by remember { mutableStateOf(initialSecretNote) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, PrimaryRose.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .testTag("onboarding_dialog_surface"),
                color = SoftCream,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(PrimaryRose.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )
                            .border(1.5.dp, PrimaryRose, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart",
                            tint = DeepRose,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Welcome to Tiny Us",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "A cozy, quiet world made just for the two of you.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Names Input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = boyName,
                            onValueChange = { if (it.length <= MAX_NAME_LENGTH) boyName = it },
                            label = { Text("Boy's Name (e.g. Alex)") },
                            placeholder = { Text("Him") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryRose)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_boy_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryRose,
                                unfocusedBorderColor = Color(0xFFD8D8D8),
                                focusedLabelColor = PrimaryRose
                            )
                        )

                        OutlinedTextField(
                            value = girlName,
                            onValueChange = { if (it.length <= MAX_NAME_LENGTH) girlName = it },
                            label = { Text("Girl's Name (e.g. Sam)") },
                            placeholder = { Text("Her") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryRose)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_girl_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryRose,
                                unfocusedBorderColor = Color(0xFFD8D8D8),
                                focusedLabelColor = PrimaryRose
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Anniversary Date Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Special Anniversary Date",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFD8D8D8), RoundedCornerShape(12.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("onboarding_date_picker_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PrimaryRose
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = anniversaryDate.format(dateFormatter),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secret Note Toggle (Unified Keepsake system)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSecretFields = !showSecretFields }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (showSecretFields) PrimaryRose else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showSecretFields) "Hide Secret Keepsake Note" else "+ Add Secret Keepsake Note (Optional)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showSecretFields) DeepRose else PrimaryRose
                        )
                    }

                    AnimatedVisibility(
                        visible = showSecretFields,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = secretCode,
                                onValueChange = { secretCode = it.take(8).uppercase() },
                                label = { Text("Passcode (e.g. LOVE)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_secret_code"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryRose,
                                    unfocusedBorderColor = Color(0xFFD8D8D8)
                                )
                            )

                            OutlinedTextField(
                                value = secretNote,
                                onValueChange = { secretNote = it.take(180) },
                                label = { Text("Private message for your love") },
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_secret_note"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryRose,
                                    unfocusedBorderColor = Color(0xFFD8D8D8)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Complete Button
                    Button(
                        onClick = {
                            val finalBoy = boyName.trim().ifBlank { "Him" }.take(MAX_NAME_LENGTH)
                            val finalGirl = girlName.trim().ifBlank { "Her" }.take(MAX_NAME_LENGTH)
                            onComplete(
                                finalBoy,
                                finalGirl,
                                anniversaryDate,
                                secretCode.trim(),
                                secretNote.trim()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("onboarding_complete_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepRose),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Begin Our Journey",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val nowMillis = remember {
            LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        val initialSelectedMillis = remember(anniversaryDate) {
            anniversaryDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Prevent selecting future dates
                    return utcTimeMillis <= nowMillis + 86400000L
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            anniversaryDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm", color = DeepRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
