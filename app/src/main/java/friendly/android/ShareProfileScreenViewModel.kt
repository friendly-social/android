package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.FriendToken
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyFriendsClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class ShareProfileScreenVmState(
    val shareResult: FriendlyFriendsClient.GenerateResult.Success?,
    val error: Boolean,
) {
    fun toUiState(): ShareProfileScreenUiState {
        if (shareResult == null) {
            if (error) return ShareProfileScreenUiState.Error
            return ShareProfileScreenUiState.Generating
        }
        return ShareProfileScreenUiState.Share(
            shareUrl = buildShareUrl(friendToken = shareResult.token),
            expiration = shareResult.expiration,
        )
    }
}

class ShareProfileScreenViewModel(
    private val authStorage: AuthStorage,
    private val client: FriendlyClient,
) : ViewModel() {
    private val _state: MutableStateFlow<ShareProfileScreenVmState> =
        MutableStateFlow(ShareProfileScreenVmState(null, false))

    val state = _state
        .map(ShareProfileScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ShareProfileScreenUiState.Generating,
        )

    fun generateUrl() {
        _state.update { it.copy(shareResult = null, error = false) }

        viewModelScope.launch {
            val auth = authStorage.getAuth()
            when (val result = client.friends.generate(auth)) {
                is FriendlyFriendsClient.GenerateResult.Success -> {
                    _state.update { old -> old.copy(shareResult = result) }
                }

                is FriendlyFriendsClient.GenerateResult.IOError,
                is FriendlyFriendsClient.GenerateResult.ServerError,
                is FriendlyFriendsClient.GenerateResult.Unauthorized,
                -> {
                    _state.update { old -> old.copy(error = true) }
                }
            }
        }
    }
}

private fun buildShareUrl(friendToken: FriendToken): String =
    "https://invite.getfriend.ly/${friendToken.string}"
