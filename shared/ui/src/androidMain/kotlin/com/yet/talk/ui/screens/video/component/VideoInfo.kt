package com.yet.talk.ui.screens.video.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yet.talk.feature.video.VideoComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ToggleAction
import com.yet.talk.ui.components.video.label
import com.yet.talk.ui.platform.copyText
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The details (`VideoInfo`): title, `channel · N likes · N comments`, Like, Watch later, Share, Copy link, the summary,
 * why Hacker News recommends it, the category, type and level, and the HackerTube credit.
 */
@Composable
fun VideoInfo(model: VideoComponent.Model, component: VideoComponent) {
    val video = model.video
    val colors = MaterialTheme.talkColors
    val context = LocalContext.current
    val copied = stringResource(MR.strings.toast_link_copied)
    Column(Modifier.padding(MaterialTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        Text(video?.title ?: model.url, style = MaterialTheme.talkTypography.headingLarge, color = colors.textPrimary)
        Text(
            stringResource(MR.strings.video_stats, video?.channel.orEmpty(), model.stats.likes.toString(), model.stats.comments.toString()),
            style = MaterialTheme.talkTypography.small, color = colors.textSecondary,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            ToggleAction(if (model.viewer.liked) TalkIcons.LikeFilled else TalkIcons.Like, stringResource(MR.strings.video_like), colors.like.takeIf { model.viewer.liked }, component::onLike)
            ToggleAction(TalkIcons.Clock, stringResource(MR.strings.video_later), colors.accent.takeIf { model.viewer.watchLater }, component::onWatchLater)
            ToggleAction(TalkIcons.Share, stringResource(MR.strings.video_share), null, component::onShare)
            ToggleAction(TalkIcons.Link, stringResource(MR.strings.video_copy_link), null) { copyText(context, model.url, copied) }
        }
        if (video == null) return@Column
        video.summary?.let { Text(it, style = MaterialTheme.talkTypography.body, color = colors.textPrimary) }
        if (video.whyHn != null) {
            Column(
                Modifier.clip(RoundedCornerShape(12.dp)).background(colors.surface).padding(MaterialTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                Text(stringResource(MR.strings.video_why), style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary)
                Text(video.whyHn.orEmpty(), style = MaterialTheme.talkTypography.body, color = colors.textPrimary)
                video.quote?.let { Text("“$it”", style = MaterialTheme.talkTypography.body, color = colors.textSecondary) }
                if (video.recommenders > 0) Text(stringResource(MR.strings.video_recommenders, video.recommenders.toString()), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
            }
        }
        val labels = video.categories + listOfNotNull(video.type?.label(), video.level?.label())
        if (labels.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                Text(labels.joinToString("  ·  "), style = MaterialTheme.talkTypography.small, color = colors.accent)
            }
        }
        if (video.whyHn != null) Text(stringResource(MR.strings.videos_credit), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
    }
}
