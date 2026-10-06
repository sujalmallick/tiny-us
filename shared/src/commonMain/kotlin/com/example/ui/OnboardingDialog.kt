package com.example.ui

import com.example.data.CoupleDates
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import com.example.data.PersonalProfile
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelIcons
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource

private const val MAX_NAME_LENGTH = 10

/**
 * First-launch welcome & customization dialog.
 * Allows couple users to set their names, anniversary date, and optional secret code note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingDialog(
    initialBoyName: String = PersonalProfile.DEFAULT_NAME_A,
    initialGirlName: String = PersonalProfile.DEFAULT_NAME_B,
    initialAnniversaryDate: LocalDate = CoupleDates.today(),
    initialSecretCode: String = "",
    initialSecretNote: String = "",
    /** When given, the setup also asks for both birthdays (optional) and saves them here (plan 09, A). */
    birthdayStore: com.example.data.BirthdayStore? = null,
    onDismiss: () -> Unit,
    onComplete: (
        boyName: String,
        girlName: String,
        anniversaryDate: LocalDate,
        secretCode: String,
        secretNote: String
    ) -> Unit
) {
    var boyName by remember { mutableStateOf(if (PersonalProfile.isPlaceholderName(initialBoyName)) "" else initialBoyName) }
    var girlName by remember { mutableStateOf(if (PersonalProfile.isPlaceholderName(initialGirlName)) "" else initialGirlName) }
    var anniversaryDate by remember { mutableStateOf(initialAnniversaryDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    var showSecretFields by remember { mutableStateOf(initialSecretCode.isNotEmpty() || initialSecretNote.isNotEmpty()) }
    var secretCode by remember { mutableStateOf(initialSecretCode) }
    var secretNote by remember { mutableStateOf(initialSecretNote) }
    var boyBirthday by remember { mutableStateOf(birthdayStore?.birthdays()?.get(com.example.data.Partner.BOY)?.toString() ?: "") }
    var girlBirthday by remember { mutableStateOf(birthdayStore?.birthdays()?.get(com.example.data.Partner.GIRL)?.toString() ?: "") }


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TinyColors.Scrim.copy(alpha = 0.55f))
                .padding(TinySpace.xl),
            contentAlignment = Alignment.Center
        ) {
            TinyEnterTransition {
                TinySurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_dialog_surface")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(TinySpace.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header icon
                        TinyIconBadge(
                            icon = TinyIcons.Heart,
                            modifier = Modifier.describedAs(Res.string.ui_heart),
                            size = 56.dp,
                            iconSize = 28.dp
                        )

                        Spacer(modifier = Modifier.height(TinySpace.lg))

                        Text(
                            text = stringResource(Res.string.ui_welcome_to_tiny_us),
                            style = TinyType.Display,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() }
                        )

                        Spacer(modifier = Modifier.height(TinySpace.xs))

                        Text(
                            text = stringResource(Res.string.ui_a_cozy_quiet_world_made_just_for_the_two),
                            style = TinyType.Body.copy(color = TinyColors.InkMuted),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(TinySpace.xxl))

                        // Names input
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(TinySpace.md)
                        ) {
                            OutlinedTextField(
                                value = boyName,
                                onValueChange = { if (it.length <= MAX_NAME_LENGTH) boyName = it },
                                label = { Text(stringResource(Res.string.label_your_name)) },
                                placeholder = { Text(PersonalProfile.DEFAULT_NAME_A) },
                                leadingIcon = {
                                    Icon(PixelIcons.Person, contentDescription = null, tint = TinyColors.Rose)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_boy_name"),
                                shape = TinyFieldShape,
                                colors = tinyTextFieldColors()
                            )

                            OutlinedTextField(
                                value = girlName,
                                onValueChange = { if (it.length <= MAX_NAME_LENGTH) girlName = it },
                                label = { Text(stringResource(Res.string.label_partner_name)) },
                                placeholder = { Text(PersonalProfile.DEFAULT_NAME_B) },
                                leadingIcon = {
                                    Icon(PixelIcons.Person, contentDescription = null, tint = TinyColors.Rose)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_girl_name"),
                                shape = TinyFieldShape,
                                colors = tinyTextFieldColors()
                            )
                        }

                        Spacer(modifier = Modifier.height(TinySpace.lg))

                        // Anniversary date selector
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(Res.string.ui_special_anniversary_date),
                                style = TinyType.Label
                            )
                            Spacer(modifier = Modifier.height(TinySpace.sm))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clip(TinyFieldShape)
                                    .background(TinyColors.Card, TinyFieldShape)
                                    .border(1.dp, TinyColors.Line, TinyFieldShape)
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 14.dp, vertical = TinySpace.md)
                                    .testTag("onboarding_date_picker_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = PixelIcons.CalendarMonth,
                                    contentDescription = null,
                                    tint = TinyColors.Rose
                                )
                                Spacer(modifier = Modifier.width(TinySpace.md))
                                Text(
                                    text = DateText.format(anniversaryDate, "MMM dd, yyyy"),
                                    style = TinyType.Body
                                )
                            }
                        }

                        // Birthdays, optional: the app throws a little surprise on the day (plan 09, A)
                        if (birthdayStore != null) {
                            Spacer(modifier = Modifier.height(TinySpace.lg))
                            Column(modifier = Modifier.fillMaxWidth().testTag("onboarding_birthdays")) {
                                Text(stringResource(Res.string.onboarding_birthdays_title), style = TinyType.Label)
                                Text(
                                    stringResource(Res.string.onboarding_birthdays_body),
                                    style = TinyType.Caption,
                                    modifier = Modifier.padding(top = 2.dp, bottom = TinySpace.sm)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.md)) {
                                    BirthdayField(
                                        label = boyName.ifBlank { PersonalProfile.DEFAULT_NAME_A },
                                        value = boyBirthday,
                                        onValueChange = { boyBirthday = it },
                                        modifier = Modifier.weight(1f),
                                        testTag = "onboarding_boy_birthday"
                                    )
                                    BirthdayField(
                                        label = girlName.ifBlank { PersonalProfile.DEFAULT_NAME_B },
                                        value = girlBirthday,
                                        onValueChange = { girlBirthday = it },
                                        modifier = Modifier.weight(1f),
                                        testTag = "onboarding_girl_birthday"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(TinySpace.md))

                        // Secret note toggle (unified keepsake system)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(TinyRadius.Small)
                                .clickable { showSecretFields = !showSecretFields }
                                .padding(vertical = TinySpace.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PixelIcons.Lock,
                                contentDescription = null,
                                tint = if (showSecretFields) TinyColors.Rose else TinyColors.InkMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(TinySpace.sm))
                            Text(
                                text = if (showSecretFields) stringResource(Res.string.ui_hide_secret_note) else stringResource(Res.string.ui_add_secret_note),
                                style = TinyType.Label.copy(color = TinyColors.Rose)
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
                                    .padding(top = TinySpace.sm),
                                verticalArrangement = Arrangement.spacedBy(TinySpace.md)
                            ) {
                                OutlinedTextField(
                                    value = secretCode,
                                    onValueChange = { secretCode = it.take(8).uppercase() },
                                    label = { Text(stringResource(Res.string.ui_passcode_e_g_love)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_secret_code"),
                                    shape = TinyFieldShape,
                                    colors = tinyTextFieldColors()
                                )

                                OutlinedTextField(
                                    value = secretNote,
                                    onValueChange = { secretNote = it.take(180) },
                                    label = { Text(stringResource(Res.string.ui_private_message_for_your_love)) },
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_secret_note"),
                                    shape = TinyFieldShape,
                                    colors = tinyTextFieldColors()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(TinySpace.xxl))

                        // Complete button
                        TinyButton(
                            text = stringResource(Res.string.ui_begin_our_journey),
                            onClick = {
                                val finalBoy = boyName.trim().ifBlank { PersonalProfile.DEFAULT_NAME_A }.take(MAX_NAME_LENGTH)
                                val finalGirl = girlName.trim().ifBlank { PersonalProfile.DEFAULT_NAME_B }.take(MAX_NAME_LENGTH)
                                birthdayStore?.let { store ->
                                    if (boyBirthday.isNotEmpty()) store.setBirthday(com.example.data.Partner.BOY, boyBirthday)
                                    if (girlBirthday.isNotEmpty()) store.setBirthday(com.example.data.Partner.GIRL, girlBirthday)
                                }
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
                                .heightIn(min = 48.dp),
                            style = TinyButtonStyle.Primary,
                            testTag = "onboarding_complete_button"
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val nowMillis = remember {
            CoupleDates.today().atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }
        val initialSelectedMillis = remember(anniversaryDate) {
            anniversaryDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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
                            anniversaryDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(Res.string.ui_confirm), style = TinyType.Label.copy(color = TinyColors.Rose))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.ui_cancel), style = TinyType.Label.copy(color = TinyColors.InkMuted))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Asked once of couples whose saved names are still an older build's "Him"/"Her" defaults: set
 * names now (opens the setup with empty name fields) or keep them. Nothing is renamed silently.
 */
@Composable
fun NamePromptDialog(onSetNames: () -> Unit, onKeep: () -> Unit) {
    TinyDialog(onDismissRequest = onKeep) {
        TinyDialogHeader(
            title = stringResource(Res.string.name_prompt_title),
            icon = com.example.ui.theme.PixelIcons.Favorite
        )
        Text(stringResource(Res.string.name_prompt_body), style = com.example.ui.theme.TinyType.Body)
        Row(horizontalArrangement = Arrangement.spacedBy(com.example.ui.theme.TinySpace.sm)) {
            // Equal halves, so large fonts wrap the labels instead of squeezing one button.
            TinyButton(text = stringResource(Res.string.name_prompt_keep), onClick = onKeep, modifier = Modifier.weight(1f), style = TinyButtonStyle.Outline)
            TinyButton(text = stringResource(Res.string.name_prompt_set), onClick = onSetNames, modifier = Modifier.weight(1f))
        }
    }
}
