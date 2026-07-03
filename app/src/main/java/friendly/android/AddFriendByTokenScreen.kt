package friendly.android

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import friendly.android.AddFriendByTokenScreenUiEvents.SnackbarEvent
import friendly.android.AddFriendByTokenScreenUiEvents.SnackbarEvent.SnackbarEventKind
import friendly.sdk.FriendToken
import friendly.sdk.UserId

sealed interface AddFriendByTokenScreenUiState {
    data object Waiting : AddFriendByTokenScreenUiState
    data object Unauthorized : AddFriendByTokenScreenUiState
    data object FriendTokenExpired : AddFriendByTokenScreenUiState
    data object NetworkError : AddFriendByTokenScreenUiState
    data object ServerError : AddFriendByTokenScreenUiState
    data object Success : AddFriendByTokenScreenUiState
}

sealed interface AddFriendByTokenScreenUiEvents {
    data class SnackbarEvent(val kind: SnackbarEventKind) :
        AddFriendByTokenScreenUiEvents {
        enum class SnackbarEventKind {
            FriendLinkExpired,
            NetworkErrorOccurred,
            ServerErrorOccurred,
            UnauthorizedError,
        }
    }
}

@Composable
private fun snackbarStrings(): Map<SnackbarEventKind, String> =
    SnackbarEventKind.entries.associateWith { event ->
        when (event) {
            FriendLinkExpired ->
                stringResource(R.string.friend_link_expired)

            NetworkErrorOccurred ->
                stringResource(R.string.network_error_occurred)

            ServerErrorOccurred -> stringResource(R.string.server_error)

            UnauthorizedError ->
                stringResource(R.string.unauthorized)
        }
    }

@Composable
fun AddFriendByTokenScreen(
    goToSignUp: () -> Unit,
    onNetworkScreen: () -> Unit,
    onGoBack: () -> Unit,
    friendToken: FriendToken,
    userId: UserId,
    vm: AddFriendByTokenScreenViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val state by vm.state.collectAsState()

    val lifecycleOwner = rememberLifecycleOwner()

    val snackbarStrings = snackbarStrings()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.add(userId, friendToken)

        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                when (event) {
                    is SnackbarEvent -> {
                        snackbarHostState.showSnackbar(
                            message = snackbarStrings.getValue(event.kind),
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        AnimatedContent(state) { state ->
            when (state) {
                AddFriendByTokenScreenUiState.FriendTokenExpired -> {
                    FriendTokenExpired(
                        onGoBack = onGoBack,
                        modifier = Modifier,
                    )
                }

                AddFriendByTokenScreenUiState.NetworkError -> {
                    NetworkError(
                        vm = vm,
                        userId = userId,
                        friendToken = friendToken,
                        onGoBack = onGoBack,
                        modifier = Modifier,
                    )
                }

                AddFriendByTokenScreenUiState.Unauthorized -> {
                    Unauthorized(goToSignUp, Modifier)
                }

                AddFriendByTokenScreenUiState.ServerError -> {
                    Text(stringResource(R.string.server_error_occurred))
                }

                AddFriendByTokenScreenUiState.Waiting -> {
                    LoadingIndicator(Modifier.size(100.dp))
                }

                AddFriendByTokenScreenUiState.Success -> {
                    Success(
                        onNetworkScreen = onNetworkScreen,
                        modifier = Modifier.padding(contentPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendTokenExpired(
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.friend_token_expired_text),
            modifier = Modifier,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onGoBack) {
            Text(text = stringResource(R.string.go_back))
        }
    }
}

@Composable
private fun NetworkError(
    vm: AddFriendByTokenScreenViewModel,
    userId: UserId,
    friendToken: FriendToken,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        NetworkErrorBox(
            onRetry = { vm.add(userId, friendToken) },
            modifier = Modifier,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onGoBack) {
            Text(text = stringResource(R.string.go_back))
        }
    }
}

@Composable
private fun Unauthorized(
    goToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(
                R.string.add_friend_by_token_unauthorized_text,
            ),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = goToSignUp) {
            Text(stringResource(R.string.sign_up))
        }
    }
}

@Composable
private fun Success(
    onNetworkScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.add_friend_by_token_success_text),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            contentPadding = ButtonDefaults.MediumContentPadding,
            shape = ButtonDefaults.squareShape,
            onClick = onNetworkScreen,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.go_to_network))
        }
    }
}
