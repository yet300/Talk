package com.yet.talk.feature.post.component.revisions

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

/** For UI previews: a fixed model, actions do nothing. */
class PreviewRevisionsComponent(
    model: RevisionsComponent.Model = RevisionsComponent.Model(
        revisions = emptyList(), selected = null, isLatest = true, isLoading = true, error = null,
    ),
) : RevisionsComponent {
    override val model: Value<RevisionsComponent.Model> = MutableValue(model)

    override fun onSelect(number: Int) = Unit

    override fun onRefresh() = Unit

    override fun onBack() = Unit

    override fun onErrorShown() = Unit
}
