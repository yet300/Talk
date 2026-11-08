package com.yet.talk.feature.editprofile

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.account.AppLock
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.repository.GitHubRepository
import com.yet.talk.domain.model.github.GitHubAccount
import com.yet.talk.domain.model.github.GitHubCard
import com.yet.talk.domain.model.github.GitHubRef
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.domain.usecase.profile.UpdateProfile
import com.yet.talk.feature.editprofile.EditProfileComponent.Field
import com.yet.talk.feature.editprofile.EditProfileComponent.Nip05Status
import com.yet.talk.feature.editprofile.store.EditProfileStore
import com.yet.talk.feature.editprofile.store.EditProfileStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EditProfileStoreTest {

    private val me = UserId("me")

    private class FakeAccounts(id: UserId) : AccountRepository {
        override val account = MutableStateFlow<Account?>(Account(id, "npub1me"))
        override suspend fun generateKey() = error("unused")
        override suspend fun importKey(secretKey: String) = error("unused")
        override suspend fun exportSecretKey() = error("unused")
        override suspend fun signOut() = Unit
        override val lock = MutableStateFlow(AppLock())
        override suspend fun unlock(passcode: String) = Result.success(Unit)
        override suspend fun setPasscode(passcode: String) = Result.success(Unit)
        override suspend fun removePasscode() = Result.success(Unit)
    }

    private class FakeProfiles : ProfileRepository {
        val stored = MutableStateFlow(User(UserId("me"), "npub1me", name = "yara", about = "hi", picture = "https://i.giphy.com/a.gif"))
        val saved = mutableListOf<ProfileEdit>()
        override fun observeUser(id: UserId): Flow<User> = stored
        override fun observeStats(id: UserId): Flow<UserStats> = flowOf(UserStats())
        override suspend fun refresh(id: UserId) = Result.success(Unit)
        override suspend fun updateMyProfile(edit: ProfileEdit) = Result.success(Unit).also { saved += edit }
        override suspend fun verifyNip05(id: UserId, identifier: String) = Result.success(identifier == "yara@nadim.dev")
    }

    private class FakeGitHub : GitHubRepository {
        override fun card(ref: GitHubRef): Flow<GitHubCard?> = flowOf(null)
        override suspend fun proofText() = Result.success("Verifying that I control the following Nostr public key: npub1me")
        override suspend fun verifyLink(login: String, gistLink: String) =
            if (gistLink.endsWith("good")) Result.success(GitHubAccount(login, "good")) else Result.failure(DomainError.GitHubProofMissing)
    }

    private val profiles = FakeProfiles()
    private val store by lazy {
        EditProfileStoreFactory(
            DefaultStoreFactory(), FakeAccounts(me), profiles, UpdateProfile(profiles, FakeAccounts(me)), FakeGitHub(),
        ).create()
    }

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsFromTheProfileAndSavesTheEdits() = runTest {
        assertEquals("yara" to "https://i.giphy.com/a.gif", store.state.field(Field.Name) to store.state.avatar.url)
        val labels = mutableListOf<EditProfileStore.Label>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { store.labels.collect { labels += it } }

        store.accept(EditProfileStore.Intent.ChangeField(Field.About, "  new about "))
        profiles.stored.value = profiles.stored.value.copy(name = "late") // a late load does not replace what the user typed
        store.accept(EditProfileStore.Intent.SubmitImageLink(ProfileImage.Banner, "https://giphy.com/gifs/wave-abc123"))
        store.accept(EditProfileStore.Intent.Save)

        // The untouched avatar is not sent, so the current one stays.
        assertEquals(ProfileEdit(name = "yara", about = "new about", banner = "https://media.giphy.com/media/abc123/giphy.gif"), profiles.saved.single())
        assertEquals(listOf<EditProfileStore.Label>(EditProfileStore.Label.Saved), labels)
    }

    @Test
    fun refusesLinksFromOtherSites() = runTest {
        store.accept(EditProfileStore.Intent.SubmitImageLink(ProfileImage.Avatar, "https://example.com/me.gif"))
        assertEquals(DomainError.ImageHostNotAllowed, store.state.error)
        assertEquals("https://i.giphy.com/a.gif" to false, store.state.avatar.url to store.state.avatar.changed)
    }

    @Test
    fun linksGitHubThroughAGistAndSavesTheLink() = runTest {
        store.accept(EditProfileStore.Intent.ChangeGitHubLogin("not a login"))
        store.accept(EditProfileStore.Intent.LinkGitHub)
        assertEquals(DomainError.InvalidGitHubLogin, store.state.error)

        store.accept(EditProfileStore.Intent.ChangeGitHubLogin("@yara"))
        store.accept(EditProfileStore.Intent.LinkGitHub)
        store.accept(EditProfileStore.Intent.ChangeGistLink("https://gist.github.com/yara/bad"))
        store.accept(EditProfileStore.Intent.VerifyGitHub)
        assertEquals(true, store.state.github.failed)

        store.accept(EditProfileStore.Intent.ChangeGistLink("https://gist.github.com/yara/good"))
        store.accept(EditProfileStore.Intent.VerifyGitHub)
        store.accept(EditProfileStore.Intent.UseGitHubAvatar)
        store.accept(EditProfileStore.Intent.Save)

        val saved = profiles.saved.single()
        assertEquals(GitHubAccount("yara", "good"), saved.github)
        assertEquals("https://avatars.githubusercontent.com/yara", saved.picture, "a verified GitHub link may set its avatar")
    }

    @Test
    fun checksTheNip05AfterAPause() = runTest {
        store.accept(EditProfileStore.Intent.ChangeField(Field.Nip05, "yara@nadim.dev"))
        advanceUntilIdle()
        assertEquals(Nip05Status.Verified, store.state.nip05Status)
        store.accept(EditProfileStore.Intent.ChangeField(Field.Nip05, ""))
        assertEquals(Nip05Status.None, store.state.nip05Status)
    }
}
