package friendly.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.FriendToken
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyFriendsClient
import friendly.sdk.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddFriendByTokenScreenViewModel(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
) : ViewModel() {
    private val _state = MutableStateFlow<AddFriendByTokenScreenUiState>(
        value = AddFriendByTokenScreenUiState.Waiting,
    )
    val state = _state.asStateFlow()

    fun add(userId: UserId, friendToken: FriendToken) {
        val authorization = authStorage.getAuth()

        if (authorization == null) {
            _state.update {
                AddFriendByTokenScreenUiState.Unauthorized
            }
            return
        }

        viewModelScope.launch {
            val result = client.friends.add(
                authorization = authorization,
                token = friendToken,
                userId = userId,
            )

            // todo add ability to fetch user info and to navigate to user account after add
            val finalState = when (result) {
                is FriendlyFriendsClient.AddResult.FriendTokenExpired -> {
                    AddFriendByTokenScreenUiState.FriendTokenExpired
                }

                is FriendlyFriendsClient.AddResult.IOError -> {
                    AddFriendByTokenScreenUiState.NetworkError
                }

                is FriendlyFriendsClient.AddResult.ServerError -> {
                    AddFriendByTokenScreenUiState.UnknownError
                }

                is FriendlyFriendsClient.AddResult.Unauthorized -> {
                    AddFriendByTokenScreenUiState.Unauthorized
                }

                is FriendlyFriendsClient.AddResult.Success -> {
                    AddFriendByTokenScreenUiState.Success
                }
            }

            _state.update { finalState }
        }
    }
}

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
        modifier = modifier.fillMaxSize(),
    ) {
        when (state) {
            AddFriendByTokenScreenUiState.FriendTokenExpired -> {
                FriendTokenExpired()
            }

            AddFriendByTokenScreenUiState.NetworkError -> {
                NetworkError(vm, userId, friendToken)
            }

            AddFriendByTokenScreenUiState.Unauthorized -> {
                Unauthorized(goToSignUp)
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
private fun FriendTokenExpired() {
    Text(
        text = stringResource(R.string.friend_token_expired_text),
    )
}

@Composable
private fun NetworkError(
    vm: AddFriendByTokenScreenViewModel,
    userId: UserId,
    friendToken: FriendToken,
) {
    Column {
        Text(text = stringResource(R.string.network_error_occurred))
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { vm.add(userId, friendToken) }) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun Unauthorized(goToSignUp: () -> Unit) {
    Column {
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
private fun Success(goHome: () -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.add_friend_by_token_success_text),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = goHome) {
            Text(text = "Go home")
        }
    }
}
