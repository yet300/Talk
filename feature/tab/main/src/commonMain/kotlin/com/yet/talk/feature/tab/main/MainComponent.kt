package com.yet.talk.feature.tab.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent

/**
 * Everything below the root once signed in, as list-detail panels: the tabs in the main panel, a post or
 * profile in the details panel. The UI picks the [ChildPanelsMode] from the window: single pane on phones,
 * the Duo outer screen and the web; two panes on the Duo inner screen sideways (list | article).
 */
interface MainComponent : WebNavigationOwner {
    val panels: Value<ChildPanels<*, TabsComponent, *, DetailsComponent, Nothing, Nothing>>

    /**
     * Two panes only: the left pane shows the open post's replies instead of the tabs ("reader"), without
     * touching the article on the right. Back leaves it.
     */
    val isReading: Value<Boolean>

    fun onModeChanged(mode: ChildPanelsMode)

    /** Reply or the reply count in the article, in two panes. */
    fun onShowReplies()

    /** A navigation item (rail, bottom bar, side column): closes the details panel and shows the tab. */
    fun onTabSelected(tab: TabsComponent.Tab)

    /** Opens a post in the details panel, replacing what it showed (used after publishing). */
    fun openPost(id: PostId)

    fun openProfile(id: UserId)

    /** What a link opens in the details panel. */
    sealed interface Link {
        data class Post(val id: PostId) : Link

        data class Profile(val id: UserId) : Link
    }

    /** Root destinations: the editor, settings and revisions open above everything. */
    sealed interface Output {
        data class Reply(val parent: PostId) : Output

        data class Quote(val quoted: PostId) : Output

        data class Edit(val id: PostId) : Output

        data class OpenRevisions(val id: PostId) : Output

        data object NewPost : Output

        data object OpenSettings : Output
    }

    fun interface Factory {
        /** [link] opens in the details panel at once (a shared link). */
        operator fun invoke(componentContext: ComponentContext, link: Link?, output: (Output) -> Unit): MainComponent
    }
}
