package com.yet.talk.feature.auth

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

/** For UI previews: a fixed model, actions do nothing. */
class PreviewAuthComponent(
    model: AuthComponent.Model = AuthComponent.Model(key = "", isWorking = false, canImport = false, newKey = null, newKeyCopied = false, canConfirmSaved = false, error = null),
) : AuthComponent {
    override val model: Value<AuthComponent.Model> = MutableValue(model)

    override fun onKeyChanged(key: String) = Unit

    override fun onImportKey() = Unit

    override fun onCreateKey() = Unit

    override fun onNewKeyCopied() = Unit

    override fun onNewKeySaved() = Unit

    override fun onErrorShown() = Unit
}
