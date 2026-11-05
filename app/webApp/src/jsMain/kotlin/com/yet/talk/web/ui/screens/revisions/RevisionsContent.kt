package com.yet.talk.web.ui.screens.revisions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.SkeletonCards
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.components.markdown.Markdown
import com.yet.talk.web.ui.screens.revisions.component.RevisionList
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Revisions (`Revisions · Web`): the selected version in the main column, read-only with a way back to the
 * published one; the list in the right column ([RevisionsSide]), or above the text when that column hides.
 */
@Composable
fun RevisionsContent(component: RevisionsComponent) {
    val model by component.model.subscribeAsState()
    TopBar(stringResource(MR.strings.revisions_title), onBack = component::onBack)
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    Div({ classes("t-narrow-only") }) { RevisionList(model, component::onSelect) }
    val selected = model.selected
    when {
        selected == null && model.isLoading -> SkeletonCards(count = 1)
        selected != null -> {
            if (!model.isLatest) {
                Div({ classes("t-notice") }) {
                    Span({ classes("t-grow") }) {
                        Span({ classes("t-strong") }) { Text(stringResource(MR.strings.revisions_snapshot, selected.createdAt.toDateText())) }
                    }
                    TalkButton(
                        stringResource(MR.strings.revisions_published_version),
                        onClick = { model.revisions.firstOrNull()?.let { component.onSelect(it.number) } },
                        style = ButtonStyle.Secondary,
                    )
                }
            }
            Div({ classes("t-article-body", "t-revision-body") }) { Markdown(selected.content) }
        }
    }
}

/** The right column: the list of versions. */
@Composable
fun RevisionsSide(component: RevisionsComponent) {
    val model by component.model.subscribeAsState()
    Div({ classes("t-wide-only") }) { RevisionList(model, component::onSelect) }
}
