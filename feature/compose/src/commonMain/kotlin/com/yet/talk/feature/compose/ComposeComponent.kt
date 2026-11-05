package com.yet.talk.feature.compose

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId

/** The markdown editor with a live preview. The draft survives configuration changes and process death. */
interface ComposeComponent {
    val model: Value<Model>

    fun onTextChanged(text: String)

    fun onViewChanged(view: View)

    /** Uploads to Blossom and inserts the image's markdown. */
    fun onAttachImage(file: ImageFile)

    fun onRemoveImage(url: String)

    fun onPublish()

    fun onClose()

    fun onErrorShown()

    enum class View { Edit, Split, Preview }

    data class Model(
        val mode: ComposeMode,
        val text: String,
        val images: List<Image>,
        /** The parent, quoted or edited post, once loaded. */
        val target: Post?,
        val view: View,
        val maxLength: Int,
        val isUploading: Boolean,
        val isPublishing: Boolean,
        val canPublish: Boolean,
        val error: DomainError?,
    )

    sealed interface Output {
        data class Published(val id: PostId) : Output

        data object Closed : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, mode: ComposeMode, output: (Output) -> Unit): ComposeComponent
    }
}
