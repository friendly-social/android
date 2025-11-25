package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.FriendlyClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class ShareProfileScreenVmState(
    val shareUrl: String?,
) {
    fun toUiState(): ShareProfileScreenUiState {
        if (shareUrl == null) {
            return ShareProfileScreenUiState.Generating
        }
        return ShareProfileScreenUiState.Share(shareUrl)
    }
}

class ShareProfileScreenViewModel(
    private val authStorage: AuthStorage,
    private val client: FriendlyClient,
) : ViewModel() {
    private val _state: MutableStateFlow<ShareProfileScreenVmState> =
        MutableStateFlow(ShareProfileScreenVmState(null))

    val state = _state
        .map(ShareProfileScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ShareProfileScreenUiState.Generating,
        )

    fun generateUrl() {
        viewModelScope.launch {
            val auth = authStorage.getAuth() ?: return@launch // TODO
            val friendToken = client.friends.generate(auth)
            val userId = authStorage.getUserId() ?: return@launch

            _state.update { old ->
                old.copy(
                    shareUrl = "friendly://add/${userId.long}/${friendToken.orThrow().string}",
                )
            }
        }
    }
}
