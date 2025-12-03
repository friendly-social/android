package friendly.android

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
