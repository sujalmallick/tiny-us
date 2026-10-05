package com.example.ui

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.AppLockPolicy
import com.example.security.AppLock
import com.example.security.AppLockStore
import com.example.security.DiscreetMode
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import kotlinx.coroutines.delay
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons
import com.example.resources.*

internal tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

/** Full-screen lock shown instead of the app while [AppLock.isLocked]. */
@Composable
fun AppLockScreen(store: AppLockStore) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var cooldownLeft by remember { mutableIntStateOf(0) }
    var showForgotHelp by remember { mutableStateOf(false) }

    val biometricsReady = activity != null && store.biometricsEnabled && AppLock.canUseBiometrics(activity)
    val unlockTitle = stringResource(R.string.lock_biometric_title)
    val unlockSubtitle = stringResource(R.string.lock_biometric_subtitle)
    val usePin = stringResource(R.string.lock_use_pin)
    val wrongPin = stringResource(R.string.lock_wrong_pin)

    fun promptBiometric() {
        if (activity == null) return
        AppLock.promptBiometric(activity, unlockTitle, unlockSubtitle, usePin, allowDeviceCredential = false) {
            store.recordSuccess()
            AppLock.unlock()
        }
    }

    LaunchedEffect(Unit) {
        val until = store.lockedOutUntil
        if (until > System.currentTimeMillis()) cooldownLeft = ((until - System.currentTimeMillis()) / 1000).toInt() + 1
        if (biometricsReady) promptBiometric()
    }
    LaunchedEffect(cooldownLeft) {
        if (cooldownLeft > 0) {
            delay(1000)
            cooldownLeft -= 1
        }
    }

    fun submit() {
        when (val result = store.checkPin(pin)) {
            AppLockStore.PinResult.Correct -> AppLock.unlock()
            is AppLockStore.PinResult.Wrong -> {
                message = wrongPin
                cooldownLeft = result.cooldownSeconds
            }
            is AppLockStore.PinResult.CoolingDown -> cooldownLeft = result.secondsLeft
        }
        pin = ""
    }

    Box(Modifier.fillMaxSize().background(TinyColors.Paper).testTag("app_lock_screen"), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 360.dp).padding(TinySpace.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TinyIconBadge(icon = PixelIcons.Lock, size = 64.dp, iconSize = 28.dp)
            Spacer(Modifier.height(TinySpace.lg))
            Text(stringResource(R.string.lock_title), style = TinyType.Display, textAlign = TextAlign.Center)
            Spacer(Modifier.height(TinySpace.xs))
            Text(stringResource(R.string.lock_subtitle), style = TinyType.Body.copy(color = TinyColors.InkMuted), textAlign = TextAlign.Center)
            Spacer(Modifier.height(TinySpace.xl))
            PinDots(pin.length)
            Spacer(Modifier.height(10.dp))
            val status = when {
                cooldownLeft > 0 -> stringResource(R.string.lock_cooldown, cooldownLeft)
                else -> message
            }
            Text(
                status ?: " ",
                style = TinyType.Caption.copy(color = TinyColors.Rose),
                textAlign = TextAlign.Center,
                modifier = Modifier.heightIn(min = 18.dp)
            )
            Spacer(Modifier.height(TinySpace.md))
            PinPad(
                enabled = cooldownLeft == 0,
                onDigit = { if (pin.length < AppLockPolicy.MAX_PIN_LENGTH) { pin += it; message = null } },
                onBackspace = { pin = pin.dropLast(1) },
                leftKey = if (biometricsReady) ({ promptBiometric() }) else null
            )
            Spacer(Modifier.height(TinySpace.xl))
            PrimaryPill(
                text = stringResource(R.string.lock_unlock),
                enabled = cooldownLeft == 0 && pin.length >= AppLockPolicy.MIN_PIN_LENGTH,
                tag = "lock_unlock_button",
                onClick = ::submit
            )
            Spacer(Modifier.height(TinySpace.sm))
            TinyButton(
                text = stringResource(R.string.lock_forgot),
                onClick = { showForgotHelp = true },
                style = TinyButtonStyle.Ghost
            )
        }
    }

    if (showForgotHelp) {
        ForgotPinDialog(activity = activity, store = store, onDismiss = { showForgotHelp = false })
    }
}

