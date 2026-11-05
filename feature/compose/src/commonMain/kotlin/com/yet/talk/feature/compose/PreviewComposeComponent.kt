package com.yet.talk.feature.compose

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.rule.ContentRules

/** For UI previews: a fixed model, actions do nothing. */
class PreviewComposeComponent(
    model: ComposeComponent.Model = ComposeComponent.Model(
        mode = ComposeMode.New,
        text = "## A heading\n\nSome **markdown**.",
        images = emptyList(),
        target = null,
        view = ComposeComponent.View.Split,
        maxLength = ContentRules.MAX_POST_LENGTH,
        isUploading = false,
        isPublishing = false,
        canPublish = true,
        error = null,
    ),
) : ComposeComponent {
    override val model: Value<ComposeComponent.Model> = MutableValue(model)

    override fun onTextChanged(text: String) = Unit

    override fun onViewChanged(view: ComposeComponent.View) = Unit

    override fun onAttachImage(file: ImageFile) = Unit

    override fun onRemoveImage(url: String) = Unit

    override fun onPublish() = Unit

    override fun onClose() = Unit

    override fun onErrorShown() = Unit
}
