package com.su.clubfair.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.su.clubfair.R
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.Palette

/**
 * The torch toggle.
 *
 * A fair hall is lit for people, not for cameras, and a booth's rotating code is
 * on a screen or a printed sign that is usually under someone's shadow. Without
 * this the fallback is "stand somewhere else", which at a busy booth is not
 * available — and since booth codes rotate, the camera is now the *only* way to
 * collect a stamp, so anything that helps it read is load-bearing rather than a
 * convenience.
 *
 * Only drawn when the camera reports a flash unit. An emulator and a good few
 * front-facing-only devices have none, and a control that cannot do anything is
 * worse than an absent one.
 *
 * This used to live in `ScanControls.kt` alonga typed booth-number fallback. That
 * fallback is gone — a student cannot type an HMAC — and this is what survived.
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
            .background(if (enabled) accent else Color.White.copy(alpha = 0.12f))
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
