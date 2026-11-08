package com.yet.talk.ui.screens.revisions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.SkeletonCards
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkMarkdown
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.screens.revisions.component.revisionList
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * Revisions (`Revisions-Mobile`): the list of versions on top, the selected one below it, read-only, with Published
 * version to go back to the latest. Picking a version crossfades the text.
 */
@Composable
fun RevisionsContent(component: RevisionsComponent) {
    val model by component.model.subscribeAsState()
    GlassPage(bar = { BackBar(stringResource(MR.strings.revisions_title), onBack = component::onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item(key = "error") { ErrorRow(model.error, onDismiss = component::onErrorShown) }
            revisionList(model, component::onSelect)
            item(key = "divider") { HorizontalDivider(color = MaterialTheme.talkColors.border, thickness = MaterialTheme.sizes.hairline) }
            item(key = "snapshot") {
                val selected = model.selected
                AnimatedContent(selected, contentKey = { it?.number }, transitionSpec = { fadeIn(TalkMotion.fade()) togetherWith fadeOut(TalkMotion.fade()) }, label = "revision") { revision ->
                    when {
                        revision == null && model.isLoading -> SkeletonCards(1)
                        revision != null -> androidx.compose.foundation.layout.Column {
                            AnimatedVisibility(!model.isLatest) {
                                Row(
                                    Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg).background(MaterialTheme.talkColors.surface, MaterialTheme.shapes.medium).padding(MaterialTheme.spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                                ) {
                                    Text(stringResource(MR.strings.revisions_snapshot, revision.createdAt.toDateText()), style = MaterialTheme.talkTypography.bodyBold, color = MaterialTheme.talkColors.textPrimary, modifier = Modifier.weight(1f))
                                    TalkButton(stringResource(MR.strings.revisions_published_version), { model.revisions.firstOrNull()?.let { component.onSelect(it.number) } }, ButtonStyle.Secondary)
                                }
                            }
                            TalkMarkdown(revision.content, Modifier.padding(MaterialTheme.spacing.lg))
                        }
                    }
                }
            }
        }
    }
}
