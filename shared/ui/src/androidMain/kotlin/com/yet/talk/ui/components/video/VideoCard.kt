package com.yet.talk.ui.components.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yet.talk.domain.model.video.Video
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes
import com.yet.talk.ui.theme.talkTypography

/** A catalog video (`VideoCard`): thumbnail, title, `channel · category · level`, why HN recommends it. Opens the video page. */
@Composable
fun VideoCard(video: Video, onOpen: (String) -> Unit, modifier: Modifier = Modifier, small: Boolean = false) {
    val colors = MaterialTheme.talkColors
    Column(
        modifier.clip(MaterialTheme.talkShapes.card).clickable(role = Role.Button) { onOpen(video.id) }.padding(MaterialTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9).clip(MaterialTheme.talkShapes.card).background(colors.surface), contentAlignment = Alignment.Center) {
            AsyncImage(model = video.thumbnail(small), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9))
            Box(Modifier.size(40.dp).clip(CircleShape).background(colors.mediaScrim), contentAlignment = Alignment.Center) {
                TalkIcon(TalkIcons.Play, contentDescription = null, size = 20.dp, tint = Color.White)
            }
        }
        Text(video.title, style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(video.channel, video.categories.firstOrNull() ?: video.primaryTopic, video.level?.label()).joinToString(" · "),
            style = MaterialTheme.talkTypography.small, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (!small) video.whyHn?.let { Text(it, style = MaterialTheme.talkTypography.small, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
}

/** "intro" → "Intro": the catalog's levels and types, as the design writes them. */
fun String.label(): String = replaceFirstChar { it.uppercase() }
