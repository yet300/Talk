package com.yet.talk.feature.tab.main

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.feature.tab.home.PreviewHomeComponent
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MainComponentTest {

    private class FakeTabs(val output: (TabsComponent.Output) -> Unit) : TabsComponent, WebNavigationOwner.NoOp {
        override val stack: Value<ChildStack<*, TabsComponent.Child>> = MutableValue(
            ChildStack(configuration = "home", instance = TabsComponent.Child.Home(PreviewHomeComponent()) as TabsComponent.Child),
        )
        override fun onTabClicked(tab: TabsComponent.Tab) = Unit
    }

    private class FakeDetails(override val link: MainComponent.Link, val output: (DetailsComponent.Output) -> Unit) :
        DetailsComponent, WebNavigationOwner.NoOp {
        override val stack: Value<ChildStack<*, DetailsComponent.Child>> get() = error("unused")
        override fun onBack() = output(DetailsComponent.Output.Closed)
        override fun open(link: MainComponent.Link) = Unit
    }

    private val backDispatcher = BackDispatcher()
    private val outputs = mutableListOf<MainComponent.Output>()
    private var tabs: FakeTabs? = null

    private fun main(link: MainComponent.Link? = null): MainComponent {
        val lifecycle = LifecycleRegistry().apply { resume() }
        return DefaultMainComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle, backHandler = backDispatcher),
            link = link,
            output = { outputs += it },
            tabsFactory = { _, output -> FakeTabs(output).also { tabs = it } },
            detailsFactory = { _, link, output -> FakeDetails(link, output) },
        )
    }

    private val MainComponent.details get() = panels.value.details?.instance

    @Test
    fun aPostFromTheTabsOpensInTheDetailsPanelAndBackClosesIt() {
        val main = main()
        tabs!!.output(TabsComponent.Output.OpenPost(PostId("a")))
        assertEquals(MainComponent.Link.Post(PostId("a")), main.details?.link)

        tabs!!.output(TabsComponent.Output.OpenPost(PostId("b")))
        assertEquals(MainComponent.Link.Post(PostId("b")), main.details?.link, "another card replaces the article")

        assertTrue(backDispatcher.back())
        assertNull(main.details)
    }

    @Test
    fun aSharedLinkStartsWithTheDetailsOpen() {
        assertEquals(MainComponent.Link.Post(PostId("x")), main(MainComponent.Link.Post(PostId("x"))).details?.link)
    }

    @Test
    fun repliesTakeTheLeftPaneOnlyInTwoPanesAndBackLeavesThem() {
        val main = main(MainComponent.Link.Post(PostId("a")))
        main.onShowReplies()
        assertFalse(main.isReading.value, "a single pane shows the replies under the article")

        main.onModeChanged(ChildPanelsMode.DUAL)
        main.onShowReplies()
        assertTrue(main.isReading.value)

        assertTrue(backDispatcher.back())
        assertFalse(main.isReading.value)
        assertEquals(MainComponent.Link.Post(PostId("a")), main.details?.link, "back left the replies, not the article")

        main.onShowReplies()
        main.onModeChanged(ChildPanelsMode.SINGLE)
        assertFalse(main.isReading.value, "folding to one pane leaves the replies pose")
    }

    @Test
    fun editorDestinationsGoToTheRoot() {
        main()
        tabs!!.output(TabsComponent.Output.NewPost)
        assertEquals(listOf<MainComponent.Output>(MainComponent.Output.NewPost), outputs)
    }
}
