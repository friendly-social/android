package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import friendly.android.UserAvatarStyle.UserAvatarShape
import friendly.sdk.ActivityDetails
import friendly.sdk.Nickname
import friendly.sdk.UserId

sealed interface ActivityScreenUiState {
    data object Loading : ActivityScreenUiState

    data class Idle(
        val activity: List<ActivityDetails>,
        val isRefreshing: Boolean,
    ) : ActivityScreenUiState

    data class NetworkError(
        val isRefreshing: Boolean,
    ) : ActivityScreenUiState
}

private val ActivityScreenUiState.isRefreshing
    get() = when (this) {
        is Loading -> false
        is Idle -> isRefreshing
        is NetworkError -> isRefreshing
    }

@Composable
fun ActivityScreen(
    vm: ActivityScreenViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { vm.load() }

    val state by vm.state.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        state = pullToRefreshState,
        onRefresh = vm::refresh,
        indicator = {
            LoadingIndicator(
                modifier = Modifier
                    .safeDrawingPadding()
                    .align(Alignment.TopCenter),
                isRefreshing = state.isRefreshing,
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
                    title = {},
                    navigationIcon = {
                        // todo
                        IconButton(onClick = {}) {
                            UserAvatar(
                                // todo this thing
                                nickname = Nickname.orThrow("pai y"),
                                userId = UserId(21),
                                uri = null,
                                style = UserAvatarStyle(
                                    size = 24.dp,
                                    noAvatarSize = 14.dp,
                                    shape = UserAvatarShape.Certain(CircleShape),
                                ),
                                modifier = Modifier,
                            )
                        }
                    },
                )
            },
            modifier = modifier
                .padding(contentPadding)
                .fillMaxSize(),
        ) { innerPadding ->
            when (val state = state) {
                is Idle -> {
                    IdleState(
                        state = state,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }

                is Loading -> {
                    Box(
                        contentAlignment = Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is NetworkError -> {
                    NetworkErrorBox(
                        onRetry = {},
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleState(
    state: ActivityScreenUiState.Idle,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.activity) { item ->
                ActivityDetails(
                    details = item,
                    modifier = Modifier,
                )
            }
        }
    }
}

@Composable
fun ActivityDetails(
    details: ActivityDetails,
    modifier: Modifier = Modifier,
) {
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
                    text = "${details.post.owner.nickname.string} | ${details.post.instant}",
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
