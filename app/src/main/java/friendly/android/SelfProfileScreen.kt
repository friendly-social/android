package friendly.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import friendly.android.SelfProfileScreenViewModel.UserProfile

sealed interface SelfProfileScreenUiState {
    data class Present(val profile: UserProfile) : SelfProfileScreenUiState

    data object Loading : SelfProfileScreenUiState

    data object Error : SelfProfileScreenUiState
}

@Composable
fun SelfProfileScreen(
    vm: SelfProfileScreenViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    var signOutDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
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
                is SelfProfileScreenUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LoadingIndicator(Modifier.size(48.dp))
                    }
                }

                is SelfProfileScreenUiState.Present -> {
                    LoadedSelfProfileState(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is SelfProfileScreenUiState.Error ->
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadedSelfProfileState(
    state: SelfProfileScreenUiState.Present,
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

        Spacer(Modifier.height(32.dp))
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
