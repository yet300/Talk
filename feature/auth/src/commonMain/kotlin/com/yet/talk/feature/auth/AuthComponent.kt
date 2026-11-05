package com.yet.talk.feature.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError

/** Sign in with a Nostr key: create a new one or paste an `nsec`. The root switches to the app once signed in. */
interface AuthComponent {
    val model: Value<Model>

    fun onKeyChanged(key: String)

    fun onImportKey()

    /** Creates a key and shows it once ([Model.newKey]); signing in waits for [onNewKeySaved]. */
    fun onCreateKey()

    /** The new key was copied: "I saved it" unlocks at once. */
    fun onNewKeyCopied()

    /** "I saved it": sign in with the new key. */
    fun onNewKeySaved()

    fun onErrorShown()

    data class Model(
        val key: String,
        val isWorking: Boolean,
        val canImport: Boolean,
        /** The `nsec` of a key just created, shown once ("Save your key"); null otherwise. */
        val newKey: String?,
        val newKeyCopied: Boolean,
        val canConfirmSaved: Boolean,
        val error: DomainError?,
    )

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext): AuthComponent
    }
}
