package friendly.android

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
    val isRefreshing = isRefreshing(state)
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        vm.loadInitial()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = vm::refresh,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier
                    .safeDrawingPadding()
                    .align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = pullToRefreshState,
            )
        },
        modifier = modifier,
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                ScaffoldContent(vm, state)
            }
        }
    }
}

@Composable
private fun ScaffoldContent(
    vm: FeedScreenViewModel,
    state: FeedScreenUiState,
) {
    AnimatedContent(
        targetState = state,
    ) { state ->
        when (val state = state) {
            is FeedScreenUiState.Idle -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    EmptyFeed(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                    onRetry = vm::retry,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }

            is FeedScreenUiState.EmptyFeed -> {
                EmptyFeed(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }

            is FeedScreenUiState.ServerError -> {
                ServerError(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }

            is FeedScreenUiState.Loading -> {
                LoadingState()
            }
        }
    }
}

private fun isRefreshing(state: FeedScreenUiState): Boolean = when (state) {
    is FeedScreenUiState.EmptyFeed -> state.isRefreshing
    is FeedScreenUiState.Idle -> false
    is FeedScreenUiState.Loading -> false
    is FeedScreenUiState.NetworkError -> state.isRefreshing
    is FeedScreenUiState.ServerError -> state.isRefreshing
}

@Composable
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
private fun EmptyFeed(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_inbox),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.you_re_all_caught_up),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.add_more_friends_feed_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ServerError(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Server error")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NetworkError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    NetworkErrorBox(
        onRetry = onRetry,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    )
}
