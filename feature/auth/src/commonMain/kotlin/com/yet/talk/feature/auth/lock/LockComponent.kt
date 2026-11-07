package com.yet.talk.feature.auth.lock

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError

/**
 * The lock screen (`AppLock`, "Talk is locked"): the passcode opens the key (NIP-49, the web). A forgotten passcode
 * means signing in again with the `nsec` backup, so [onForgot] erases this device's account.
 */
interface LockComponent {
    val model: Value<Model>

    fun onPasscodeChanged(passcode: String)

    fun onUnlock()

    /** Forgot the passcode: erase everything on this device (after the UI's warning). */
    fun onForgot()

    fun onErrorShown()

    data class Model(val passcode: String, val isUnlocking: Boolean, val error: DomainError?)

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext): LockComponent
    }
}
