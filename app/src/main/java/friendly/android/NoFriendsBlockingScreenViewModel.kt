package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.NoFriendsBlockingScreenUiEvent.SnackbarEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private data class NoFriendsBlockingScreenVmState(
    val friendLink: ValidatableField<String>,
    val isLoading: Boolean,
) {
    fun toUiState(): NoFriendsBlockingScreenUiState {
        if (isLoading) return NoFriendsBlockingScreenUiState.Loading

        return NoFriendsBlockingScreenUiState.Idle(friendLink)
    }
}

class NoFriendsBlockingScreenViewModel(
    private val addFriend: AddFriendUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(
        NoFriendsBlockingScreenVmState(ValidatableField(""), false),
    )

    val state = _state
        .map(NoFriendsBlockingScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = Eagerly,
            initialValue = NoFriendsBlockingScreenUiState.Loading,
        )

    private val _events = MutableSharedFlow<NoFriendsBlockingScreenUiEvent>()
    val events = _events.shareIn(viewModelScope, Eagerly)

    fun onFriendLink(newValue: String) {
        _state.update {
            it.copy(
                friendLink = ValidatableField(
                    value = newValue,
                    isValid = FriendInviteLink.validate(newValue),
                ),
            )
        }
    }

    private suspend fun addFriendByLink() {
        val friendLink = FriendInviteLink(_state.value.friendLink.value)
        val friendToken = friendLink.asFriendToken()
        if (friendToken == null) {
            _events.emit(SnackbarEvent(LinkIsInvalid))
            _state.update { old ->
                old.copy(
                    friendLink = old.friendLink.copy(isValid = false),
                )
            }
            return
        }

        val addFriendResult = addFriend(friendToken = friendToken)

        when (addFriendResult) {
            is FriendTokenExpired -> {
                _events.emit(SnackbarEvent(FriendLinkExpired))
            }

            is IOError -> {
                _events.emit(SnackbarEvent(NetworkErrorOccurred))
            }

            is ServerError -> {
                _events.emit(SnackbarEvent(ServerErrorOccurred))
            }

            is Unauthorized -> {
                _events.emit(SnackbarEvent(UnauthorizedError))
            }

            is Success -> {
                _events.emit(SnackbarEvent(AddFriendSuccess))
                delay(100.milliseconds)
                _events.emit(AddFriendSuccess)
            }
        }
    }

    fun onConfirm() {
        if (!_state.value.friendLink.isValid) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            addFriendByLink()
            _state.update { it.copy(isLoading = false) }
        }
    }
}
