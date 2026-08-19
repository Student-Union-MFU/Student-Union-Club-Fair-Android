package com.su.clubfair.ui.events

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.Hairline
import com.su.clubfair.ui.components.PullUpIndicator
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.components.pullUpToRefresh
import com.su.clubfair.ui.components.rememberPullUpToRefreshState
import com.su.clubfair.ui.model.Announcement
import com.su.clubfair.data.PostOutcome
import com.su.clubfair.data.ReactionOutcome
import com.su.clubfair.ui.model.PreviewAnnouncements
import com.su.clubfair.ui.model.Reaction
import com.su.clubfair.ui.model.ReactionPalette
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme
import kotlinx.coroutines.delay

/**
 * The Events tab: the Student Union's announcements channel, read-only.
 *
 * Modelled on a Discord announcement channel rather than a feed of cards, and
 * the difference is what it says about who is talking. A card grid reads as
 * content the app is showing you; a channel reads as a room with someone in it,
 * where posts arrive in order and the newest is the one at the bottom where you
 * are already looking.
 *
 * A student can't post — [isAdmin] decides whether there is a composer at the
 * bottom at all — but they aren't mute either: [ReactionPalette] gives them five
 * ways to answer, which is what keeps this from being a notice board nobody can
 * respond to.
 */
@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    isStaff: Boolean = false,
    announcements: List<Announcement> = PreviewAnnouncements,
    onReact: (postId: Long, emoji: String) -> Unit = { _, _ -> },
    reactionOutcome: ReactionOutcome? = null,
    onClearReactionOutcome: () -> Unit = {},
    refreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    posting: Boolean = false,
    postOutcome: PostOutcome? = null,
    onPost: (String) -> Unit = {},
    onClearPostOutcome: () -> Unit = {},
) {
    // The feed is the repository's, whole. It used to be `announcements + posted`,
    // where `posted` was a local list the composer appended to — a post that
    // reached no server, no other student, and not even the next tab switch,
    // because the `remember` holding it died with the screen.
    val feed = announcements
    val listState = rememberLazyListState()

    // The draft lives here, not in the composer, because whether it may be
    // cleared is now the server's answer rather than the tap's. Clearing it on
    // the tap is what would lose two thousand characters to a timeout.
    var draft by rememberSaveable { mutableStateOf("") }

    // Only success empties the box. A rejection and a dead network both keep the
    // text exactly where the author left it, so the fix is a second tap rather
    // than retyping the announcement.
    LaunchedEffect(postOutcome) {
        if (postOutcome is PostOutcome.Posted) {
            draft = ""
            onClearPostOutcome()
        }
    }

    // Which post has its picker open, by id. One at a time: two open pickers on
    // screen would both look like the one the next tap belongs to.
    var pickerFor by rememberSaveable { mutableStateOf<Long?>(null) }

    // Land on the newest post, and follow the channel down as posts arrive — the
    // bottom is where a chat is read from.
    LaunchedEffect(feed.size) {
        if (feed.isNotEmpty()) listState.animateScrollToItem(feed.lastIndex)
    }

    // Opening a picker adds a row *below* the post, which on the newest one lands
    // under the footer — the list keeps its scroll position while the content
    // grows past it. Bring the post that owns the picker up to the top of the
    // viewport so its picker always has room underneath.
    LaunchedEffect(pickerFor) {
        val index = feed.indexOfFirst { it.id == pickerFor }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    // Up from the newest post, not down from the oldest. The gesture is on the
    // list rather than the whole screen so it starts where the reader's thumb
    // already is, and so the indicator can sit at the foot of the channel
    // instead of over the composer.
    val pullState = rememberPullUpToRefreshState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
    ) {
        ChannelHeader(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))

        // Under the header rather than beside the chip that failed. The picker
        // has already closed by the time the server answers, so there is no
        // longer anything on screen for a per-post message to point at — and a
        // reaction that did not save is a fact about the channel, not about the
        // one post.
        //
        // Clears itself: the student's next tap is the retry, and a line still
        // sitting there after a reaction has gone through would be reporting a
        // failure that no longer applies.
        reactionOutcome.failureText()?.let { message ->
            LaunchedEffect(reactionOutcome) {
                delay(ReactionNoticeMillis)
                onClearReactionOutcome()
            }
            Text(
                text = message,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                lineHeight = 1.4.em,
                color = Palette.Alert,
                modifier = Modifier.padding(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    top = Dimens.SpaceXs,
                ),
            )
        }

        // The gesture and the indicator live on this box, so the pull region is
        // the channel itself: the header stays put and the indicator rises from
        // the foot of the list rather than out from under the composer.
        // `clipToBounds` is what parks it out of sight at rest.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .pullUpToRefresh(
                    isRefreshing = refreshing,
                    state = pullState,
                    onRefresh = onRefresh,
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    top = Dimens.SpaceLg,
                    // A student has no composer under the list any more, so the last
                    // post has to clear the floating nav bar by itself.
                    bottom = if (isStaff) Dimens.Space else Dimens.NavBarClearance,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
            ) {
                itemsIndexed(feed, key = { _, post -> post.id }) { _, post ->
                    AnnouncementRow(
                        post = post,
                        pickerOpen = pickerFor == post.id,
                        onTogglePicker = { pickerFor = if (pickerFor == post.id) null else post.id },
                        onReact = { emoji ->
                            // Straight to the store. The list this row was built from
                            // is derived from that store, so the chip lights up when
                            // the write lands rather than optimistically before it —
                            // one source of truth, and no reconciliation.
                            onReact(post.id, emoji)
                            pickerFor = null
                        },
                    )
                }
            }

            PullUpIndicator(
                state = pullState,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // Nothing at all where the message box would be for a student. The
        // channel says "React, don't reply" at the top and every post ends in a
        // reaction row, so a locked composer explaining the same rule a third
        // time was just a permanent grey bar across the bottom of the screen.
        if (isStaff) {
            Box(
                modifier = Modifier.padding(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    bottom = Dimens.NavBarClearance,
                ),
            ) {
                Composer(
                    draft = draft,
                    onDraftChange = {
                        draft = it
                        // The failure line belongs to the text that produced it.
                        // Left up while the author edits, it would still be
                        // accusing a message they had already fixed.
                        if (postOutcome != null) onClearPostOutcome()
                    },
                    sending = posting,
                    failure = postOutcome.failureText(),
                    onSend = { onPost(draft.trim()) },
                )
            }
        }
    }
}