@Composable
private fun ForgotPinDialog(activity: FragmentActivity?, store: AppLockStore, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val canReset = activity != null && AppLock.hasDeviceCredential(activity)
    val title = stringResource(R.string.lock_reset_prompt_title)
    val subtitle = stringResource(R.string.lock_reset_prompt_subtitle)
    val resetDone = stringResource(R.string.lock_reset_done)
    LockDialogCard(onDismiss) {
        TinyDialogHeader(title = stringResource(R.string.lock_forgot), icon = PixelIcons.Lock)
        Text(
            stringResource(if (canReset) R.string.lock_forgot_body else R.string.lock_forgot_no_screen_lock),
            style = TinyType.Body.copy(color = TinyColors.InkMuted)
        )
        if (canReset) {
            PrimaryPill(stringResource(R.string.lock_reset_with_phone), enabled = true, tag = "lock_reset_button") {
                AppLock.promptBiometric(activity!!, title, subtitle, "", allowDeviceCredential = true) {
                    store.disable()
                    AppLock.applyWindowPrivacy(activity, store)
                    AppLock.unlock()
                    Toast.makeText(context, resetDone, Toast.LENGTH_LONG).show()
                }
                onDismiss()
            }
        }
        TinyButton(
            text = org.jetbrains.compose.resources.stringResource(Res.string.action_close),
            onClick = onDismiss,
            style = TinyButtonStyle.Ghost,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

/** Two-step "choose PIN / confirm PIN" dialog. Calls [onPinChosen] with a valid, confirmed PIN. */
@Composable
fun PinSetupDialog(onPinChosen: (String) -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val mismatch = stringResource(R.string.lock_setup_mismatch)

    LockDialogCard(onDismiss) {
        TinyDialogHeader(
            title = stringResource(if (first == null) R.string.lock_setup_choose else R.string.lock_setup_confirm),
            subtitle = stringResource(R.string.lock_setup_hint),
            icon = PixelIcons.Lock
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PinDots(pin.length) }
        Text(
            error ?: " ",
            style = TinyType.Caption.copy(color = TinyColors.Rose),
            modifier = Modifier.heightIn(min = 18.dp)
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PinPad(
                enabled = true,
                onDigit = { if (pin.length < AppLockPolicy.MAX_PIN_LENGTH) { pin += it; error = null } },
                onBackspace = { pin = pin.dropLast(1) },
                leftKey = null
            )
        }
        PrimaryPill(
            text = stringResource(if (first == null) R.string.action_next else R.string.lock_setup_turn_on),
            enabled = AppLockPolicy.isValidPin(pin),
            tag = "lock_setup_next"
        ) {
            val chosen = first
            when {
                chosen == null -> { first = pin; pin = "" }
                chosen == pin -> onPinChosen(pin)
                else -> { error = mismatch; first = null; pin = "" }
            }
        }
    }
}

/** Settings card: PIN lock, biometrics, grace period, preview hiding and discreet mode. */
@Composable
fun PrivacyLockSettings() {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val store = remember(context) { AppLockStore(context) }
    var enabled by remember { mutableStateOf(store.isEnabled) }
    var biometrics by remember { mutableStateOf(store.biometricsEnabled) }
    var grace by remember { mutableIntStateOf(store.graceSeconds) }
    var hidePreview by remember { mutableStateOf(store.hidePreview) }
    var discreet by remember { mutableStateOf(DiscreetMode.isEnabled(context)) }
    var showSetup by remember { mutableStateOf(false) }
    val biometricsAvailable = activity != null && AppLock.canUseBiometrics(activity)

    fun refreshWindow() { activity?.let { AppLock.applyWindowPrivacy(it, store) } }

    Column(verticalArrangement = Arrangement.spacedBy(TinySpace.md)) {
        SettingSwitchRow(
            title = stringResource(R.string.lock_setting_title),
            subtitle = stringResource(R.string.lock_setting_subtitle),
            checked = enabled,
            tag = "settings_app_lock_switch"
        ) { on ->
            if (on) showSetup = true else {
                store.disable(); enabled = false; biometrics = false; refreshWindow()
            }
        }
        if (enabled) {
            if (biometricsAvailable) {
                SettingSwitchRow(stringResource(R.string.lock_setting_biometrics), null, biometrics, "settings_biometrics_switch") {
                    store.biometricsEnabled = it; biometrics = it
                }
            }
            Text(stringResource(R.string.lock_setting_grace), style = TinyType.Caption)
            Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                AppLockPolicy.GRACE_PERIOD_OPTIONS_SECONDS.forEach { seconds ->
                    val label = when (seconds) {
                        0 -> stringResource(R.string.lock_grace_immediately)
                        else -> stringResource(R.string.lock_grace_minutes, seconds / 60)
                    }
                    TinyChip(
                        text = label,
                        selected = grace == seconds,
                        onClick = { store.graceSeconds = seconds; grace = seconds }
                    )
                }
            }
            SettingSwitchRow(stringResource(R.string.lock_setting_hide_preview), stringResource(R.string.lock_setting_hide_preview_sub), hidePreview, "settings_hide_preview_switch") {
                store.hidePreview = it; hidePreview = it; refreshWindow()
            }
            TinyButton(
                text = stringResource(R.string.lock_setting_change_pin),
                onClick = { showSetup = true },
                style = TinyButtonStyle.Ghost,
                compact = true
            )
        }
        SettingSwitchRow(
            title = stringResource(R.string.discreet_setting_title),
            subtitle = stringResource(R.string.discreet_setting_subtitle),
            checked = discreet,
            tag = "settings_discreet_switch"
        ) {
            DiscreetMode.setEnabled(context, it); discreet = it
        }
    }

    if (showSetup) {
        PinSetupDialog(
            onPinChosen = { pin ->
                store.enable(pin); enabled = true; showSetup = false; refreshWindow()
            },
            onDismiss = { showSetup = false }
        )
    }
}

@Composable
private fun SettingSwitchRow(title: String, subtitle: String?, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TinyType.BodyStrong)
            if (subtitle != null) Text(subtitle, style = TinyType.Caption, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(TinySpace.md))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.testTag(tag),
            colors = tinySwitchColors()
        )
    }
}

