package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.FriendlyFeedClient
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val someRandomUserSample = FeedItem.Entry(
    nickname = Nickname.orThrow("demn demov"),
    description = UserDescription.orThrow(
        "hi i like very much things in this world tho",
    ),
    interests = listOf(
        Interest.orThrow("zig"),
        Interest.orThrow("zigga"),
        Interest.orThrow("zed"),
        Interest.orThrow("zetnik"),
        Interest.orThrow("networking"),
        Interest.orThrow("chinese tea"),
        Interest.orThrow("neovim"),
        Interest.orThrow("metric time"),
        Interest.orThrow("gui"),
        Interest.orThrow("meow"),
        Interest.orThrow("dkdkdkd"),
        Interest.orThrow("67"),
        Interest.orThrow("Brruuuuh"),
        Interest.orThrow("whiskey"),
        Interest.orThrow("in"),
        Interest.orThrow("OMG"),
        Interest.orThrow("amogus"),
        Interest.orThrow("Android Studio is a piece of shit"),
    ),
)

private data class FeedScreenVmState(
    val currentFeedItem: FeedItem = FeedItem.Loading,
    val isNetworkError: Boolean,
) {
    companion object {
        val Initial = FeedScreenVmState(
            currentFeedItem = FeedItem.Loading,
            isNetworkError = false,
        )
    }

    fun toUiState(): FeedScreenUiState {
        if (isNetworkError) {
            return FeedScreenUiState.NetworkError
        }

        return FeedScreenUiState.Idle(currentFeedItem)
    }
}

class FeedScreenViewModel(private val client: FriendlyFeedClient) :
    ViewModel() {
    private val _state = MutableStateFlow(FeedScreenVmState.Initial)
    val state: StateFlow<FeedScreenUiState> = _state
        .map(FeedScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeedScreenUiState.Idle(FeedItem.Loading),
        )

    fun loadFeed() {
        viewModelScope.launch {
            // TODO
        }
    }

    fun like() {
        viewModelScope.launch {
            // TODO
        }
    }

    fun dislike() {
        viewModelScope.launch {
            // TODO
        }
    }
}
