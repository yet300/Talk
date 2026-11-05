package com.yet.talk.web.ui.screens.revisions.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.post.Revision
import com.yet.talk.domain.rule.PostSummaries
import com.yet.talk.domain.rule.RevisionDiff
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * The versions (`RevisionList`): "Published" (the latest on relays) and the earlier ones, each with its
 * date, its title and the lines it changed; the limited state when only the latest exists.
 */
@Composable
fun RevisionList(model: RevisionsComponent.Model, onSelect: (Int) -> Unit) {
    val revisions = model.revisions
    val latest = revisions.firstOrNull() ?: return
    Div({ classes("t-part") }) {
        Group(stringResource(MR.strings.revisions_published))
        Row(
            title = stringResource(MR.strings.revisions_published_version),
            hint = stringResource(MR.strings.revisions_latest_hint, latest.createdAt.toDateText()),
            diff = null,
            selected = model.selected == latest,
        ) { onSelect(latest.number) }
        if (revisions.size == 1) {
            EmptyState(
                icon = TalkIcons.History,
                title = stringResource(MR.strings.revisions_empty_title),
                body = stringResource(MR.strings.revisions_empty_body),
            )
        } else {
            Group(stringResource(MR.strings.revisions_earlier))
            revisions.drop(1).forEachIndexed { index, revision ->
                val older = revisions.getOrNull(index + 2)
                Row(
                    title = revision.createdAt.toDateText(),
                    hint = PostSummaries.of(revision.content).title,
                    diff = older?.let { diffText(it, revision) },
                    selected = model.selected == revision,
                ) { onSelect(revision.number) }
            }
            P({ classes("t-caption", "t-revisions-caption") }) { Text(stringResource(MR.strings.revisions_limited)) }
        }
    }
}

@Composable
private fun Group(label: String) {
    H2({ classes("t-caption", "t-revision-group") }) { Text(label) }
}

@Composable
private fun Row(title: String, hint: String, diff: String?, selected: Boolean, onClick: () -> Unit) {
    A(href = "#", attrs = {
        classes("t-row")
        if (selected) {
            classes("t-rev-on")
            attr("aria-current", "true")
        }
        onClick { it.preventDefault(); onClick() }
    }) {
        Div({ classes("t-row-text") }) {
            Div({ classes("x-name") }) { Text(title) }
            Div({ classes("t-caption") }) { Text(hint) }
        }
        if (diff != null) Span({ classes("t-caption") }) { Text(diff) }
    }
}

private fun diffText(older: Revision, newer: Revision): String {
    val changes = RevisionDiff.lineChanges(older.content, newer.content)
    return "+${changes.added} −${changes.removed}"
}
