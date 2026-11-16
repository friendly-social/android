package friendly.android

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import friendly.android.SelfProfileScreenUiState.Idle
import friendly.query.QueryState
import friendly.sdk.FileDescriptor
import friendly.sdk.Interest
import friendly.sdk.UserDetails
import friendly.android.FriendlyNavGraph.Home.EditProfile as EditProfileRoute

private val UserDetails.unlinkedEmailBadgeVisible: Boolean
    get() = this.email == null

sealed interface SelfProfileScreenUiState {
    data class Idle(val isLoggingOut: Boolean) : SelfProfileScreenUiState
}

private val SelfProfileScreenUiState.isLoggingOut
    get() = (this as? Idle)?.isLoggingOut ?: false

@Composable
fun SelfProfileScreen(
    vm: SelfProfileScreenViewModel,
    onSignOut: () -> Unit,
    onEditProfileClick: (EditProfileRoute) -> Unit,
    onProfilePictureClick: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val state by vm.state.collectAsState()
    var signOutDialogVisible by remember { mutableStateOf(false) }
    var linkEmailDialogVisible by remember { mutableStateOf(false) }

    val queryState by vm.profileQuery.data.collectAsState()
    val profile = queryState.item

    val pullToRefreshState = rememberPullToRefreshState()

    val isRefreshing = remember(queryState) {
        queryState.fetch is Refreshing
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = vm.profileQuery::refresh,
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
                TopBar(
                    state = state,
                    profile = profile,
                    onEditProfileClick = {
                        profile
                            ?.toEditRoute(vm::fileUri)
                            ?.let(onEditProfileClick)
                    },
                    onSignOutClick = { signOutDialogVisible = true },
                    onLinkEmailClick = { linkEmailDialogVisible = true },
                    modifier = Modifier,
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            ScaffoldContent(
                state = state,
                vm = vm,
                contentPadding = contentPadding,
                onProfilePictureClick = onProfilePictureClick,
                onSignOut = onSignOut,
                queryState = queryState,
                profile = profile,
                signOutDialogVisible = signOutDialogVisible,
                linkEmailDialogVisible = linkEmailDialogVisible,
                onSignOutDialogVisible = { signOutDialogVisible = it },
                onLinkEmailAlertDialog = { linkEmailDialogVisible = it },
                onEditProfileClick = {
                    val route = profile?.toEditRoute(vm::fileUri)
                    route?.let(onEditProfileClick)
                },
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TopBar(
    state: SelfProfileScreenUiState,
    profile: UserDetails?,
    onEditProfileClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onLinkEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {},
        actions = {
            if (state is Idle) {
                IconButton(onClick = onEditProfileClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_outlined),
                        contentDescription = null,
                    )
                }
            }

            if (profile?.unlinkedEmailBadgeVisible ?: false) {
                IconButton(onClick = onLinkEmailClick) {
                    BadgedBox(badge = { Badge() }) {
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_mail_outlined,
                            ),
                            contentDescription = null,
                        )
                    }
                }
            }

            IconButton(onClick = onSignOutClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_logout),
                    contentDescription = null,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ScaffoldContent(
    vm: SelfProfileScreenViewModel,
    queryState: QueryState<UserDetails>,
    profile: UserDetails?,
    signOutDialogVisible: Boolean,
    onSignOutDialogVisible: (Boolean) -> Unit,
    linkEmailDialogVisible: Boolean,
    onLinkEmailAlertDialog: (Boolean) -> Unit,
    onEditProfileClick: () -> Unit,
    onSignOut: () -> Unit,
    onProfilePictureClick: (String) -> Unit,
    contentPadding: PaddingValues,
    state: SelfProfileScreenUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        LinkEmailAlertDialog(
            visible = linkEmailDialogVisible,
            onAlertVisibility = onLinkEmailAlertDialog,
            onEditProfileClick = onEditProfileClick,
        )

        SignOutAlertDialog(
            visible = signOutDialogVisible,
            isLoggingOut = state.isLoggingOut,
            onAlertVisibility = onSignOutDialogVisible,
            vm = vm,
            onSignOut = onSignOut,
        )

        if (profile != null) {
            LoadedSelfProfileState(
                profile = profile,
                onImageClick = { uri ->
                    onProfilePictureClick(uri.toString())
                },
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
                fileUri = vm::fileUri,
            )
        } else {
            when (queryState.fetch) {
                is QueryState.FetchStatus.Idle -> {
                    if (queryState.error) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.profile_screen_error_text,
                                ),
                            )
                        }
                    }
                }

                is Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LoadingIndicator(Modifier.size(48.dp))
                    }
                }

                is Refreshing -> TODO()
            }
        }
    }
}

@Composable
fun LinkEmailAlertDialog(
    visible: Boolean,
    onAlertVisibility: (Boolean) -> Unit,
    onEditProfileClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandIn(),
        exit = ExitTransition.None,
    ) {
        AlertDialog(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_mail_outlined),
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.warning)) },
            text = { Text(stringResource(R.string.no_linked_email_text)) },
            onDismissRequest = { onAlertVisibility(false) },
            confirmButton = {
                TextButton(onClick = onEditProfileClick) {
                    Text(stringResource(R.string.go_to_profile_edit))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAlertVisibility(false) }) {
                    Text(stringResource(R.string.later))
                }
            },
        )
    }
}

@Composable
private fun LoadedSelfProfileState(
    profile: UserDetails,
    onImageClick: (Uri) -> Unit,
    fileUri: (FileDescriptor) -> Uri?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = Zero,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                UserAvatar(
                    nickname = profile.nickname,
                    userId = profile.id,
                    uri = profile.avatar?.let(fileUri),
                    style = Large,
                    modifier = Modifier.clickable(
                        onClick = {
                            profile.avatar?.let(fileUri)?.let(onImageClick)
                        },
                        indication = null,
                        interactionSource = null,
                    ),
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = profile.nickname.string,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            profile.interests.raw.forEachIndexed { _, interest ->
                InterestChip(interest)
            }
        }

        Spacer(Modifier.height(24.dp))

        ProfileMarkdown(
            markdown = profile.description.string,
            onImageClick = { onImageClick(it.toUri()) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(
            modifier = Modifier.padding(contentPadding.takeBottom()),
        )
    }
}

@Composable
private fun SignOutAlertDialog(
    visible: Boolean,
    isLoggingOut: Boolean,
    vm: SelfProfileScreenViewModel,
    onAlertVisibility: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandIn(),
        exit = ExitTransition.None,
    ) {
        AlertDialog(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                )
            },
            title = {
                Text(stringResource(R.string.attention))
            },
            text = {
                when (isLoggingOut) {
                    true -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }

                    false -> {
                        Text(stringResource(R.string.log_out_alert_text))
                    }
                }
            },
            onDismissRequest = {
                onAlertVisibility(false)
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.logout(onSignOut) },
                    enabled = !isLoggingOut,
                ) {
                    Text(stringResource(R.string.log_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAlertVisibility(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun UserDetails.toEditRoute(
    fileUri: (FileDescriptor) -> Uri?,
): EditProfileRoute {
    val profile = this
    return EditProfileRoute(
        nickname = profile.nickname.serializable(),
        description = profile.description.serializable(),
        interests = profile.interests.raw.map(Interest::serializable),
        socialLink = profile.socialLink?.serializable(),
        email = profile.email?.serializable(),
        userId = profile.id.serializable(),
        avatarUri = profile.avatar?.let(fileUri)?.toString(),
    )
}
