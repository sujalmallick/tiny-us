package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import androidx.compose.ui.text.font.FontStyle
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun TbSecretDialog(
    onDismiss: () -> Unit
) = SecretKeepsakeDialog(onDismiss)

@Composable
fun SecretKeepsakeDialog(
    onDismiss: () -> Unit,
    prefs: com.example.data.PreferencesManager? = null
) {
    val profile = com.example.data.ProfileManager.getProfile()
    val title = prefs?.secretCode?.ifBlank { null } ?: profile.secretCodeTitle.ifBlank { profile.boyName }
    val subtitle = profile.secretCodeSubtitle.ifBlank { stringResource(Res.string.ui_keepsake_subtitle_default) }
    val body = prefs?.secretCodeBody?.ifBlank { null } ?: profile.secretCodeBody.ifBlank {
        stringResource(Res.string.ui_keepsake_body_default)
    }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .testTag("tb_secret_dialog"),
        contentPadding = PaddingValues(horizontal = TinySpace.xxl, vertical = 28.dp),
        verticalSpacing = 0.dp,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Letter-style header
        Text(
            text = title,
            style = TinyType.Display.copy(letterSpacing = 2.sp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TinySpace.xs))

        Text(
            text = subtitle,
            style = TinyType.Body.copy(
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = TinyColors.Rose
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TinySpace.lg))

        // Hairline separator
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(1.dp)
                .background(TinyColors.Line)
        )

        Spacer(modifier = Modifier.height(TinySpace.lg))

        Text(
            text = body,
            style = TinyType.Body.copy(fontFamily = FontFamily.Serif, lineHeight = 22.sp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TinySpace.xxl))

        TinyButton(
            text = stringResource(Res.string.ui_close),
            onClick = onDismiss,
            style = TinyButtonStyle.Secondary
        )
    }
}
