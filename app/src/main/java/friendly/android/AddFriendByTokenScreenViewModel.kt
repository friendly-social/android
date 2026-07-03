package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.AddFriendByTokenScreenUiEvents.SnackbarEvent
import friendly.android.AddFriendByTokenScreenUiEvents.SnackbarEvent.SnackbarEventKind.FriendLinkExpired
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

    private val _events = MutableSharedFlow<AddFriendByTokenScreenUiEvents>()
    val events = _events.shareIn(viewModelScope, Eagerly)

    fun add(userId: UserId, friendToken: FriendToken) {
        viewModelScope.launch {
            val result = addFriend(friendToken, userId)
            val finalState = when (result) {
                is FriendlyFriendsClient.AddResult.FriendTokenExpired -> {
                    _events.emit(SnackbarEvent(FriendLinkExpired))
                    AddFriendByTokenScreenUiState.FriendTokenExpired
                }

                is FriendlyFriendsClient.AddResult.IOError -> {
                    _events.emit(SnackbarEvent(NetworkErrorOccurred))
                    AddFriendByTokenScreenUiState.NetworkError
                }

                is FriendlyFriendsClient.AddResult.ServerError -> {
                    _events.emit(SnackbarEvent(ServerErrorOccurred))
                    AddFriendByTokenScreenUiState.ServerError
                }

                is FriendlyFriendsClient.AddResult.Unauthorized -> {
                    _events.emit(SnackbarEvent(UnauthorizedError))
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
