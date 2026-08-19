package com.su.clubfair.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.LocalAccent

/**
 * The student's face, or their initials when there isn't one.
 *
 * Both states in one composable rather than an `if` at each call site, because
 * the interesting case is the gap between them: the photo is a network read that
 * can be slow, can fail on a URL Google has since rotated, and is simply absent
 * for a student who never set one. All three resolve the same way here — the
 * initials are drawn first and the photo is laid over them, so a circle that has
 * not loaded shows letters rather than a hole, and the layout never shifts when
 * it does.
 *
 * That ordering is the whole implementation. [AsyncImage] paints nothing while
 * loading and nothing on failure, since neither a placeholder nor an error
 * painter is given, and "nothing" over the initials is exactly the fallback we
 * want without a state machine to express it.
 *
 * Initials stay the honest default. The app has no photo of its own: the only one
 * that ever exists came in on a Google credential, and a student who signed up
 * with a phone number has none to show.
 *
 * Silent to a screen reader on purpose — a decorative portrait beside the name it
 * duplicates adds nothing, and hearing it announced before the name is noise.
 */
@Composable
fun StudentAvatar(
    student: Student,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val accent = LocalAccent.current

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f))
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // Both names now that sign-up asks for both: one letter collides
            // across a fair of thousands, two mostly don't.
            text = student.initials,
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            // Scaled to the circle rather than fixed, so the same composable
            // works at 64.dp on Profile and at whatever a later caller wants.
            fontSize = (size.value * 0.4f).sp,
            color = accent,
        )

        student.avatarUrl?.takeIf { it.isNotBlank() }?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}
