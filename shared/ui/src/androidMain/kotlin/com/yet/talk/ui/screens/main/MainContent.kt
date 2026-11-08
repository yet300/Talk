@file:OptIn(ExperimentalDecomposeApi::class)

package com.yet.talk.ui.screens.main

import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.ui.theme.talkColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import com.yet.talk.ui.navigation.Page
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.panels.ChildPanels
import com.arkivanov.decompose.extensions.compose.experimental.panels.ChildPanelsAnimators
import com.arkivanov.decompose.extensions.compose.experimental.panels.HorizontalChildPanelsLayout
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.PredictiveBackParams
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.materialPredictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.ui.components.LocalReaderMode
import com.yet.talk.ui.components.LocalSelectedPost
import com.yet.talk.ui.components.ReaderMode
import com.yet.talk.ui.navigation.pushAnimator
import com.yet.talk.ui.navigation.swapAnimator
import com.yet.talk.ui.screens.main.component.DetailsContent
import com.yet.talk.ui.screens.main.component.DetailsPlaceholder
import com.yet.talk.ui.screens.main.component.ReadingPane
import com.yet.talk.ui.screens.main.component.SideColumn
import com.yet.talk.ui.screens.main.component.TabsContent
import com.yet.talk.ui.theme.reducedMotion

/**
 * Main as list-detail panels (`ChildPanels`, section 7 of the UI review). The window picks the mode: one pane on
 * phones, the Duo outer screen and the Duo inner screen upright (the details panel covers the tabs and goes back with
 * the predictive back gesture); two panes on wide windows (the Duo inner screen sideways, tablets in landscape),
 * split on the fold as 415 | 476, with the side column on the camera side. In two panes Reply swaps the left pane to
 * the post's replies (decision 6).
 */
@Composable
fun MainContent(component: MainComponent, onOpenSettings: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panels by component.panels.subscribeAsState()
        val isReading by component.isReading.subscribeAsState()
        val tabsStack by panels.main.instance.stack.subscribeAsState()
        // A wide window keeps the large-screen chrome (the side column, no tab bar) whatever the panels show.
        val wide = panelsMode(maxWidth, maxHeight) == ChildPanelsMode.DUAL
        // Chats splits itself (the list | the conversation), so it takes the whole width instead of leaving the
        // right pane empty beside a cramped list.
        val fullWidth = tabsStack.active.instance.tab == TabsComponent.Tab.Chats
        val mode = if (wide && !fullWidth) ChildPanelsMode.DUAL else ChildPanelsMode.SINGLE
        LaunchedEffect(component, mode) { component.onModeChanged(mode) }
        val dual = panels.mode != ChildPanelsMode.SINGLE
        val details = panels.details?.instance
        val selected = (details?.link as? MainComponent.Link.Post)?.id
        val reduced = reducedMotion
        val animators = remember(reduced) { ChildPanelsAnimators<Any, Any, Any, Any, Nothing, Nothing>(single = pushAnimator(reduced), dual = swapAnimator() to swapAnimator()) }
        val layout = remember { HorizontalChildPanelsLayout(dualWeights = 415f to 476f) }
        val readerMode = remember(dual, component) { ReaderMode(twoPane = dual, onShowReplies = component::onShowReplies) }
        CompositionLocalProvider(LocalSelectedPost provides selected.takeIf { dual }, LocalReaderMode provides readerMode) {
            Row(Modifier.fillMaxSize()) {
                if (wide) SideColumn(panels.main.instance, details, isReading = isReading, onBack = { details?.onBack() }, onTab = component::onTabSelected)
                ChildPanels(
                    panels = component.panels,
                    modifier = Modifier.weight(1f),
                    layout = layout,
                    animators = animators,
                    mainChild = { child -> Page {
                        val post = details?.activePost()
                        if (dual && isReading && post != null) ReadingPane(post) else TabsContent(child.instance, onOpenSettings, phone = !wide)
                    }},
                    detailsChild = {
                        Page {
                            Row {
                                // The fold (or the split on a tablet) between the two panes.
                                if (dual) VerticalDivider(color = MaterialTheme.talkColors.border)
                                DetailsContent(it.instance)
                            }
                        }
                    },
                    secondPanelPlaceholder = {
                        Page {
                            Row {
                                VerticalDivider(color = MaterialTheme.talkColors.border)
                                DetailsPlaceholder()
                            }
                        }
                    },
                    predictiveBackParams = { state ->
                        val open = state.details?.instance
                        if (state.mode == ChildPanelsMode.SINGLE && open != null) {
                            PredictiveBackParams(open.backHandler, open::onBack, animatable = ::materialPredictiveBackAnimatable)
                        } else {
                            null
                        }
                    },
                )
            }
        }
    }
}

/**
 * Two panes in an expanded window (840 dp wide) that is not short: the Duo's inner screen sideways (951 × 669) and
 * tablets. A phone held sideways is wide but short (under 480 dp), and stays one pane, as AGENTS.md has it.
 */
private fun panelsMode(width: Dp, height: Dp): ChildPanelsMode =
    if (width >= 840.dp && height >= 480.dp) ChildPanelsMode.DUAL else ChildPanelsMode.SINGLE

@Composable
private fun DetailsComponent.activePost() = (stack.subscribeAsState().value.active.instance as? DetailsComponent.Child.Post)?.component
