package friendly.android

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
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
import friendly.android.NetworkScreenUiState.Success.FriendItem
import friendly.sdk.Nickname
import friendly.sdk.UserAccessHash
import friendly.sdk.UserId

sealed interface NetworkScreenUiState {
    val isRefreshing: Boolean

    data class AuthFailure(override val isRefreshing: Boolean = false) :
        NetworkScreenUiState

    data class NetworkFailure(override val isRefreshing: Boolean = false) :
        NetworkScreenUiState

    data class Other(override val isRefreshing: Boolean = false) :
        NetworkScreenUiState

    data class Loading(override val isRefreshing: Boolean = false) :
        NetworkScreenUiState

    data class NoFriends(override val isRefreshing: Boolean = false) :
        NetworkScreenUiState

    data class Success(
        val friends: List<FriendItem>,
        override val isRefreshing: Boolean = false,
    ) : NetworkScreenUiState {
        data class FriendItem(
            val avatar: Uri?,
            val nickname: Nickname,
            val id: UserId,
            val accessHash: UserAccessHash,
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun NetworkScreen(
    vm: NetworkScreenViewModel,
    onProfile: (UserId, UserAccessHash) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        Log.d("navigation", "network screen displayed")
        vm.initialize()
    }

    val state by vm.state.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(state.isRefreshing) {
        println("state.isRefreshing: ${state.isRefreshing}")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.network)) },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            state = pullToRefreshState,
            onRefresh = {
                vm.refresh()
            },
            indicator = {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = state.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    state = pullToRefreshState,
                )
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            when (val state = state) {
                is NetworkScreenUiState.AuthFailure -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize(),
                    ) {
                        Text(stringResource(R.string.network_auth_failure_text))
                    }
                }

                is NetworkScreenUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        LoadingIndicator(Modifier.size(64.dp))
                    }
                }

                is NetworkScreenUiState.NetworkFailure -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize(),
                    ) {
                        Text(stringResource(R.string.network_error_occurred))

                        Spacer(Modifier.height(16.dp))

                        FilledTonalButton(onClick = vm::retry) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                is NetworkScreenUiState.Other -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize(),
                    ) {
                        Text(
                            text =
                            stringResource(R.string.unknown_error_occurred),
                            modifier = Modifier,
                        )
                    }
                }

                is NetworkScreenUiState.NoFriends -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_group_add),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_friends_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_friends_text),
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                is NetworkScreenUiState.Success -> {
                    Success(
                        state = state,
                        onFriendClick = { friend ->
                            onProfile(friend.id, friend.accessHash)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Success(
    state: NetworkScreenUiState.Success,
    onFriendClick: (FriendItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier) {
        items(
            items = state.friends,
            key = { friend -> friend.id.long },
        ) { friend ->
            FriendItem(
                avatarUri = friend.avatar,
                nickname = friend.nickname,
                userId = friend.id,
                onClick = { onFriendClick(friend) },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
            )
        }
        item(key = -1) {
            Text(
                text = "Add more friends to expand your network!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun FriendItem(
    avatarUri: Uri?,
    nickname: Nickname,
    userId: UserId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        UserAvatar(
            nickname = nickname,
            userId = userId,
            uri = avatarUri,
            modifier = Modifier,
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = nickname.string,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
