package com.yet.talk.feature.root

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.PreferencesRepository
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.feature.auth.PreviewAuthComponent
import com.yet.talk.feature.compose.PreviewComposeComponent
import com.yet.talk.feature.post.component.revisions.PreviewRevisionsComponent
import com.yet.talk.feature.root.store.RootStoreFactory
import com.yet.talk.feature.settings.PreviewSettingsComponent
import com.yet.talk.feature.post.component.cards.PreviewPostCardsComponent
import com.yet.talk.feature.editprofile.PreviewEditProfileComponent
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
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
        override suspend fun generateKey() = error("unused")
        override suspend fun importKey(secretKey: String) = error("unused")
        override suspend fun exportSecretKey() = error("unused")
        override suspend fun signOut() = Unit
    }

    private object Preferences : PreferencesRepository {
        override val theme = MutableStateFlow(ThemeMode.System)
        override suspend fun setTheme(mode: ThemeMode) {
            theme.value = mode
        }
    }

    /** Records what the root asks of Main; its panels are never rendered here. */
    private class FakeMain(val link: MainComponent.Link?, val output: (MainComponent.Output) -> Unit) :
        MainComponent, WebNavigationOwner.NoOp {
        val opened = mutableListOf<PostId>()
        override val panels: Value<ChildPanels<*, TabsComponent, *, DetailsComponent, Nothing, Nothing>>
            get() = error("unused")
        override val isReading: Value<Boolean> = MutableValue(false)
        override fun onModeChanged(mode: ChildPanelsMode) = Unit
        override fun onShowReplies() = Unit
        override fun openPost(id: PostId) {
            opened += id
        }
        override fun openProfile(id: UserId) = Unit
        val tabs = mutableListOf<TabsComponent.Tab>()
        override fun onTabSelected(tab: TabsComponent.Tab) {
            tabs += tab
        }
    }

    private val accounts = Accounts()
    private var main: FakeMain? = null
    private var deepLink: String? = null

    private val root by lazy {
        val factories = DestinationFactories(
            auth = { _ -> PreviewAuthComponent() },
            main = { _, link, output -> FakeMain(link, output).also { main = it } },
            revisions = { _, _, _ -> PreviewRevisionsComponent() },
            compose = { _, _, _ -> PreviewComposeComponent() },
            settings = { _, _ -> PreviewSettingsComponent() },
            editProfile = { _, _ -> PreviewEditProfileComponent() },
        )
        val lifecycle = LifecycleRegistry().apply { resume() }
        DefaultRootComponent(DefaultComponentContext(lifecycle), RootStoreFactory(DefaultStoreFactory(), accounts, Preferences), factories, deepLink, PreviewPostCardsComponent())
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

        main!!.output(MainComponent.Output.OpenSettings)
        assertIs<RootComponent.Child.Settings>(active)
        root.onBack()
        assertIs<RootComponent.Child.Main>(active)

        root.onOpenSettings()
        root.onNewPost()
        root.onTabSelected(TabsComponent.Tab.Bookmarks)
        assertIs<RootComponent.Child.Main>(active)
        assertEquals(listOf(TabsComponent.Tab.Bookmarks), main!!.tabs)

        main!!.output(MainComponent.Output.NewPost)
        accounts.account.value = null
        assertIs<RootComponent.Child.Auth>(active)
        assertEquals(1, root.stack.value.items.size, "nothing from the signed-in session stays behind")
    }

    @Test
    fun theThemeFollowsThePreference() {
        assertEquals(ThemeMode.System, root.theme.value)
        Preferences.theme.value = ThemeMode.Dark
        assertEquals(ThemeMode.Dark, root.theme.value)
        Preferences.theme.value = ThemeMode.System
    }

    @Test
    fun aTabsLinkSelectsThatTab() {
        deepLink = "http://localhost:8080/bookmarks"
        accounts.account.value = Account(UserId("me"), "npub1me")

        assertIs<RootComponent.Child.Main>(active)
        assertEquals(listOf(TabsComponent.Tab.Bookmarks), main!!.tabs)
        assertEquals(null, main!!.link)
    }

    @Test
    fun aSharedLinkOpensInMainOnceSignedIn() {
        deepLink = "https://talk.example/post/abc/revisions?x=1"
        accounts.account.value = Account(UserId("me"), "npub1me")

        val items = root.stack.value.items.map { it.instance }
        assertIs<RootComponent.Child.Main>(items[0])
        assertIs<RootComponent.Child.Revisions>(items[1])
        assertEquals(MainComponent.Link.Post(PostId("abc")), main!!.link)

        accounts.account.value = null
        accounts.account.value = Account(UserId("me"), "npub1me")
        assertEquals(null, main!!.link, "the link is opened once")
        assertEquals(1, root.stack.value.items.size)
    }
}
