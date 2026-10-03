@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package friendly.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import friendly.query.InfiniteQuery
import friendly.query.InfiniteQueryState
import friendly.sdk.ActivityDetails
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

@Composable
fun ActivityScreen(
    vm: ActivityScreenViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val activityState by vm.activity.state.collectAsStateWithLifecycle()

    val lazyColumnState = rememberLazyListState()

    val isAppending = activityState.fetch is FetchingNext
    val isInitiallyLoading = activityState.fetch is Loading
    val isRefreshing = activityState.fetch is Refreshing

    vm.activity.setFetchNextOnScroll(activityState, lazyColumnState)

    PullToRefreshBox(
        isRefreshing = isRefreshing || isInitiallyLoading,
        state = pullToRefreshState,
        onRefresh = vm.activity::refresh,
        indicator = {
            LoadingIndicator(
                modifier = Modifier
                    .safeDrawingPadding()
                    .align(Alignment.TopCenter),
                isRefreshing = isRefreshing || isInitiallyLoading,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = pullToRefreshState,
            )
        },
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "activity")
                    },
                    navigationIcon = { },
                )
            },
            modifier = Modifier
                .fillMaxSize(),
        ) { innerPadding ->
            LazyColumn(
                state = lazyColumnState,
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            ) {
                item {
                    Spacer(Modifier.height(12.dp))

                    if (activityState.error) {
                        Text("some error occurred")
                    }
                }

                items(
                    items = activityState.items,
                    contentType = { "feed" },
                    key = { item -> item.id.long },
                ) { item ->
                    ActivityDetails(
                        details = item,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }

                item {
                    if (isAppending) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityDetails(details: ActivityDetails, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        when (details) {
            is ActivityDetails.Reply -> {
                Text(
                    text = details.post.text.string.take(50),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text =
                        "${details.post.owner.nickname.string} | ${details.post.instant}",
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
