package com.yet.talk.feature.editprofile

import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.feature.editprofile.mapper.stateToModel
import com.yet.talk.feature.editprofile.store.EditProfileStore
import com.yet.talk.feature.editprofile.store.EditProfileStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

internal class DefaultEditProfileComponent(
    componentContext: ComponentContext,
    private val output: (EditProfileComponent.Output) -> Unit,
    storeFactory: EditProfileStoreFactory,
) : EditProfileComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    init {
        coroutineScope().launch {
            store.labels.collect { label ->
                when (label) {
                    EditProfileStore.Label.Saved -> output(EditProfileComponent.Output.Saved)
                }
            }
        }
    }

    override val model: Value<EditProfileComponent.Model> = store.asValue().map(stateToModel)

    override fun onFieldChanged(field: EditProfileComponent.Field, text: String) =
        store.accept(EditProfileStore.Intent.ChangeField(field, text))

    override fun onImageLinkSubmitted(kind: ProfileImage, link: String) = store.accept(EditProfileStore.Intent.SubmitImageLink(kind, link))

    override fun onSave() = store.accept(EditProfileStore.Intent.Save)

    override fun onCancel() = output(EditProfileComponent.Output.Closed)

    override fun onErrorShown() = store.accept(EditProfileStore.Intent.DismissError)
}

@Inject
internal class DefaultEditProfileComponentFactory(private val storeFactory: EditProfileStoreFactory) : EditProfileComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (EditProfileComponent.Output) -> Unit): EditProfileComponent =
        DefaultEditProfileComponent(componentContext, output, storeFactory)
}
