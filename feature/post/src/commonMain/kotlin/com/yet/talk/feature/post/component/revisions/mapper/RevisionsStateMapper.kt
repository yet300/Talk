package com.yet.talk.feature.post.component.revisions.mapper

import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.feature.post.component.revisions.store.RevisionsStore

internal val stateToModel: (RevisionsStore.State) -> RevisionsComponent.Model = { state ->
    val latest = state.revisions.firstOrNull()
    val selected = state.revisions.firstOrNull { it.number == state.selectedNumber } ?: latest
    RevisionsComponent.Model(
        revisions = state.revisions,
        selected = selected,
        isLatest = selected != null && selected == latest,
        isLoading = state.isLoading && state.revisions.isEmpty(),
        error = state.error,
    )
}
