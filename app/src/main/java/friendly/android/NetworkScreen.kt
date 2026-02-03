@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package friendly.android

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

@Composable
fun NetworkScreen(
    vm: NetworkScreenViewModel,
    onProfile: (UserId, UserAccessHash) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        Log.d("navigation", "network screen displayed")
        vm.initialize()
    }

    val state by vm.state.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()
    val topAppBar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        state = pullToRefreshState,
        onRefresh = { vm.refresh() },
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
        modifier = modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    state = state,
                    topAppBar = topAppBar,
                    onShare = onShare,
                )
            },
            modifier = Modifier.fillMaxSize()
                .nestedScroll(topAppBar.nestedScrollConnection),
        ) { innerPadding ->
            ScaffoldContent(
                vm = vm,
                state = state,
                padding = innerPadding,
                onProfile = onProfile,
                onShare = onShare,
            )
        }
    }
}

@Composable
private fun TopAppBar(
    state: NetworkScreenUiState,
    onShare: () -> Unit,
    topAppBar: TopAppBarScrollBehavior,
) {
    LargeFlexibleTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.network_title),
                maxLines = 1,
                overflow = Ellipsis,
            )
        },
        subtitle = {
            AnimatedVisibility(state is Success) {
                Text(
                    text = stringResource(R.string.network_add_friends_hint),
                    maxLines = 1,
                    overflow = Ellipsis,
                )
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = topAppBar,
    )
}

@Composable
private fun ScaffoldContent(
    vm: NetworkScreenViewModel,
    state: NetworkScreenUiState,
    padding: PaddingValues,
    onProfile: (UserId, UserAccessHash) -> Unit,
    onShare: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding),
    ) {
        AnimatedContent(
            targetState = state,
            contentKey = { state ->
                when (state) {
                    is AuthFailure -> 0
                    is NetworkFailure -> 1
                    is Other -> 2
                    is Loading -> 3
                    is NoFriends -> 4
                    is Success -> 5
                }
            },
        ) { state ->
            when (val state = state) {
                is NetworkScreenUiState.AuthFailure -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(stringResource(R.string.network_auth_failure_text))
                    }
                }

                is NetworkScreenUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LoadingIndicator(Modifier.size(64.dp))
                    }
                }

                is NetworkScreenUiState.NetworkFailure -> {
                    NetworkErrorBox(
                        onRetry = vm::retry,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    )
                }

                is NetworkScreenUiState.Other -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
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
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_group_add),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp),
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.no_friends_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.no_friends_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        itemsIndexed(
            items = state.friends,
            key = { _, friend -> friend.id.long },
        ) { i, friend ->
            if (i == 0) {
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(2.dp))
            }
            FriendItem(
                index = i,
                count = state.friends.size,
                avatarUri = friend.avatar,
                nickname = friend.nickname,
                userId = friend.id,
                onClick = { onFriendClick(friend) },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
            )
            if (i == state.friends.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FriendItem(
    index: Int,
    count: Int,
    avatarUri: Uri?,
    nickname: Nickname,
    userId: UserId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        leadingContent = {
            UserAvatar(
                nickname = nickname,
                userId = userId,
                uri = avatarUri,
                style = Small,
            )
        },
    ) {
        Text(nickname.string)
    }
}