@Composable
private fun PinDots(filled: Int) {
    val dotsDescription = pluralStringResource(R.plurals.ui_pin_digits_entered, filled, filled)
    Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.md), modifier = Modifier.semantics { contentDescription = dotsDescription }) {
        repeat(AppLockPolicy.MAX_PIN_LENGTH) { i ->
            val on = i < filled
            Box(
                Modifier.size(14.dp)
                    .background(if (on) TinyColors.Rose else Color.Transparent, PixelCircleShape)
                    .border(2.dp, if (on) TinyColors.Rose else TinyColors.Line, PixelCircleShape)
            )
        }
    }
}

@Composable
private fun PinPad(enabled: Boolean, onDigit: (String) -> Unit, onBackspace: () -> Unit, leftKey: (() -> Unit)?) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { d -> PadKey(enabled, "pin_key_$d", onClick = { onDigit(d) }) { PadDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (leftKey != null) {
                PadKey(true, "pin_key_biometric", onClick = leftKey) {
                    Icon(PixelIcons.Fingerprint, contentDescription = stringResource(R.string.lock_biometric_title), tint = TinyColors.Rose)
                }
            } else {
                Spacer(Modifier.size(64.dp))
            }
            PadKey(enabled, "pin_key_0", onClick = { onDigit("0") }) { PadDigit("0") }
            PadKey(enabled, "pin_key_back", onClick = onBackspace) {
                Icon(PixelIcons.Backspace, contentDescription = stringResource(R.string.lock_backspace), tint = TinyColors.InkMuted)
            }
        }
    }
}

@Composable
private fun PadDigit(d: String) {
    Text(d, style = TinyType.Title.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium))
}

@Composable
private fun PadKey(enabled: Boolean, tag: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(64.dp)
            .background(if (enabled) TinyColors.Card else TinyColors.Card.copy(alpha = 0.5f), PixelCircleShape)
            .border(1.dp, TinyColors.Line, PixelCircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun PrimaryPill(text: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    TinyButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        style = TinyButtonStyle.Primary,
        enabled = enabled,
        testTag = tag
    )
}

@Composable
internal fun LockDialogCard(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    TinyDialog(
        onDismissRequest = onDismiss,
        verticalSpacing = TinySpace.md,
        content = content
    )
}
