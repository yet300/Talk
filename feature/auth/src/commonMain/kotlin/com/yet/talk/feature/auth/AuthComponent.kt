package com.yet.talk.feature.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError

/** Sign in with a Nostr key: create a new one or paste an `nsec`. The root switches to the app once signed in. */
interface AuthComponent {
    val model: Value<Model>

    fun onKeyChanged(key: String)

    fun onImportKey()

    fun onCreateKey()

    fun onErrorShown()

    data class Model(
        val key: String,
        val isWorking: Boolean,
        val canImport: Boolean,
        val error: DomainError?,
    )

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext): AuthComponent
    }
}
