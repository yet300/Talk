package com.yet.talk.ui.components.list

import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.SkeletonCards
import com.yet.talk.ui.components.Spinner
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * A paged list (`PagedList`): pull to refresh, the next page as the end scrolls into view, skeleton cards on the
 * first load, [empty] when there is nothing, "You have reached the end", and Retry when a page fails. Items are keyed
 * and move into place when the list changes (`animateItem`). [header] scrolls with the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> PagedList(
    items: LazyPagingItems<T>,
    key: (T) -> Any,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentType: (T) -> Any? = { null },
    refreshIndicatorOffset: Dp = contentPadding.calculateTopPadding(),
    header: LazyListScope.() -> Unit = {},
    empty: @Composable () -> Unit,
    item: @Composable (T) -> Unit,
) {
    val refreshing = items.loadState.refresh is LoadState.Loading
    // The spinner answers a pull only: the feeds also reload by themselves (relays, the outbox), and a spinner over
    // the content for those would look stuck.
    var pulled by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) { if (!refreshing) pulled = false }
    val pull = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = pulled,
        onRefresh = {
            pulled = true
            items.refresh()
        },
        modifier = modifier.fillMaxSize(),
        state = pull,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pull,
                isRefreshing = pulled,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = refreshIndicatorOffset),
                color = MaterialTheme.talkColors.accent,
            )
        },
    ) {
        LazyColumn(state = state, contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            header()
            items(count = items.itemCount, key = items.itemKey(key), contentType = items.itemContentType(contentType)) { index ->
                Box(Modifier.animateItem()) { items[index]?.let { item(it) } }
            }
            footer(items, refreshing)?.let { footer ->
                item(key = footer, contentType = Footer::class) {
                    Box(Modifier.animateItem()) {
                        when (footer) {
                            Footer.Loading -> SkeletonCards()
                            Footer.Empty -> empty()
                            Footer.Retry -> RetryRow { items.retry() }
                            Footer.More -> Spinner()
                            Footer.End -> End()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun End(modifier: Modifier = Modifier) {
    Text(
        stringResource(MR.strings.feed_end),
        style = MaterialTheme.talkTypography.small,
        color = MaterialTheme.talkColors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(MaterialTheme.spacing.xl),
    )
}

@Composable
private fun RetryRow(onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg), contentAlignment = Alignment.Center) {
        TalkButton(stringResource(MR.strings.action_retry), onRetry, ButtonStyle.Secondary)
    }
}

/** What follows a paged list's items: its first load, its empty state, a retry, the next page loading, or its end. */
private enum class Footer { Loading, Empty, Retry, More, End }

private fun footer(items: LazyPagingItems<*>, refreshing: Boolean): Footer? {
    val append = items.loadState.append
    return when {
        items.itemCount == 0 && refreshing -> Footer.Loading
        items.itemCount == 0 && items.loadState.refresh is LoadState.Error -> Footer.Retry
        items.itemCount == 0 -> Footer.Empty
        append is LoadState.Loading -> Footer.More
        append is LoadState.Error -> Footer.Retry
        append.endOfPaginationReached -> Footer.End
        else -> null
    }
}