/**
 * A post's age, in the reader's own language, kept current.
 *
 * The model used to carry `"Today at 12:30"` as a baked string, which could not
 * be translated, could not follow a 24-hour locale, and stopped being true at
 * midnight while the app sat open on a desk. `DateUtils` does the wording and
 * the pluralisation per locale; the ticker is what stops "Just now" sitting
 * under a post from an hour ago.
 *
 * A minute's poll for a channel of four posts is nothing, and it is the coarsest
 * interval at which the shortest label can go stale.
 */
@Composable
private fun rememberRelativeTime(postedAtMillis: Long): String {
    val context = LocalContext.current
    val justNow = stringResource(R.string.events_just_now)

    val label by produceState(initialValue = justNow, postedAtMillis, justNow) {
        while (true) {
            val elapsed = System.currentTimeMillis() - postedAtMillis
            value = if (elapsed < DateUtils.MINUTE_IN_MILLIS) {
                justNow
            } else {
                DateUtils.getRelativeTimeSpanString(
                    postedAtMillis,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE,
                ).toString()
            }
            delay(60_000)
        }
    }
    return label
}

@Composable
private fun ChannelHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = Dimens.Space)) {
        Text(
            text = stringResource(R.string.events_channel),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 20.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.events_channel_topic),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 12.sp,
            color = Ink.Muted,
        )
        Spacer(Modifier.height(Dimens.Space))
        Hairline()
    }
}

