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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.AppLockPolicy
import com.example.security.AppLock
import com.example.security.AppLockStore
import com.example.security.DiscreetMode
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepRose
import kotlinx.coroutines.delay

private val LockCream = Color(0xFFFFF6EE)
private val LockInk = Color(0xFF553D36)
private val LockMuted = Color(0xFF816E62)
private val KeyFill = Color(0xFFFFFCF8)
private val KeyBorder = Color(0xFFE8D8C9)

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

    Box(Modifier.fillMaxSize().background(LockCream).testTag("app_lock_screen"), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 360.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(64.dp).background(DeepRose.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = DeepRose, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.lock_title), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = LockInk)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.lock_subtitle), fontSize = 13.sp, color = LockMuted)
            Spacer(Modifier.height(20.dp))
            PinDots(pin.length)
            Spacer(Modifier.height(10.dp))
            val status = when {
                cooldownLeft > 0 -> stringResource(R.string.lock_cooldown, cooldownLeft)
                else -> message
            }
            Text(status ?: " ", fontSize = 12.sp, color = DeepRose, textAlign = TextAlign.Center, modifier = Modifier.height(18.dp))
            Spacer(Modifier.height(12.dp))
            PinPad(
                enabled = cooldownLeft == 0,
                onDigit = { if (pin.length < AppLockPolicy.MAX_PIN_LENGTH) { pin += it; message = null } },
                onBackspace = { pin = pin.dropLast(1) },
                leftKey = if (biometricsReady) ({ promptBiometric() }) else null
            )
            Spacer(Modifier.height(16.dp))
            PrimaryPill(
                text = stringResource(R.string.lock_unlock),
                enabled = cooldownLeft == 0 && pin.length >= AppLockPolicy.MIN_PIN_LENGTH,
                tag = "lock_unlock_button",
                onClick = ::submit
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.lock_forgot),
                fontSize = 13.sp,
                color = LockMuted,
                modifier = Modifier.clickable { showForgotHelp = true }.padding(8.dp)
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
        Text(stringResource(R.string.lock_forgot), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LockInk)
        Text(
            stringResource(if (canReset) R.string.lock_forgot_body else R.string.lock_forgot_no_screen_lock),
            fontSize = 13.sp, color = LockMuted
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
        Text(stringResource(R.string.action_close), color = LockMuted, modifier = Modifier.align(Alignment.End).clickable(onClick = onDismiss).padding(8.dp))
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
        Text(
            stringResource(if (first == null) R.string.lock_setup_choose else R.string.lock_setup_confirm),
            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LockInk
        )
        Text(stringResource(R.string.lock_setup_hint), fontSize = 12.sp, color = LockMuted)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PinDots(pin.length) }
        Text(error ?: " ", fontSize = 12.sp, color = DeepRose, modifier = Modifier.height(18.dp))
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Text(stringResource(R.string.lock_setting_grace), fontSize = 12.sp, color = DarkSlate.copy(alpha = 0.7f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLockPolicy.GRACE_PERIOD_OPTIONS_SECONDS.forEach { seconds ->
                    val label = when (seconds) {
                        0 -> stringResource(R.string.lock_grace_immediately)
                        else -> stringResource(R.string.lock_grace_minutes, seconds / 60)
                    }
                    val selected = grace == seconds
                    Box(
                        Modifier
                            .border(if (selected) 2.dp else 1.dp, if (selected) DeepRose else KeyBorder, RoundedCornerShape(12.dp))
                            .background(if (selected) DeepRose.copy(alpha = 0.08f) else KeyFill, RoundedCornerShape(12.dp))
                            .clickable { store.graceSeconds = seconds; grace = seconds }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text(label, fontSize = 12.sp, color = if (selected) DeepRose else DarkSlate) }
                }
            }
            SettingSwitchRow(stringResource(R.string.lock_setting_hide_preview), stringResource(R.string.lock_setting_hide_preview_sub), hidePreview, "settings_hide_preview_switch") {
                store.hidePreview = it; hidePreview = it; refreshWindow()
            }
            Text(
                stringResource(R.string.lock_setting_change_pin),
                color = DeepRose, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { showSetup = true }.padding(vertical = 4.dp)
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
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkSlate)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = DarkSlate.copy(alpha = 0.6f))
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DeepRose)
        )
    }
}

@Composable
private fun PinDots(filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.semantics { contentDescription = "$filled digits entered" }) {
        repeat(AppLockPolicy.MAX_PIN_LENGTH) { i ->
            val on = i < filled
            Box(
                Modifier.size(14.dp)
                    .background(if (on) DeepRose else Color.Transparent, CircleShape)
                    .border(2.dp, if (on) DeepRose else KeyBorder, CircleShape)
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
                row.forEach { d -> PadKey(enabled, "pin_key_$d", onClick = { onDigit(d) }) { Text(d, fontSize = 22.sp, color = LockInk) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (leftKey != null) {
                PadKey(true, "pin_key_biometric", onClick = leftKey) {
                    Icon(Icons.Default.Fingerprint, contentDescription = stringResource(R.string.lock_biometric_title), tint = DeepRose)
                }
            } else {
                Spacer(Modifier.size(64.dp))
            }
            PadKey(enabled, "pin_key_0", onClick = { onDigit("0") }) { Text("0", fontSize = 22.sp, color = LockInk) }
            PadKey(enabled, "pin_key_back", onClick = onBackspace) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.lock_backspace), tint = LockMuted)
            }
        }
    }
}

@Composable
private fun PadKey(enabled: Boolean, tag: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(64.dp)
            .background(if (enabled) KeyFill else KeyFill.copy(alpha = 0.5f), CircleShape)
            .border(1.dp, KeyBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun PrimaryPill(text: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .background(if (enabled) DeepRose else DeepRose.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

@Composable
internal fun LockDialogCard(onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LockCream)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
