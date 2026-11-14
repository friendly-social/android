package friendly.android

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import friendly.query.InfiniteQuery
import friendly.query.InfiniteQueryState
import kotlinx.coroutines.flow.filter

@SuppressLint("ComposableNaming")
@Composable
fun InfiniteQuery<*, *>.setFetchNextOnScroll(
    queryState: InfiniteQueryState<*>,
    lazyListState: LazyListState,
    remainingItemsThreshold: Int = 100,
) {
    val query = this
    LaunchedEffect(lazyListState, queryState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .filter { visibleItemsInfo ->
                val lastVisibleItemIndex = visibleItemsInfo
                    .lastOrNull()?.index
                    ?: return@filter false
                val totalSize = queryState.items.size
                remainingItemsThreshold >= (totalSize - lastVisibleItemIndex)
            }
            .collect {
                if (queryState.hasNext) {
                    query.fetchNext()
                }
            }
    }
}
