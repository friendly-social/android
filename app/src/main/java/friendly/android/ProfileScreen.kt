package friendly.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import friendly.android.ProfileScreenViewModel.UserProfile
import friendly.sdk.UserAccessHash
import friendly.sdk.UserId

sealed interface ProfileScreenUiState {
    data class Present(val profile: UserProfile) : ProfileScreenUiState

    data object Loading : ProfileScreenUiState

    data object Error : ProfileScreenUiState
}

sealed interface ProfileScreenSource {
    data object SelfProfile : ProfileScreenSource
    data class FriendProfile(val id: UserId, val accessHash: UserAccessHash) :
        ProfileScreenSource
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun ProfileScreen(
    source: ProfileScreenSource,
    onHome: () -> Unit,
    onSignOut: () -> Unit,
    vm: ProfileScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    var signOutDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.load(source)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    if (source is ProfileScreenSource.FriendProfile) {
                        IconButton(onClick = onHome) {
                            Icon(
                                painter =
                                painterResource(R.drawable.ic_arrow_back),
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = {
                    if (source is ProfileScreenSource.SelfProfile) {
                        IconButton(
                            onClick = { signOutDialogVisible = true },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_logout),
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .consumeWindowInsets(innerPadding)
                .fillMaxSize(),
        ) {
            SignOutAlertDialog(
                visible = signOutDialogVisible,
                onAlertVisibility = { newValue ->
                    signOutDialogVisible = newValue
                },
                vm = vm,
                onSignOut = onSignOut,
            )

            when (val state = state) {
                is ProfileScreenUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        LoadingIndicator(Modifier.size(48.dp))
                    }
                }

                is ProfileScreenUiState.Present -> {
                    LoadedProfileState(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is ProfileScreenUiState.Error ->
                    Text(
                        text = stringResource(
                            R.string.profile_screen_error_text,
                        ),
                        modifier = Modifier.padding(innerPadding),
                    )
            }
        }
    }
}

@Composable
private fun SignOutAlertDialog(
    visible: Boolean,
    vm: ProfileScreenViewModel,
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

@Composable
fun LoadedProfileState(
    state: ProfileScreenUiState.Present,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(128.dp))

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
                    modifier = Modifier.size(128.dp),
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
            state.profile.interests.forEach { interest ->
                Text(
                    text = interest.string,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            color = Color.pastelFromString(
                                string = interest.string,
                                useDark = isSystemInDarkTheme(),
                            ),
                        )
                        .padding(4.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = state.profile.description.string,
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(32.dp))
    }
}
