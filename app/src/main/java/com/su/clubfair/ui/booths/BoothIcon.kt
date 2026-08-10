package com.su.clubfair.ui.booths

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.su.clubfair.R
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.accent

/**
 * A club's icon, or its booth code when there isn't one.
 *
 * Six of the 28 booths have no icon: nothing in the existing set is close enough
 * to Buddhism, Christian, DAC, board games, petanque or human rights, and a wrong
 * glyph on a club's card is worse than none — see migration 000017.
 *
 * The fallback is a generic "a club" glyph — a group of people.
 *
 * It was the booth's own code, on the reasoning that the printed number is the most
 * useful thing a tile can show when it cannot show a picture. On the zone wall that
 * was visibly wrong: the status disc in each tile's corner *already* shows the code,
 * so B1 and B9 rendered it twice, which reads as a rendering fault rather than as a
 * fallback. The code is not lost — it is still on the disc, and still in the search
 * row's second line.
 *
 * One composable rather than an `if (icon != null)` at each of the four call sites,
 * so a booth without art can never render as an empty square in one of them.
 *
 * [tint] overrides the zone colour for the tiles that draw on a coloured card and
 * need their own ink — see `cardInk` in `ZoneBoothWall`.
 */
@Composable
fun BoothIcon(
    booth: Booth,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Icon(
        painter = painterResource(booth.icon ?: R.drawable.ic_users_round),
        contentDescription = null,
        tint = tint ?: booth.accent,
        modifier = modifier.size(size),
    )
}
