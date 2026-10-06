package friendly.android

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import friendly.query.InfiniteQuery
import friendly.query.InfiniteQueryState
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun InfiniteQuery<*, *>.setFetchNextOnScroll(
    queryState: InfiniteQueryState<*>,
    lazyListState: LazyListState,
    remainingItemsBeforeLoadingNextPage: Int = 100,
) {
    val query = this
    LaunchedEffect(lazyListState, queryState.hasNext) {
        val flow = snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .filterNotNull()
            .filter { lastVisibleItemIndex ->
                val thresholdIndex =
                    queryState.items.size - remainingItemsBeforeLoadingNextPage
                lastVisibleItemIndex >= thresholdIndex
            }
            .collect {
                if (queryState.hasNext) {
                    query.fetchNext()
                }
            }
    }
}
