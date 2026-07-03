package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.NoFriendsBlockingScreenUiEvent.SnackbarEvent
import friendly.sdk.FriendToken
import friendly.sdk.UserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
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
                    isValid = isFriendLinkValid(newValue),
                ),
            )
        }
    }

    private suspend fun addFriendByLink() {
        val parsed = parseFriendLink(_state.value.friendLink.value)

        if (parsed == null) {
            _events.emit(SnackbarEvent(LinkIsInvalid))
            _state.update { old ->
                old.copy(
                    friendLink = old.friendLink.copy(isValid = false),
                )
            }
            return
        }

        val (userId, friendToken) = parsed

        val addFriendResult = addFriend(
            friendToken = friendToken,
            userId = userId,
        )

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

private fun isFriendLinkValid(link: String): Boolean {
    // TODO: add proper validation
    return link.isNotBlank()
}

private fun parseFriendLink(link: String): Pair<UserId, FriendToken>? {
    val encodedPart = link.split("?reference=")
        .getOrNull(1) ?: return null
    val charset = "UTF-8"
    val decoded = URLDecoder.decode(encodedPart, charset)
    val segments = decoded.split('/')
    val userId = segments.getOrNull(1)?.toLongOrNull() ?: return null
    val friendToken = segments.getOrNull(2) ?: return null

    return UserId(userId) to FriendToken.orThrow(friendToken)
}