@Composable
private fun AnnouncementRow(
    post: Announcement,
    pickerOpen: Boolean,
    onTogglePicker: () -> Unit,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The avatar is back, and the argument against it is worth keeping: every
    // post here is by the same author, so a repeated mark down the left margin
    // states a constant once per post and costs the body a 52dp indent to do
    // it. What that argument missed is that the indent is the point — it is
    // what makes a run of posts read as a conversation rather than as a list of
    // paragraphs, which is the shape every messaging app converged on. The mark
    // is the Student Union's own, so the channel is signed rather than labelled.
    //
    // No role badge, though. That one really was the constant said twice.
    Row(modifier = modifier.fillMaxWidth()) {
        AuthorAvatar()
        Spacer(Modifier.size(Dimens.Space))
        Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = post.author,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 15.sp,
                color = Color.White,
            )
            Spacer(Modifier.size(Dimens.SpaceSm))
            Text(
                text = rememberRelativeTime(post.postedAtMillis),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 11.sp,
                color = Ink.Faint,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = post.body,
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 14.sp,
            lineHeight = 1.45.em,
            color = Ink.Label,
        )

        Spacer(Modifier.height(Dimens.SpaceSm))
        ReactionRow(
            reactions = post.reactions,
            pickerOpen = pickerOpen,
            onTogglePicker = onTogglePicker,
            onReact = onReact,
        )
        }
    }
}

/**
 * The Student Union's mark, as the channel's avatar.
 *
 * The monogram alone, cut out of `art/su.png` — the full lockup carries
 * "STUDENT UNION / MAE FAH LUANG UNIVERSITY" under it, which at 40dp is a grey
 * smear under a shape. The name is already spelled out beside this.
 *
 * A rounded square, not a circle, at exactly the geometry `RowIcon` uses for
 * every icon plate in Settings and Profile — 40dp at [Dimens.RadiusSm]. Three
 * reasons, in increasing order of how much they matter:
 *
 *  - The mark is angular. A circle crops toward a shape it does not have, and
 *    wastes its corners doing it.
 *  - This app is built out of rounded rectangles. Circles are spent on the
 *    profile button, the drag knob and the reaction chips — controls you press.
 *    An avatar is not one.
 *  - A circle means a person. Every app that carries both — Slack, Discord,
 *    Teams — rounds people and squares organisations, and the Student Union is
 *    an organisation. Messenger is all circles because everything in it is
 *    somebody.
 *
 * Plain fill rather than the app's glass: `glassSurface` brings a hairline with
 * it, and five outlined plates down the margin is a column of frames where a
 * column of marks belongs.
 */
@Composable
private fun AuthorAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .background(Color.White.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_su_mark),
            contentDescription = null,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun ReactionRow(
    reactions: List<Reaction>,
    pickerOpen: Boolean,
    onTogglePicker: () -> Unit,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            reactions.forEach { reaction ->
                ReactionChip(
                    reaction = reaction,
                    onClick = { onReact(reaction.emoji) },
                )
            }
            AddReactionChip(open = pickerOpen, onClick = onTogglePicker)
        }

        // The picker sits under the chips rather than over them: a popup would
        // cover the post you are reacting to, and this row is five items wide.
        if (pickerOpen) {
            Spacer(Modifier.height(Dimens.SpaceXs))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusPill))
                    .glassSurface(cornerRadius = Dimens.RadiusPill)
                    .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                ReactionPalette.forEach { emoji ->
                    Text(
                        text = emoji,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onReact(emoji) }
                            .padding(Dimens.SpaceXs),
                        fontSize = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionChip(
    reaction: Reaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    // "👍, 63" as one announcement, and lit-or-not carried by `toggleable`'s own
    // state rather than by a colour a screen reader cannot see. Without this the
    // chip is an emoji and a number read as two unrelated nodes.
    val label = stringResource(R.string.events_reaction_desc, reaction.emoji, reaction.count)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            // Yours is lit and outlined, everyone else's is a plain grey chip —
            // the same tell Discord uses, and the only way to read your own
            // reaction back off a count you're already inside.
            .background(
                if (reaction.mine) accent.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (reaction.mine) accent.copy(alpha = 0.55f) else Color.Transparent,
                shape = RoundedCornerShape(Dimens.RadiusPill),
            )
            .toggleable(value = reaction.mine, role = Role.Checkbox) { onClick() }
            .padding(horizontal = Dimens.SpaceSm, vertical = 3.dp)
            .clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = reaction.emoji, fontSize = 13.sp)
        Spacer(Modifier.size(Dimens.SpaceXs))
        Text(
            text = "${reaction.count}",
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 12.sp,
            color = if (reaction.mine) accent else Ink.Muted,
        )
    }
}

