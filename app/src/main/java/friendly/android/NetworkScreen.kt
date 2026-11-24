package friendly.android

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import friendly.android.FriendlyNavGraph.Home
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
    onProfile: (Home.Profile) -> Unit,
    onShare: () -> Unit,
    onFeed: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val state by vm.state.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()
    val topAppBar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = rememberLifecycleOwner()
    val snackbarStrings = snackbarStrings()

    LaunchedEffect(Unit) {
        vm.initialize()

        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                val message = snackbarStrings[event] ?: return@collect
                snackbarHostState.showSnackbar(message)
            }
        }
    }

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
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    state = state,
                    topAppBar = topAppBar,
                    onShare = onShare,
                )
            },
            floatingActionButton = { ExpandNetworkFab(onFeed) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topAppBar.nestedScrollConnection),
        ) { innerPadding ->
            ScaffoldContent(
                vm = vm,
                state = state,
                padding = innerPadding,
                onProfile = onProfile,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
            )
        }
    }
}

@Composable
private fun ExpandNetworkFab(onFeed: () -> Unit) {
    // TODO:
    //  display here amount of incoming friend requests to tease
    //  the user. also display this amount in the tooltip
    TooltipBox(
        positionProvider =
        TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
        ),
        tooltip = {
            PlainTooltip(
                modifier =
                Modifier.semantics {
                    liveRegion = LiveRegionMode.Assertive
                    paneTitle = "Expand network"
                },
            ) {
                Text(stringResource(R.string.see_friends_of_your_friends))
            }
        },
        state = rememberTooltipState(),
    ) {
        ExtendedFloatingActionButton(
            onClick = onFeed,
            icon = {
                Icon(
                    painter =
                    painterResource(
                        R.drawable.ic_partner_exchange_filled,
                    ),
                    contentDescription = null,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.explore_network),
                )
            },
        )
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
private fun snackbarStrings(): Map<NetworkScreenEvent, String> = mapOf(
    NetworkScreenEvent.AddFriendByLinkSuccess to
        stringResource(R.string.add_friend_by_token_success_text),
    NetworkScreenEvent.AddFriendByLinkFailure to
        stringResource(R.string.add_friend_by_token_failure_text),
    NetworkScreenEvent.FriendLinkExpired to
        stringResource(R.string.friend_link_expired),
    NetworkScreenEvent.FriendLinkInvalid to
        stringResource(R.string.link_is_invalid),
)

@Composable
private fun ScaffoldContent(
    vm: NetworkScreenViewModel,
    state: NetworkScreenUiState,
    padding: PaddingValues,
    onProfile: (Home.Profile) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    var addFriendByLinkDialogVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        AddFriendByLinkDialog(
            visible = addFriendByLinkDialogVisible,
            onDismiss = { addFriendByLinkDialogVisible = false },
            onAdd = { link ->
                vm.addFriendByLink(link)
                addFriendByLinkDialogVisible = false
            },
        )

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
                            val route = Home.Profile(
                                userId = friend.id.long,
                                accessHash = friend.accessHash.string,
                                nickname = friend.nickname.string,
                                avatarUri = friend.avatar?.toString(),
                            )
                            onProfile(route)
                        },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        onAddFriendByLink = {
                            addFriendByLinkDialogVisible = true
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
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onAddFriendByLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
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
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
            )
            if (i == state.friends.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
        item {
            OutlinedButton(
                contentPadding = ButtonDefaults.MediumContentPadding,
                shape = ButtonDefaults.squareShape,
                onClick = onAddFriendByLink,
                modifier = Modifier
                    .heightIn(ButtonDefaults.MediumContainerHeight)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        R.string.add_your_friend_by_invite_link,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
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
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
): Unit = with(sharedTransitionScope) {
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
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = sharedTransitionScope
                            .rememberSharedContentState(
                                key = sharedProfileAvatarKey(userId),
                            ),
                        animatedVisibilityScope = animatedContentScope,
                    ),
            )
        },
        modifier = modifier,
    ) {
        Text(
            text = nickname.string,
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = sharedTransitionScope
                        .rememberSharedContentState(
                            key = sharedProfileNicknameKey(userId),
                        ),
                    animatedVisibilityScope = animatedContentScope,
                ),
        )
    }
}

@Composable
private fun AddFriendByLinkDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAdd: (link: String) -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandIn(),
        exit = ExitTransition.None,
    ) {
        AlertDialog(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_group_add),
                    contentDescription = null,
                )
            },
            title = {
                Text(stringResource(R.string.add_friend_by_invite_link))
            },
            text = {
                OutlinedTextField(
                    state = textFieldState,
                    placeholder = {
                        Text(
                            text = stringResource(
                                R.string.add_friend_by_invite_link_placeholder,
                            ),
                        )
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        textFieldState.clearText()
                        onAdd(textFieldState.string)
                    },
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        textFieldState.clearText()
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun sharedProfileNicknameKey(userId: UserId): String =
    "network-nickname-${userId.long}"

@Composable
fun sharedProfileAvatarKey(userId: UserId): String =
    "network-avatar-${userId.long}"
