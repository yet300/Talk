package com.yet.talk.feature.chat.component.verify

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.chat.SafetyCode
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import kotlin.time.Instant

/**
 * Verify a contact (`VerifyContact`): the safety code from both keys, the same on both devices, and its QR code to
 * scan in person; "Mark as verified" once they match. Kept on this device only.
 */
interface VerifyComponent {
    val model: Value<Model>

    fun onMarkVerified()

    fun onUnverify()

    /** A QR code was scanned: verified when it is this contact's code. */
    fun onScanned(payload: String)

    fun onBack()

    fun onErrorShown()

    data class Model(
        val person: User,
        val code: SafetyCode?,
        val verifiedAt: Instant?,
        /** The last scan did not match. */
        val scanMismatch: Boolean,
        val error: DomainError?,
    )

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, person: UserId, onBack: () -> Unit): VerifyComponent
    }
}
