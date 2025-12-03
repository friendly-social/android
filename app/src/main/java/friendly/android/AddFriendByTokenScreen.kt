package friendly.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import friendly.sdk.FriendToken
import friendly.sdk.UserId

sealed interface AddFriendByTokenScreenUiState {
    data object Waiting : AddFriendByTokenScreenUiState
    data object Unauthorized : AddFriendByTokenScreenUiState
    data object FriendTokenExpired : AddFriendByTokenScreenUiState
    data object NetworkError : AddFriendByTokenScreenUiState
    data object UnknownError : AddFriendByTokenScreenUiState
    data object Success : AddFriendByTokenScreenUiState
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddFriendByTokenScreen(
    goToSignUp: () -> Unit,
    goHome: () -> Unit,
    friendToken: FriendToken,
    userId: UserId,
    vm: AddFriendByTokenScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.add(userId, friendToken)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        when (state) {
            AddFriendByTokenScreenUiState.FriendTokenExpired -> {
                FriendTokenExpired(Modifier)
            }

            AddFriendByTokenScreenUiState.NetworkError -> {
                NetworkError(vm, userId, friendToken, Modifier)
            }

            AddFriendByTokenScreenUiState.Unauthorized -> {
                Unauthorized(goToSignUp, Modifier)
            }

            AddFriendByTokenScreenUiState.UnknownError -> {
                Text(stringResource(R.string.unknown_error_occurred))
            }

            AddFriendByTokenScreenUiState.Waiting -> {
                LoadingIndicator(Modifier.size(100.dp))
            }

            AddFriendByTokenScreenUiState.Success -> {
                Success(goHome)
            }
        }
    }
}

@Composable
private fun FriendTokenExpired(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.friend_token_expired_text),
        modifier = modifier,
    )
}

@Composable
private fun NetworkError(
    vm: AddFriendByTokenScreenViewModel,
    userId: UserId,
    friendToken: FriendToken,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(text = stringResource(R.string.network_error_occurred))
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { vm.add(userId, friendToken) }) {
            Text(text = "Retry")
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
    goHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.add_friend_by_token_success_text),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = goHome) {
            Text(text = stringResource(R.string.go_home))
        }
    }
}
