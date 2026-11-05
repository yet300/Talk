package com.yet.talk.feature.tab.main.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.tab.activity.ActivityComponent
import com.yet.talk.feature.tab.bookmarks.BookmarksComponent
import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.feature.tab.search.SearchComponent

/**
 * The tabs (bottom bar, web rail, Duo side column): the main panel of [com.yet.talk.feature.tab.main.MainComponent].
 * Each tab keeps its state while another one is shown. On the web the active tab is the URL
 * (`/`, `/search`, `/activity`, `/bookmarks`, `/me`) without adding history entries.
 */
interface TabsComponent : WebNavigationOwner {
    val stack: Value<ChildStack<*, Child>>

    /** The signed-in user, for the rail's identity row and the phone header avatar. */
    val me: Value<Me>

    fun onTabClicked(tab: Tab)

    data class Me(val user: User? = null, val npub: String? = null, val relayCount: Int = 0)

    /** [path] is the tab's web URL (`/search`…); Home is `/`. */
    enum class Tab(val path: String?) { Home(null), Search("search"), Activity("activity"), Bookmarks("bookmarks"), Profile("me") }

    sealed class Child(val tab: Tab) {
        class Home(val component: HomeComponent) : Child(Tab.Home)

        class Search(val component: SearchComponent) : Child(Tab.Search)

        class Activity(val component: ActivityComponent) : Child(Tab.Activity)

        class Bookmarks(val component: BookmarksComponent) : Child(Tab.Bookmarks)

        class Profile(val component: ProfileComponent) : Child(Tab.Profile)
    }

    /** Where the tabs lead: a post or profile in the details panel, or a root destination. */
    sealed interface Output {
        data class OpenPost(val id: PostId) : Output

        data class OpenProfile(val id: UserId) : Output

        data class Reply(val parent: PostId) : Output

        data class Quote(val quoted: PostId) : Output

        data object NewPost : Output

        data object OpenSettings : Output

        data class OpenRevisions(val id: PostId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): TabsComponent
    }
}
