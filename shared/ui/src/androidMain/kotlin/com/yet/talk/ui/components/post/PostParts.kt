package com.yet.talk.ui.components.post

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.PostLink
import com.yet.talk.resources.*
import com.yet.talk.resources.displayLabel
import com.yet.talk.resources.handle
import com.yet.talk.resources.toRelativeDesc
import com.yet.talk.ui.components.LocalPostCards
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes
import com.yet.talk.ui.theme.talkTypography
import com.yet.talk.ui.theme.text

/** A post's images (`x-media`): one at its own shape (up to 16:9 tall), several in a two-column grid. */
@Composable
fun MediaGrid(images: List<Image>) {
    val shape = MaterialTheme.talkShapes.card
    val border = MaterialTheme.talkColors.border
    if (images.size == 1) {
        val image = images.single()
        val ratio = image.width?.let { w -> image.height?.takeIf { it > 0 }?.let { h -> w.toFloat() / h } }?.coerceIn(0.75f, 2f) ?: (16f / 9)
        Picture(image, Modifier.fillMaxWidth().aspectRatio(ratio).clip(shape).border(1.dp, border, shape))
        return
    }
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, border, shape), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        images.take(4).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { Picture(it, Modifier.weight(1f).aspectRatio(1f)) }
            }
        }
    }
}

@Composable
private fun Picture(image: Image, modifier: Modifier) {
    AsyncImage(
        model = image.url,
        contentDescription = image.alt,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(MaterialTheme.talkColors.surface),
    )
}

/** A quoted post (`x-quote`): its author, three lines of its text or title, an image mark when it has images. */
@Composable
fun QuotedCard(id: PostId, onOpen: (Post) -> Unit) {
    val cards = LocalPostCards.current
    val quoted by remember(id, cards) { cards.quote(id) }.collectAsState(null)
    val post = quoted ?: return
    val colors = MaterialTheme.talkColors
    val shape = MaterialTheme.talkShapes.card
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, colors.border, shape)
            .clickable { onOpen(post) }
            .padding(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PostHead(post, onEdited = null)
        Text(
            if (post.summary.isArticle) post.summary.title else post.summary.text,
            style = MaterialTheme.talkTypography.body,
            color = colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (post.images.isNotEmpty()) TalkIcon(TalkIcons.Image, contentDescription = null, tint = colors.textSecondary)
    }
}

/**
 * The link below a post: a YouTube thumbnail that opens the video page, or a row with the address that opens the
 * browser. GitHub cards come with the post page.
 */
@Composable
fun LinkCard(link: PostLink) {
    val colors = MaterialTheme.talkColors
    val shape = MaterialTheme.talkShapes.card
    val uri = LocalUriHandler.current
    val cards = LocalPostCards.current
    when (link) {
        is PostLink.YouTube -> Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9).clip(shape).border(1.dp, colors.border, shape)
                .clickable(role = Role.Button) { cards.openVideo(link.videoId) },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = link.thumbnail(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9),
            )
            Box(Modifier.size(56.dp).clip(CircleShape).background(colors.mediaScrim), contentAlignment = Alignment.Center) {
                TalkIcon(TalkIcons.Play, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
            }
        }
        else -> Row(
            Modifier.fillMaxWidth().clip(shape).border(1.dp, colors.border, shape)
                .clickable(role = Role.Button) { uri.openUri(link.url) }
                .padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            TalkIcon(if (link is PostLink.GitHub) TalkIcons.GitHub else TalkIcons.Link, contentDescription = null, tint = colors.textSecondary)
            Text(
                link.url.removePrefix("https://").removePrefix("http://").removeSuffix("/"),
                style = MaterialTheme.talkTypography.body,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Name, `handle · time`, and Edited (opens the revisions) when [onEdited] is set and the post was edited. */
@Composable
fun PostHead(post: Post, onEdited: (() -> Unit)?) {
    val colors = MaterialTheme.talkColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(post.author.displayLabel, style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text(
            "${post.author.handle} · ${post.createdAt.toRelativeDesc().text()}",
            style = MaterialTheme.talkTypography.body,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (post.editedAt != null && onEdited != null) {
            Text(
                stringResource(MR.strings.post_edited),
                style = MaterialTheme.talkTypography.body,
                color = colors.textSecondary,
                modifier = Modifier.clickable(role = Role.Button, onClick = onEdited),
            )
        }
    }
}
