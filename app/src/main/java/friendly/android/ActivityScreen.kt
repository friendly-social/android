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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import friendly.sdk.ActivityDetails

@Composable
fun ActivityScreen(
    vm: ActivityScreenViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val lazyPagingItems = vm.items.collectAsLazyPagingItems()

    val mediatorRefresh = lazyPagingItems.loadState.mediator?.refresh
    val mediatorAppend = lazyPagingItems.loadState.mediator?.append

    val isAppending = mediatorAppend is LoadState.Loading

    val isRefreshing = mediatorRefresh is LoadState.Loading

    LaunchedEffect(mediatorRefresh) {
        if (mediatorRefresh is LoadState.Error &&
            lazyPagingItems.itemCount > 0
        ) {
            println("can't refresh…")
        } else {
            println("can refresh!")
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = lazyPagingItems::refresh,
        indicator = {
            LoadingIndicator(
                modifier = Modifier
                    .safeDrawingPadding()
                    .align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
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
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            ) {
                item {
                    Spacer(Modifier.height(12.dp))

                    val isError = lazyPagingItems.itemCount == 0 &&
                        mediatorRefresh is LoadState.Error

                    if (isError) {
                        Text("some error occurred")
                    }
                }

                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id.long },
                ) { index ->
                    val pagingItem = lazyPagingItems.get(index)

                    if (pagingItem != null) {
                        ActivityDetails(
                            details = pagingItem,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
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
