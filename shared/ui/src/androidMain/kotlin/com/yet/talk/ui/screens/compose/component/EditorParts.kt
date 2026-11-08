package com.yet.talk.ui.screens.compose.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.feature.compose.ComposeMode
import com.yet.talk.resources.*
import com.yet.talk.ui.components.TalkIconButton
import com.yet.talk.ui.components.post.PostHead
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes
import com.yet.talk.ui.theme.talkTypography

/** What the editor works on (decision 5): "Reply" / "Quote post" / "Edit post" and that post's head and title. */
@Composable
fun TargetPreview(mode: ComposeMode, post: Post) {
    val heading = when (mode) {
        is ComposeMode.Reply -> MR.strings.compose_reply
        is ComposeMode.Quote -> MR.strings.compose_quote
        is ComposeMode.Edit -> MR.strings.compose_edit
        else -> return
    }
    val colors = MaterialTheme.talkColors
    Column(Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(heading), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
        if (mode !is ComposeMode.Edit) {
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.talkShapes.card).border(1.dp, colors.border, MaterialTheme.talkShapes.card).padding(MaterialTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PostHead(post, onEdited = null)
                Text(post.summary.title, style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Attached images (also in the text as markdown); each can be removed. */
@Composable
fun ImageStrip(images: List<Image>, onRemove: (String) -> Unit) {
    val remove = stringResource(MR.strings.compose_remove_image)
    LazyRow(contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.lg), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        items(images, key = { it.url }) { image ->
            Box(Modifier.size(88.dp).clip(MaterialTheme.talkShapes.card).animateItem()) {
                AsyncImage(image.url, contentDescription = image.alt, contentScale = ContentScale.Crop, modifier = Modifier.size(88.dp))
                TalkIconButton(TalkIcons.Close, remove, { onRemove(image.url) }, size = 18.dp, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}
