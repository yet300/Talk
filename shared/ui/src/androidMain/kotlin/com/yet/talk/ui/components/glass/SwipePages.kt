package com.yet.talk.ui.components.glass

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yet.talk.ui.navigation.Page
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.pages.PagesScrollAnimation
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.extensions.compose.subscribeAsState

/** The pager behind [SwipePages], for a [GlassSegments] pill that follows the swipe. */
@Stable
class PagesPosition {
    internal var state by mutableStateOf<PagerState?>(null)

    /** The page shown, with the fraction it is swiped by; read it in a layout or draw block. */
    fun value(): Float? = state?.let { it.currentPage + it.currentPageOffsetFraction }
}

@Composable
fun rememberPagesPosition() = remember { PagesPosition() }

/**
 * Sibling pages to swipe between (`ChildPages`, a horizontal pager): a tab tap scrolls to its page, a swipe selects
 * it. Each page keeps its saveable state (a list's scroll position) while another is shown.
 */
@Composable
fun <C : Any, T : Any> SwipePages(
    pages: Value<ChildPages<C, T>>,
    onPageSelected: (Int) -> Unit,
    position: PagesPosition,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val saved = rememberSaveableStateHolder()
    val state by pages.subscribeAsState()
    ChildPages(
        pages = state,
        onPageSelected = onPageSelected,
        modifier = modifier,
        scrollAnimation = PagesScrollAnimation.Default,
        pager = { pagerModifier, pagerState, key, pageContent ->
            SideEffect { position.state = pagerState }
            HorizontalPager(state = pagerState, modifier = pagerModifier, key = key, pageContent = pageContent)
        },
    ) { index, page ->
        saved.SaveableStateProvider(state.items[index].configuration) { Page { content(page) } }
    }
}
