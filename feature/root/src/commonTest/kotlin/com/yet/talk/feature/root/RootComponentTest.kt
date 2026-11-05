package com.yet.talk.feature.root

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.feature.auth.PreviewAuthComponent
import com.yet.talk.feature.compose.PreviewComposeComponent
import com.yet.talk.feature.post.component.post.PreviewPostComponent
import com.yet.talk.feature.post.component.revisions.PreviewRevisionsComponent
import com.yet.talk.feature.profile.PreviewProfileComponent
import com.yet.talk.feature.root.store.RootStoreFactory
import com.yet.talk.feature.settings.PreviewSettingsComponent
import com.yet.talk.feature.tab.home.PreviewHomeComponent
import com.yet.talk.feature.tab.main.MainComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RootComponentTest {

    private class Accounts : AccountRepository {
        override val account = MutableStateFlow<Account?>(null)
        override suspend fun createAccount() = error("unused")
        override suspend fun importKey(secretKey: String) = error("unused")
        override suspend fun exportSecretKey() = error("unused")
        override suspend fun signOut() = Unit
    }

    private class FakeMain(val output: (MainComponent.Output) -> Unit) : MainComponent, WebNavigationOwner.NoOp {
        override val stack = MutableValue(
            ChildStack(configuration = MainComponent.Tab.Home, instance = MainComponent.Child.Home(PreviewHomeComponent()) as MainComponent.Child),
        )
        override fun onTabClicked(tab: MainComponent.Tab) = Unit
    }

    private val accounts = Accounts()
    private var main: FakeMain? = null

    private var deepLink: String? = null

    private val root by lazy {
        val factories = DestinationFactories(
            auth = { _ -> PreviewAuthComponent() },
            main = { _, output -> FakeMain(output).also { main = it } },
            post = { _, _, _ -> PreviewPostComponent() },
            revisions = { _, _, _ -> PreviewRevisionsComponent() },
            compose = { _, _, _ -> PreviewComposeComponent() },
            profile = { _, _, _ -> PreviewProfileComponent() },
            settings = { _, _ -> PreviewSettingsComponent() },
        )
        val lifecycle = LifecycleRegistry().apply { resume() }
        DefaultRootComponent(DefaultComponentContext(lifecycle), RootStoreFactory(DefaultStoreFactory(), accounts), factories, deepLink)
    }

    private val active get() = root.stack.value.active.instance

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun signingInAndOutSwapsTheWholeStack() {
        assertIs<RootComponent.Child.Auth>(active)

        accounts.account.value = Account(UserId("me"), "npub1me")
        assertIs<RootComponent.Child.Main>(active)

        main!!.output(MainComponent.Output.OpenPost(PostId("p")))
        assertIs<RootComponent.Child.Post>(active)
        root.onBack()
        assertIs<RootComponent.Child.Main>(active)

        main!!.output(MainComponent.Output.OpenPost(PostId("p")))
        accounts.account.value = null
        assertIs<RootComponent.Child.Auth>(active)
        assertEquals(1, root.stack.value.items.size, "nothing from the signed-in session stays behind")
    }

    @Test
    fun aSharedLinkOpensAboveMainOnceSignedIn() {
        deepLink = "https://talk.example/post/abc/revisions?x=1"
        accounts.account.value = Account(UserId("me"), "npub1me")

        val items = root.stack.value.items.map { it.instance }
        assertIs<RootComponent.Child.Main>(items[0])
        assertIs<RootComponent.Child.Post>(items[1])
        assertIs<RootComponent.Child.Revisions>(items[2])

        accounts.account.value = null
        accounts.account.value = Account(UserId("me"), "npub1me")
        assertEquals(1, root.stack.value.items.size, "the link is opened once")
    }
}
