package com.yet.talk.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * A header above swipeable lists (a profile above its tabs). It lives outside the pages, so swiping between them leaves
 * it in place, and it follows the list in view: it scrolls away with the list's first pixels, down to its pinned part,
 * and comes back as the list returns to its top. Following the list (rather than taking the scroll itself) means the
 * list's first item always sits right under it, with no gap. [offset] is how far it is moved up (0 to -range); read it
 * in a layout or draw block.
 */
@Stable
class CollapsingHeader internal constructor(private val list: () -> LazyListState) {
    var height by mutableIntStateOf(0)
        private set
    var range by mutableIntStateOf(0)
        private set

    fun setSize(height: Int, pinned: Int) {
        this.height = height
        range = (height - pinned).coerceAtLeast(0)
    }

    /** How far the header is collapsed, in pixels (0 to its range). */
    val collapsed: Int
        get() {
            val state = list()
            val scrolled = if (state.firstVisibleItemIndex == 0) state.firstVisibleItemScrollOffset else Int.MAX_VALUE
            return scrolled.coerceAtMost(range)
        }

    val offset: Float get() = -collapsed.toFloat()

    /** Dragging the header scrolls the list in view, so the header moves with it. */
    val scrollable: ScrollableState = ScrollableState { delta -> -list().dispatchRawDelta(-delta) }
}

@Composable
fun rememberCollapsingHeader(list: () -> LazyListState): CollapsingHeader {
    val latest by rememberUpdatedState(list)
    return remember { CollapsingHeader { latest() } }
}
