package friendly.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import friendly.android.SelfProfileScreenUiState.Present
import friendly.android.SelfProfileScreenViewModel.UserProfile
import friendly.sdk.Interest
import friendly.android.FriendlyNavGraph.Home.EditProfile as EditProfileRoute

private val SelfProfileScreenUiState.unlinkedEmailBadgeVisible: Boolean
    get() = if (this is Present) profile.email == null else false

sealed interface SelfProfileScreenUiState {
    data class Present(val profile: UserProfile) : SelfProfileScreenUiState

    data object Loading : SelfProfileScreenUiState

    data object Error : SelfProfileScreenUiState
}

@Composable
fun SelfProfileScreen(
    vm: SelfProfileScreenViewModel,
    onSignOut: () -> Unit,
    onEditProfileClick: (EditProfileRoute) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val state by vm.state.collectAsState()
    var signOutDialogVisible by remember { mutableStateOf(false) }
    var linkEmailDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    val state = state
                    if (state is Present) {
                        IconButton(
                            onClick = {
                                onEditProfileClick(state.toEditRoute())
                            },
                        ) {
                            Icon(
                                painter =
                                painterResource(
                                    R.drawable.ic_edit_outlined,
                                ),
                                contentDescription = null,
                            )
                        }
                    }

                    if (state.unlinkedEmailBadgeVisible) {
                        IconButton(
                            onClick = { linkEmailDialogVisible = true },
                        ) {
                            BadgedBox(
                                badge = { Badge() },
                            ) {
                                Icon(
                                    painter = painterResource(
                                        R.drawable.ic_mail_outlined,
                                    ),
                                    contentDescription = null,
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { signOutDialogVisible = true },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor =
                    MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            LinkEmailAlertDialog(
                visible = linkEmailDialogVisible,
                onAlertVisibility = { newValue ->
                    linkEmailDialogVisible = newValue
                },
                onEditProfileClick = {
                    val route = (state as? Present)?.let(Present::toEditRoute)
                    route?.let(onEditProfileClick)
                },
            )

            SignOutAlertDialog(
                visible = signOutDialogVisible,
                onAlertVisibility = { newValue ->
                    signOutDialogVisible = newValue
                },
                vm = vm,
                onSignOut = onSignOut,
            )

            when (val state = state) {
                is SelfProfileScreenUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LoadingIndicator(Modifier.size(48.dp))
                    }
                }

                is Present -> {
                    LoadedSelfProfileState(
                        state = state,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is SelfProfileScreenUiState.Error ->
                    Text(
                        text = stringResource(
                            R.string.profile_screen_error_text,
                        ),
                    )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadedSelfProfileState(
    state: Present,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
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
                    nickname = state.profile.nickname,
                    userId = state.profile.userId,
                    uri = state.profile.avatar,
                    style = Large,
                    modifier = Modifier,
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = state.profile.nickname.string,
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
            state.profile.interests.raw.forEachIndexed { _, interest ->
                InterestChip(interest)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = state.profile.description.string,
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(
            Modifier
                .padding(contentPadding.takeBottom())
                .height(32.dp),
        )
    }
}

@Composable
private fun SignOutAlertDialog(
    visible: Boolean,
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
                Text(stringResource(R.string.log_out_alert_text))
            },
            onDismissRequest = {
                onAlertVisibility(false)
            },
            confirmButton = {
                TextButton(onClick = { vm.logout(onSignOut) }) {
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

private fun Present.toEditRoute(): EditProfileRoute {
    val state = this
    return EditProfileRoute(
        nickname = state.profile.nickname.serializable(),
        description = state.profile.description.serializable(),
        interests = state.profile.interests.raw.map(Interest::serializable),
        socialLink = state.profile.socialLink?.serializable(),
        email = state.profile.email?.serializable(),
        userId = state.profile.userId.serializable(),
        avatarUri = state.profile.avatar?.toString(),
    )
}
