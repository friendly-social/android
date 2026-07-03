package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.FriendToken
import friendly.sdk.FriendlyFriendsClient
import friendly.sdk.UserId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddFriendByTokenScreenViewModel(
    private val addFriend: AddFriendUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<AddFriendByTokenScreenUiState>(
        value = AddFriendByTokenScreenUiState.Waiting,
    )
    val state = _state.asStateFlow()

    // todo introduce events here
    private val _events = MutableSharedFlow<String>()
    val events = _events.shareIn(viewModelScope, Eagerly)

    fun add(userId: UserId, friendToken: FriendToken) {
        viewModelScope.launch {
            val result = addFriend(friendToken, userId)
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
