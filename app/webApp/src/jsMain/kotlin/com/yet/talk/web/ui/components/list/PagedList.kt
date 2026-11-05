package com.yet.talk.web.ui.components.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.paging.LoadState
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.web.browser.IntersectionObserver
import com.yet.talk.web.paging.PagingItems
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.SkeletonCards
import org.jetbrains.compose.web.dom.Div

/**
 * A paged list: skeleton cards on the first load, [empty] when there is nothing, an error row with Retry,
 * and the next page when the end of the list scrolls into view.
 */
@Composable
fun <T : Any> PagedList(
    items: PagingItems<T>,
    empty: @Composable () -> Unit,
    item: @Composable (T) -> Unit,
) {
    val refresh = items.loadState.refresh
    when {
        items.itemCount == 0 && refresh is LoadState.Loading -> SkeletonCards()
        items.itemCount == 0 && refresh is LoadState.NotLoading -> empty()
    }
    for (index in 0 until items.itemCount) {
        items.peek(index)?.let { item(it) }
    }
    val error = (refresh as? LoadState.Error ?: items.loadState.append as? LoadState.Error)?.error
    if (error != null) {
        ErrorRow(error as? DomainError ?: DomainError.Network(error), onDismiss = null, onRetry = items::retry)
    }
    if (items.itemCount > 0) {
        if (items.loadState.append is LoadState.Loading) SkeletonCards(count = 1)
        EndSentinel { items[items.itemCount - 1] }
    }
}

/** Calls [onVisible] whenever this marker under the list scrolls into view. */
@Composable
private fun EndSentinel(onVisible: () -> Unit) {
    val callback = rememberUpdatedState(onVisible)
    Div({
        classes("t-end-sentinel")
        attr("aria-hidden", "true")
        ref { element ->
            val observer = IntersectionObserver { entries, _ -> if (entries.any { it.isIntersecting }) callback.value() }
            observer.observe(element)
            onDispose { observer.disconnect() }
        }
    })
}
