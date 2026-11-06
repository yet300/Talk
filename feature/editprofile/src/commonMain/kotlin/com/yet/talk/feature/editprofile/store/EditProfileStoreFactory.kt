package com.yet.talk.feature.editprofile.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.domain.rule.ProfileImageHosts
import com.yet.talk.domain.usecase.profile.UpdateProfile
import com.yet.talk.feature.editprofile.EditProfileComponent.Field
import com.yet.talk.feature.editprofile.EditProfileComponent.Nip05Status
import com.yet.talk.feature.editprofile.store.EditProfileStore.Intent
import com.yet.talk.feature.editprofile.store.EditProfileStore.Label
import com.yet.talk.feature.editprofile.store.EditProfileStore.Msg
import com.yet.talk.feature.editprofile.store.EditProfileStore.Slot
import com.yet.talk.feature.editprofile.store.EditProfileStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Inject
internal class EditProfileStoreFactory(
    private val storeFactory: StoreFactory,
    private val accounts: AccountRepository,
    private val profiles: ProfileRepository,
    private val updateProfile: UpdateProfile,
) {
    fun create(): EditProfileStore =
        object : EditProfileStore, Store<Intent, State, Label> by storeFactory.create(
            name = "EditProfileStore",
            initialState = State(),
            bootstrapper = SimpleBootstrapper(EditProfileStore.Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, EditProfileStore.Action, State, Msg, Label>() {
        private var nip05Check: Job? = null

        override fun executeAction(action: EditProfileStore.Action) {
            scope.launch {
                val me = accounts.account.filterNotNull().first().userId
                // The cached profile shows at once; the relays' latest replaces it unless the user already started editing.
                launch { profiles.observeUser(me).collect { if (!state().isEdited) dispatch(Msg.UserLoaded(it)) } }
                profiles.refresh(me) // offline, the cached profile is what there is
                dispatch(Msg.Loaded)
                checkNip05(delayMillis = 0)
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.ChangeField -> {
                    dispatch(Msg.FieldChanged(intent.field, intent.text))
                    if (intent.field == Field.Nip05) checkNip05(NIP05_PAUSE_MILLIS)
                }
                is Intent.SubmitImageLink -> submitImageLink(intent.kind, intent.link)
                Intent.Save -> save()
                Intent.DismissError -> dispatch(Msg.Failed(null))
            }
        }

        /** Checks the identifier after the user pauses typing; an empty one has nothing to check. */
        private fun checkNip05(delayMillis: Long) {
            nip05Check?.cancel()
            val identifier = state().field(Field.Nip05).trim()
            val me = state().user?.id
            if (identifier.isEmpty() || me == null) {
                dispatch(Msg.Nip05Checked(Nip05Status.None))
                return
            }
            nip05Check = scope.launch {
                delay(delayMillis)
                dispatch(Msg.Nip05Checked(Nip05Status.Checking))
                val verified = profiles.verifyNip05(me, identifier).getOrDefault(false)
                dispatch(Msg.Nip05Checked(if (verified) Nip05Status.Verified else Nip05Status.NotVerified))
            }
        }

        private fun submitImageLink(kind: ProfileImage, link: String) {
            val url = ProfileImageHosts.normalize(link) ?: return dispatch(Msg.Failed(DomainError.ImageHostNotAllowed))
            dispatch(Msg.Failed(null))
            dispatch(Msg.SlotChanged(kind, Slot(url, changed = true)))
        }

        private fun save() {
            val state = state()
            if (state.isSaving) return
            dispatch(Msg.Saving(true))
            scope.launch {
                val edit = ProfileEdit(
                    name = state.field(Field.Name),
                    displayName = state.field(Field.DisplayName),
                    about = state.field(Field.About),
                    picture = state.avatar.url.takeIf { state.avatar.changed },
                    banner = state.banner.url.takeIf { state.banner.changed },
                    website = state.field(Field.Website),
                    nip05 = state.field(Field.Nip05),
                )
                val result = updateProfile(edit)
                dispatch(Msg.Saving(false))
                result.onSuccess { publish(Label.Saved) }.onFailure { dispatch(Msg.Failed(it as? DomainError)) }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.UserLoaded -> copy(
                user = msg.user,
                fields = with(msg.user) {
                    mapOf(
                        Field.Name to name.orEmpty(), Field.DisplayName to displayName.orEmpty(), Field.About to about.orEmpty(),
                        Field.Website to website.orEmpty(), Field.Nip05 to nip05.orEmpty(),
                    )
                },
                avatar = Slot(msg.user.picture),
                banner = Slot(msg.user.banner),
            )
            Msg.Loaded -> copy(isLoaded = true)
            is Msg.FieldChanged -> copy(fields = fields + (msg.field to msg.text), isEdited = true)
            is Msg.SlotChanged -> if (msg.kind == ProfileImage.Avatar) copy(avatar = msg.slot, isEdited = true) else copy(banner = msg.slot, isEdited = true)
            is Msg.Nip05Checked -> copy(nip05Status = msg.status)
            is Msg.Saving -> copy(isSaving = msg.saving)
            is Msg.Failed -> copy(error = msg.error)
        }
    }

    private companion object {
        const val NIP05_PAUSE_MILLIS = 600L
    }
}

