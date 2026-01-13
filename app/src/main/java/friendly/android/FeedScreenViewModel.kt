package friendly.android

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.FeedQueue
import friendly.sdk.FriendlyFeedClient
import friendly.sdk.FriendlyFilesClient
import friendly.sdk.FriendlyFriendsClient
import friendly.sdk.UserDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class FeedScreenVmState(
    val currentFeedItem: FeedItem = FeedItem.Loading,
    val isRefreshing: Boolean,
    val isNetworkError: Boolean,
    val isServerError: Boolean,
    val isAuthorizationError: Boolean,
    val isFeedEmpty: Boolean,
    // TODO: val hasNewItems: Boolean,
) {
    companion object {
        val Initial = FeedScreenVmState(
            currentFeedItem = FeedItem.Loading,
            isNetworkError = false,
            isServerError = false,
            isAuthorizationError = false,
            isFeedEmpty = false,
            isRefreshing = false,
        )
    }

    fun toUiState(): FeedScreenUiState {
        if (isServerError) {
            return FeedScreenUiState.ServerError
        }
        if (isNetworkError) {
            return FeedScreenUiState.NetworkError
        }
        if (isAuthorizationError) {
            return FeedScreenUiState.AuthorizationError
        }
        if (isFeedEmpty) {
            return FeedScreenUiState.EmptyFeed
        }
        if (isRefreshing) {
            return FeedScreenUiState.Refreshing
        }

        return FeedScreenUiState.Idle(currentFeedItem)
    }
}

class FeedScreenViewModel(
    private val sendRequest: SendFriendshipRequestUseCase,
    private val decline: DeclineFriendshipUseCase,
    private val loadFeedQueue: LoadFeedQueueUseCase,
    private val filesClient: FriendlyFilesClient,
) : ViewModel() {
    private val _state = MutableStateFlow(FeedScreenVmState.Initial)
    val state: StateFlow<FeedScreenUiState> = _state
        .map(FeedScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeedScreenUiState.Idle(FeedItem.Loading),
        )

    private val feedQueue = ArrayDeque<FeedQueue.Entry>()

    fun loadFeed() {
        Log.d(
            "Feed",
            "Start refreshing feed",
        )
        viewModelScope.launch {
            val result = loadFeedQueue()
            handleFeedQueueResult(result)
        }
    }

    fun refreshFeed() {
        Log.d(
            "Feed",
            "Start refreshing feed",
        )
        _state.clearState()
        _state.setRefreshing()
        viewModelScope.launch {
            val result = loadFeedQueue()
            handleFeedQueueResult(result)
            _state.setNotRefreshing()
            Log.d(
                "Feed",
                "Finished refreshing feed",
            )
        }
    }

    private fun handleFeedQueueResult(result: FriendlyFeedClient.QueueResult) {
        when (result) {
            is FriendlyFeedClient.QueueResult.IOError ->
                _state.setNetworkError()

            is FriendlyFeedClient.QueueResult.ServerError ->
                _state.setServerError()

            is FriendlyFeedClient.QueueResult.Unauthorized ->
                _state.setAuthorizationError()

            is FriendlyFeedClient.QueueResult.Success -> {
                val queue = result.queue
                Log.d(
                    "FEED",
                    "Loaded feed (${queue.entries.size}): $result",
                )
                feedQueue.addAll(queue.entries)
                dequeueFeed()
            }
        }
    }

    private fun dequeueFeed() {
        val pendingItem = feedQueue.removeLastOrNull()

        if (pendingItem == null) {
            _state.setEmptyFeed()
            return
        }

        val feedEntry = pendingItem.details.toFeedEntry(filesClient)
        _state.setFeedEntry(feedEntry)
    }

    fun like() {
        val currentItem = _state.currentFeedEntryOrNull() ?: return

        viewModelScope.launch {
            _state.setLoading()
            val result = sendRequest(
                userId = currentItem.id,
                accessHash = currentItem.accessHash,
            )
            Log.d("FEED", "sent friendship request")
            handleFriendshipRequestResult(currentItem, result)
        }
    }

    // TODO: add ui state for errors
    private fun handleFriendshipRequestResult(
        currentEntry: FeedItem.Entry,
        result: FriendlyFriendsClient.RequestResult,
    ) {
        Log.d("FEED", "Got friendship request result: $result")
        when (result) {
            is FriendlyFriendsClient.RequestResult.IOError,
            is FriendlyFriendsClient.RequestResult.NotFound,
            is FriendlyFriendsClient.RequestResult.ServerError,
            is FriendlyFriendsClient.RequestResult.Unauthorized,
            -> {
                _state.setFeedEntry(currentEntry)
            }

            is FriendlyFriendsClient.RequestResult.Success -> {
                Log.d(
                    "FEED",
                    "Got successful friendship request result, loading next element",
                )
                loadNext()
            }
        }
    }

    // TODO: add ui state for errors
    private fun unwrapDeclineFriendshipResult(
        currentEntry: FeedItem.Entry,
        result: FriendlyFriendsClient.DeclineResult,
    ) {
        Log.d("FEED", "Got decline friendship result: $result")
        when (result) {
            is FriendlyFriendsClient.DeclineResult.IOError,
            is FriendlyFriendsClient.DeclineResult.NotFound,
            is FriendlyFriendsClient.DeclineResult.ServerError,
            is FriendlyFriendsClient.DeclineResult.Unauthorized,
            -> {
                _state.setFeedEntry(currentEntry)
            }

            is FriendlyFriendsClient.DeclineResult.Success -> {
                Log.d(
                    "FEED",
                    "Got successful decline result, loading next element",
                )
                loadNext()
            }
        }
    }

    fun dislike() {
        val currentEntry = _state.currentFeedEntryOrNull() ?: return

        viewModelScope.launch {
            val result = decline(
                userId = currentEntry.id,
                accessHash = currentEntry.accessHash,
            )
            Log.d("FEED", "Sent decline request")
            unwrapDeclineFriendshipResult(currentEntry, result)
        }
    }

    private fun loadNext() {
        _state.clearErrors()
        _state.setLoading()

        viewModelScope.launch {
            dequeueFeed()
        }
    }
}

