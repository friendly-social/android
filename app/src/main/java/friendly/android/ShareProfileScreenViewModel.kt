package friendly.android

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
import java.net.URLEncoder
import java.nio.charset.Charset

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
        viewModelScope.launch {
            val auth = authStorage.getAuthOrNull() ?: return@launch // TODO
            val friendToken = client.friends.generate(auth)
            val userId = authStorage.getUserId() ?: return@launch

            _state.update { old ->
                val shareUrl = buildShareUrl(userId, friendToken)
                println(friendToken)
                println(shareUrl)
                old.copy(shareUrl = shareUrl)
            }
        }
    }

    // TODO
    private fun buildShareUrl(
        userId: UserId,
        friendToken: FriendlyFriendsClient.GenerateResult,
    ): String = buildString {
        append("https://friendly-social.github.io/landing/#/?reference=")
        val encodedPart = URLEncoder.encode(
            "add/${userId.long}/${friendToken.orThrow().string}",
            Charset.defaultCharset(),
        )
        append(encodedPart)
    }
}