@Composable
private fun AddReactionChip(
    open: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The visible label is a bare "+", which reads as nothing at all out loud.
    val label = stringResource(R.string.events_add_reaction_desc)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .background(Color.White.copy(alpha = if (open) 0.16f else 0.08f))
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = Dimens.SpaceSm, vertical = 3.dp)
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.events_add_reaction),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 12.sp,
            color = Ink.Muted,
        )
    }
}

/** How long a failed-reaction line stays up before it clears itself. */
private const val ReactionNoticeMillis = 5_000L

/**
 * Why the last reaction did not stick, in the reader's language.
 *
 * Same shape as [failureText] for a post below: su-server's own message where it
 * sent one, an app string where it did not.
 */
@Composable
private fun ReactionOutcome?.failureText(): String? = when (this) {
    null, ReactionOutcome.Saved -> null
    ReactionOutcome.Offline -> stringResource(R.string.events_reaction_offline)
    is ReactionOutcome.Rejected -> message ?: stringResource(R.string.events_reaction_failed)
}

/**
 * Why the last announcement did not go out, in the reader's language.
 *
 * A rejection carries su-server's own Thai message — already written for the
 * person reading it — and falls back to an app string only when the server sent
 * none. Offline is the app's own sentence, because there was no server to ask.
 *
 * Null for success and for no attempt yet: both mean nothing to say.
 */
@Composable
private fun PostOutcome?.failureText(): String? = when (this) {
    null, PostOutcome.Posted -> null
    PostOutcome.Offline -> stringResource(R.string.events_post_offline)
    is PostOutcome.Rejected -> message ?: stringResource(R.string.events_post_failed)
}

/**
 * The Student Union's side of the channel — see [EventsScreen]'s `isStaff`.
 *
 * Fully controlled: the draft, whether a send is in flight and why the last one
 * failed all come from above. It held its own draft until this posted to a real
 * server, at which point "may I clear the box" stopped being a question the
 * composer could answer — the send had not happened yet when the tap did.
 */
@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    sending: Boolean,
    failure: String?,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    // Nothing to send while one is already going, so a double tap cannot publish
    // the same announcement twice to two thousand phones.
    val canSend = draft.isNotBlank() && !sending

    Column(modifier = modifier.fillMaxWidth()) {
        if (failure != null) {
            Text(
                text = failure,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                lineHeight = 1.4.em,
                color = Palette.Alert,
                modifier = Modifier.padding(
                    start = Dimens.CardPadding,
                    end = Dimens.CardPadding,
                    bottom = Dimens.SpaceSm,
                ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = Dimens.RadiusPill)
                .padding(start = Dimens.CardPadding, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (draft.isEmpty()) {
                    Text(
                        text = stringResource(R.string.events_composer_hint),
                        fontFamily = AppSans,
                        fontWeight = AppTextWeight,
                        fontSize = 14.sp,
                        color = Ink.Placeholder,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    // Locked while the request is in flight. The draft is about to
                    // be cleared or kept depending on the answer, and an edit
                    // landing in that window belongs to neither outcome.
                    enabled = !sending,
                    textStyle = TextStyle(
                        fontFamily = AppSans,
                        fontWeight = AppTextWeight,
                        fontSize = 14.sp,
                        color = Color.White,
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.size(Dimens.SpaceSm))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) accent else Color.White.copy(alpha = 0.10f))
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        color = Ink.Label,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(17.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = stringResource(R.string.events_send),
                        tint = if (canSend) Palette.Ink else Ink.Faint,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun EventsPreview() {
    SUClubFairTheme {
        // The shell supplies the backdrop in the app; stand one in for the preview.
        Box {
            MeshBackground()
            EventsScreen()
        }
    }
}
