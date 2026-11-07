package com.yet.talk.ui.components.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * A paged list (`PagedList`): pull to refresh, the next page as the end scrolls into view, a spinner while loading,
 * [empty] when there is nothing, "You have reached the end", and Retry when a page fails. [header] scrolls with it.
 */
@Composable
fun <T : Any> PagedList(
    items: LazyPagingItems<T>,
    key: (T) -> Any,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    header: LazyListScope.() -> Unit = {},
    empty: @Composable () -> Unit,
    item: @Composable (T) -> Unit,
) {
    val refreshing = items.loadState.refresh is LoadState.Loading
    PullToRefreshBox(isRefreshing = refreshing && items.itemCount > 0, onRefresh = items::refresh, modifier = modifier.fillMaxSize()) {
        LazyColumn(state = state, contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            header()
            items(count = items.itemCount, key = items.itemKey(key)) { index -> items[index]?.let { item(it) } }
            val append = items.loadState.append
            when {
                items.itemCount == 0 && refreshing -> item { Spinner() }
                items.itemCount == 0 && items.loadState.refresh is LoadState.Error -> item { RetryRow { items.retry() } }
                items.itemCount == 0 -> item { empty() }
                append is LoadState.Loading -> item { Spinner() }
                append is LoadState.Error -> item { RetryRow { items.retry() } }
                append.endOfPaginationReached -> item { End() }
            }
        }
    }
}

@Composable
private fun Spinner() {
    Box(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.xl), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.talkColors.accent, strokeWidth = 3.dp)
    }
}

@Composable
private fun End() {
    Text(
        stringResource(MR.strings.feed_end),
        style = MaterialTheme.talkTypography.small,
        color = MaterialTheme.talkColors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.xl),
    )
}

@Composable
private fun RetryRow(onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg), contentAlignment = Alignment.Center) {
        TalkButton(stringResource(MR.strings.action_retry), onRetry, ButtonStyle.Secondary)
    }
}

/** The design's empty state (`x-empty`): title, body, one action. */
@Composable
fun EmptyState(title: String, body: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.xxl, vertical = MaterialTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Text(title, style = MaterialTheme.talkTypography.headingLarge, color = MaterialTheme.talkColors.textPrimary, textAlign = TextAlign.Center)
        if (body != null) Text(body, style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.textSecondary, textAlign = TextAlign.Center)
        action?.invoke()
    }
}