private typealias MutableVmStateFlow = MutableStateFlow<FeedScreenVmState>

private fun MutableVmStateFlow.currentFeedEntryOrNull(): FeedItem.Entry? =
    this.value.currentFeedItem as? FeedItem.Entry

private fun MutableVmStateFlow.setNetworkError() {
    this.update { old ->
        old.copy(isNetworkError = true)
    }
}

private fun MutableVmStateFlow.setServerError() {
    this.update { old ->
        old.copy(isServerError = true)
    }
}

private fun MutableVmStateFlow.setAuthorizationError() {
    this.update { old ->
        old.copy(isAuthorizationError = true)
    }
}

private fun MutableVmStateFlow.clearErrors() {
    this.update { old ->
        old.copy(
            isServerError = false,
            isNetworkError = false,
            isAuthorizationError = false,
        )
    }
}

private fun MutableVmStateFlow.clearState() {
    this.value = FeedScreenVmState.Initial
}

private fun MutableVmStateFlow.setLoading() {
    this.update { old ->
        old.copy(currentFeedItem = FeedItem.Loading)
    }
}

private fun MutableVmStateFlow.setRefreshing() {
    this.update { old ->
        old.copy(isRefreshing = true)
    }
}

private fun MutableVmStateFlow.setNotRefreshing() {
    this.update { old ->
        old.copy(isRefreshing = true)
    }
}

private fun MutableVmStateFlow.setEmptyFeed() {
    this.update { old ->
        old.copy(isFeedEmpty = true)
    }
}

private fun MutableVmStateFlow.setFeedEntry(entry: FeedItem.Entry) {
    this.update { old ->
        old.copy(currentFeedItem = entry)
    }
}

private fun UserDetails.toFeedEntry(
    filesClient: FriendlyFilesClient,
): FeedItem.Entry {
    val details = this
    val avatarEndpoint = details.avatar?.let { avatar ->
        filesClient.getEndpoint(avatar)
    }
    return FeedItem.Entry(
        id = details.id,
        accessHash = details.accessHash,
        nickname = details.nickname,
        description = details.description,
        interests = details.interests,
        avatarUri = avatarEndpoint?.string?.toUri(),
    )
}
