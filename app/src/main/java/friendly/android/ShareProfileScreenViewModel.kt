package friendly.android

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyFriendsClient
import friendly.sdk.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class ShareProfileScreenVmState(val shareUrl: String?) {
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
        _state.update { it.copy(shareUrl = null) }

        viewModelScope.launch {
            val auth = authStorage.getAuthOrNull() ?: return@launch // TODO
            val friendToken = client.friends.generate(auth)
            val userId = authStorage.getUserId() ?: return@launch

            _state.update { old ->
                val shareUrl = buildShareUrlOrNull(userId, friendToken)
                println(friendToken)
                println(shareUrl)
                old.copy(shareUrl = shareUrl)
            }
        }
    }

    private fun buildShareUrlOrNull(
        userId: UserId,
        friendTokenResult: FriendlyFriendsClient.GenerateResult,
    ): String? = buildString {
        val friendToken = (friendTokenResult as? Success)?.token ?: return null
        val friendTokenString = friendToken.string
        val userIdLong = userId.long
        val encodedPart = Uri.encode("add/$userIdLong/$friendTokenString")
        append("https://friendly-social.github.io/landing/#/?reference=")
        append(encodedPart)
    }
}
