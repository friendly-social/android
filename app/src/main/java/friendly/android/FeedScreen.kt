package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

sealed interface FeedScreenUiState {
    data class NetworkError(val isRefreshing: Boolean) : FeedScreenUiState

    data class ServerError(val isRefreshing: Boolean) : FeedScreenUiState

    data class EmptyFeed(val isRefreshing: Boolean) : FeedScreenUiState

    data object Loading : FeedScreenUiState

    data class Idle(val currentFeedItems: List<FeedEntry>) : FeedScreenUiState
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun FeedScreen(vm: FeedScreenViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()
    val isRefreshing = (state as? FeedScreenUiState.EmptyFeed)
        ?.isRefreshing
        ?: false

    LaunchedEffect(Unit) {
        vm.loadInitial()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            when (val state = state) {
                is FeedScreenUiState.Idle -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        EmptyFeed(
                            isRefreshing = false,
                            onRefresh = vm::refresh,
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                        )

                        IndicatedCardFeed(
                            currentItems = state.currentFeedItems,
                            like = vm::like,
                            dislike = vm::dislike,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                is FeedScreenUiState.NetworkError -> {
                    NetworkError(
                        onRefresh = vm::refresh,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is FeedScreenUiState.EmptyFeed -> {
                    EmptyFeed(
                        isRefreshing = isRefreshing,
                        onRefresh = vm::refresh,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                    )
                }

                is FeedScreenUiState.ServerError -> {
                    ServerError(
                        onRefresh = vm::refresh,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is FeedScreenUiState.Loading -> {
                    LoadingState()
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LoadingState() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        ContainedLoadingIndicator(Modifier.size(64.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyFeed(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = pullToRefreshState,
            )
        },
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_inbox),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.you_re_all_caught_up),
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.add_more_friends_feed_text),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ServerError(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = false,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Server error")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NetworkError(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = false,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Network error")
        }
    }
}
