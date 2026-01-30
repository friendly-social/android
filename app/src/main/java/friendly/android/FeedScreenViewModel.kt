package friendly.android

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.FeedQueue
import friendly.sdk.FriendlyFeedClient.QueueResult
import friendly.sdk.FriendlyFilesClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class FeedScreenVmState(
    val currentFeedItems: List<FeedEntry> = emptyList(),
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val isNetworkError: Boolean,
    val isServerError: Boolean,
) {
    companion object {
        val Initial = FeedScreenVmState(
            currentFeedItems = emptyList(),
            isNetworkError = false,
            isServerError = false,
            isLoading = true,
            isRefreshing = false,
        )

        val NetworkError = FeedScreenVmState(
            currentFeedItems = emptyList(),
            isNetworkError = true,
            isServerError = false,
            isLoading = false,
            isRefreshing = false,
        )

        val ServerError = FeedScreenVmState(
            currentFeedItems = emptyList(),
            isNetworkError = false,
            isServerError = true,
            isLoading = false,
            isRefreshing = false,
        )
    }

    fun toUiState(): FeedScreenUiState {
        if (isServerError) {
            return FeedScreenUiState.ServerError(isRefreshing)
        }
        if (isNetworkError) {
            return FeedScreenUiState.NetworkError(isRefreshing)
        }
        if (isLoading) {
            return FeedScreenUiState.Loading
        }
        if (currentFeedItems.isEmpty()) {
            return FeedScreenUiState.EmptyFeed(isRefreshing)
        }
        return FeedScreenUiState.Idle(currentFeedItems)
    }
}

class FeedScreenViewModel(
    private val like: SendFriendshipRequestUseCase,
    private val dislike: DeclineFriendshipUseCase,
    private val loadFeedQueue: LoadFeedQueueUseCase,
    private val filesClient: FriendlyFilesClient,
) : ViewModel() {
    private val _state = MutableStateFlow(FeedScreenVmState.Initial)

    val state: StateFlow<FeedScreenUiState> = _state
        .map(FeedScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeedScreenUiState.Idle(emptyList()),
        )

    fun loadInitial() {
        if (_state.value.currentFeedItems.isNotEmpty()) return

        Log.d("Feed", "loadInitial start")

        viewModelScope.launch {
            val feedQueueResult = loadFeedQueue()
            _state.setLoadFeedQueueState(
                filesClient = filesClient,
                result = feedQueueResult,
                onAuthError = {
                    TODO("Unhandled Authorization error")
                },
            )
            Log.d("Feed", "loadInitial finished")
        }
    }

    /**
     * Reloads feed from completely new state
     */
    fun retry() {
        _state.update { FeedScreenVmState.Initial }

        Log.d("Feed", "retry start")

        viewModelScope.launch {
            val feedQueueResult = loadFeedQueue()
            _state.setLoadFeedQueueState(
                filesClient = filesClient,
                result = feedQueueResult,
                onAuthError = {
                    TODO("Unhandled Authorization error")
                },
            )
        }
    }

    fun refresh() {
        if (_state.value.isLoading) return

        _state.setRefreshing()

        viewModelScope.launch {
            val feedQueueResult = loadFeedQueue()
            _state.setRefreshFeedState(
                filesClient = filesClient,
                result = feedQueueResult,
                onAuthError = {
                    TODO("Unhandled Authorization error")
                },
            )
        }
    }

    fun like(entry: FeedEntry) {
        viewModelScope.launch {
            Log.d("Feed", "Sent like: $entry")
            like(
                userId = entry.id,
                accessHash = entry.accessHash,
            )
            Log.d("Feed", "Received like response: $entry")
        }
    }

    fun dislike(entry: FeedEntry) {
        viewModelScope.launch {
            Log.d("Feed", "Sent dislike: $entry")
            dislike(
                userId = entry.id,
                accessHash = entry.accessHash,
            )
            Log.d("Feed", "Received dislike response: $entry")
        }
    }
}

private fun MutableVmStateFlow.setRefreshFeedState(
    filesClient: FriendlyFilesClient,
    result: QueueResult,
    onAuthError: () -> Unit,
) {
    when (result) {
        is QueueResult.IOError -> this.setNetworkError()

        is QueueResult.ServerError -> this.setServerError()

        is QueueResult.Unauthorized -> onAuthError()

        is QueueResult.Success -> {
            this.update { old ->
                old.copy(
                    isRefreshing = false,
                    isNetworkError = false,
                    isServerError = false,
                    currentFeedItems = result.queue.entries.map { entry ->
                        entry.toFeedEntry(filesClient)
                    },
                )
            }
        }
    }
}

private fun MutableVmStateFlow.setLoadFeedQueueState(
    filesClient: FriendlyFilesClient,
    result: QueueResult,
    onAuthError: () -> Unit,
) {
    when (result) {
        is QueueResult.IOError -> this.setNetworkError()

        is QueueResult.ServerError -> this.setServerError()

        is QueueResult.Unauthorized -> onAuthError()

        is QueueResult.Success -> {
            this.update { old ->
                old.copy(
                    isLoading = false,
                    currentFeedItems = result.queue.entries.map { entry ->
                        entry.toFeedEntry(filesClient)
                    },
                )
            }
        }
    }
}

private typealias MutableVmStateFlow = MutableStateFlow<FeedScreenVmState>

private fun MutableVmStateFlow.setNetworkError() {
    this.update { FeedScreenVmState.NetworkError }
}

private fun MutableVmStateFlow.setServerError() {
    this.update { FeedScreenVmState.ServerError }
}

private fun MutableVmStateFlow.setRefreshing() {
    this.update { old ->
        old.copy(
            currentFeedItems = emptyList(),
            isRefreshing = true,
        )
    }
}

private fun FeedQueue.Entry.toFeedEntry(
    filesClient: FriendlyFilesClient,
): FeedEntry {
    val entry = this
    val details = entry.details
    val avatarEndpoint = details.avatar?.let { avatar ->
        filesClient.getEndpoint(avatar)
    }
    return FeedEntry(
        id = details.id,
        accessHash = details.accessHash,
        nickname = details.nickname,
        description = details.description,
        interests = details.interests,
        avatarUri = avatarEndpoint?.string?.toUri(),
        isRequest = entry.isRequest,
        isExtendedNetwork = entry.isExtendedNetwork,
    )
}
