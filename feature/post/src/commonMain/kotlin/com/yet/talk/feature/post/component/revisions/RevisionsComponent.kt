package com.yet.talk.feature.post.component.revisions

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.Revision

/** A post's revision history (kind 1010), newest first; every revision is read-only. */
interface RevisionsComponent {
    val model: Value<Model>

    fun onSelect(number: Int)

    fun onRefresh()

    fun onBack()

    fun onErrorShown()

    data class Model(
        val revisions: List<Revision>,
        /** The revision shown; the latest one until another is picked. */
        val selected: Revision?,
        val isLatest: Boolean,
        val isLoading: Boolean,
        val error: DomainError?,
    )

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, id: PostId, onBack: () -> Unit): RevisionsComponent
    }
}
