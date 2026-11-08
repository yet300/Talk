package com.yet.talk.ui.screens.revisions.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.yet.talk.domain.model.post.Revision
import com.yet.talk.domain.rule.PostSummaries
import com.yet.talk.domain.rule.RevisionDiff
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.ListRow
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The versions (`RevisionList`): "Published" (the latest on relays) and the earlier ones, each with its date, its
 * title and the lines it changed; the limited state when only the latest exists.
 */
fun LazyListScope.revisionList(model: RevisionsComponent.Model, onSelect: (Int) -> Unit) {
    val revisions = model.revisions
    val latest = revisions.firstOrNull() ?: return
    item(key = "published-title") { Caption(stringResource(MR.strings.revisions_published)) }
    item(key = "published") {
        ListRow(
            title = stringResource(MR.strings.revisions_published_version),
            caption = stringResource(MR.strings.revisions_latest_hint, latest.createdAt.toDateText()),
            selected = model.selected == latest,
            onClick = { onSelect(latest.number) },
        )
    }
    if (revisions.size == 1) {
        item(key = "limited") { EmptyState(stringResource(MR.strings.revisions_empty_title), stringResource(MR.strings.revisions_empty_body), icon = TalkIcons.History) }
        return
    }
    item(key = "earlier-title") { Caption(stringResource(MR.strings.revisions_earlier)) }
    itemsIndexed(revisions.drop(1), key = { _, it -> it.number }) { index, revision ->
        val older = revisions.getOrNull(index + 2)
        ListRow(
            title = revision.createdAt.toDateText(),
            caption = PostSummaries.of(revision.content).title,
            selected = model.selected == revision,
            onClick = { onSelect(revision.number) },
            modifier = Modifier.animateItem(),
            trailing = { older?.let { Text(diffText(it, revision), style = MaterialTheme.talkTypography.small, color = MaterialTheme.talkColors.textSecondary) } },
        )
    }
    item(key = "caption") { Caption(stringResource(MR.strings.revisions_limited)) }
}

private fun diffText(older: Revision, newer: Revision): String {
    val changes = RevisionDiff.lineChanges(older.content, newer.content)
    return "+${changes.added} −${changes.removed}"
}
