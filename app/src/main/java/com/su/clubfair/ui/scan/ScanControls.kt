package com.su.clubfair.ui.scan

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.data.BoothCode
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.BoothCount
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.Palette

/**
 * The torch toggle.
 *
 * A fair hall is lit for people, not for cameras, and a booth's printed code is
 * usually on a table under someone's shadow. Without this the fallback is
 * "stand somewhere else", which at a busy booth is not available.
 *
 * Only drawn when the camera reports a flash unit — an emulator and a good few
 * front-facing-only devices have none, and a control that cannot do anything is
 * worse than an absent one.
 */
@Composable
fun TorchButton(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (enabled) accent else Color.White.copy(alpha = 0.12f),
            )
            .clickable(role = Role.Switch) { onToggle(!enabled) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_flashlight),
            contentDescription = stringResource(
                if (enabled) R.string.scan_torch_off else R.string.scan_torch_on,
            ),
            tint = if (enabled) Palette.Ink else Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Typing a booth number instead of scanning it.
 *
 * The camera path has four ways to fail that a student cannot fix: permission
 * refused, no camera on the device, a code too damaged or too glared to read,
 * and a booth whose printed sign has gone missing by the afternoon. Every one of
 * those ended the journey, because scanning was the only way to record a
 * checkpoint.
 *
 * It is worth being clear about what this costs. A typed number is *exactly* as
 * trustworthy as a scanned one here, because [BoothCode] accepts a bare number
 * and nothing on the device can certify that anyone stood anywhere. This adds no
 * new hole — it makes the existing one visible, and it will close for both paths
 * at once when a server signs the codes.
 */
@Composable
fun ManualEntry(
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entry by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val accent = LocalAccent.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val number = entry.toIntOrNull()
    val valid = number != null && number in 1..BoothCount
    val showProblem = entry.isNotEmpty() && !valid

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = Dimens.RadiusLg)
            .padding(Dimens.CardPadding),
    ) {
        Text(
            text = stringResource(R.string.scan_manual_title),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
        )

        Spacer(Modifier.height(Dimens.Space))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(Dimens.RadiusMd))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = Dimens.Space),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_hash),
                contentDescription = null,
                tint = Ink.Faint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(Dimens.SpaceSm))
            Box(modifier = Modifier.weight(1f)) {
                if (entry.isEmpty()) {
                    Text(
                        text = stringResource(R.string.scan_manual_hint, BoothCount),
                        fontFamily = AlanSans,
                        fontSize = 15.sp,
                        color = Ink.Placeholder,
                    )
                }
                BasicTextField(
                    value = entry,
                    // Digits only, filtered on the way in rather than validated
                    // on the way out: a number pad still offers a decimal point
                    // and a minus sign on plenty of keyboards.
                    onValueChange = { raw -> entry = raw.filter(Char::isDigit).take(2) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontFamily = AlanSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color.White,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    cursorBrush = SolidColor(accent),
                )
            }
        }

        if (showProblem) {
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = stringResource(R.string.scan_manual_invalid, BoothCount),
                fontFamily = AlanSans,
                fontSize = 12.sp,
                color = Palette.Alert,
            )
        }

        Spacer(Modifier.height(Dimens.Space))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            GhostButton(
                text = stringResource(R.string.scan_manual_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            AccentButton(
                text = stringResource(R.string.scan_manual_submit),
                enabled = valid,
                onClick = { number?.let { onSubmit(BoothCode.format(it)) } },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AccentButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(if (enabled) accent else accent.copy(alpha = 0.3f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = if (enabled) Palette.Ink else Palette.Ink.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.White,
        )
    }
}
