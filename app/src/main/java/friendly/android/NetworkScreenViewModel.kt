package friendly.android

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.NetworkScreenUiState.Success.FriendItem
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyFriendsClient
import friendly.sdk.FriendlyNetworkClient
import friendly.sdk.NetworkDetails
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class NetworkScreenVmState(
    val isNetworkFailure: Boolean,
    val isAuthFailure: Boolean,
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val friends: List<FriendItem>?,
) {
    fun toUiState(): NetworkScreenUiState {
        if (isLoading) {
            return NetworkScreenUiState.Loading(
                isRefreshing = isRefreshing,
            )
        }
        if (isNetworkFailure) {
            return NetworkScreenUiState.NetworkFailure(
                isRefreshing = isRefreshing,
            )
        }
        if (isAuthFailure) {
            return NetworkScreenUiState.AuthFailure(
                isRefreshing = isRefreshing,
            )
        }
        if (friends == null) {
            return NetworkScreenUiState.Other(
                isRefreshing = isRefreshing,
            )
        }
        if (friends.isEmpty()) {
            return NetworkScreenUiState.NoFriends(
                isRefreshing = isRefreshing,
            )
        }
        return NetworkScreenUiState.Success(
            friends = friends,
            isRefreshing = isRefreshing,
        )
    }

    companion object {
        val Initial = NetworkScreenVmState(
            isNetworkFailure = false,
            isAuthFailure = false,
            isLoading = true,
            isRefreshing = false,
            friends = null,
        )

        val NetworkError = NetworkScreenVmState(
            isNetworkFailure = true,
            isAuthFailure = false,
            isLoading = false,
            isRefreshing = false,
            friends = null,
        )

        val AuthError = NetworkScreenVmState(
            isNetworkFailure = false,
            isAuthFailure = true,
            isLoading = false,
            isRefreshing = false,
            friends = null,
        )
    }
}

sealed interface NetworkScreenEvent {
    data object FriendLinkInvalid : NetworkScreenEvent
    data object FriendLinkExpired : NetworkScreenEvent
    data object AddFriendByLinkFailure : NetworkScreenEvent
    data object AddFriendByLinkSuccess : NetworkScreenEvent
}

class NetworkScreenViewModel(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
    private val addFriendUseCase: AddFriendUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(NetworkScreenVmState.Initial)

    val state = _state
        .map(NetworkScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = NetworkScreenUiState.Loading(),
        )

    private val _events = MutableSharedFlow<NetworkScreenEvent>()

    val events: SharedFlow<NetworkScreenEvent> = _events.shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
    )

    fun initialize() {
        if (_state.value.friends == null) {
            _state.update { NetworkScreenVmState.Initial }
        }
        loadFriends()
    }

    fun retry() {
        _state.update { NetworkScreenVmState.Initial }
        loadFriends()
    }

    fun refresh() {
        _state.update { old ->
            old.copy(isRefreshing = true)
        }
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            val auth = authStorage.getAuth()
            val newState = when (val result = client.network.details(auth)) {
                is FriendlyNetworkClient.DetailsResult.IOError -> {
                    NetworkScreenVmState.NetworkError
                }

                is FriendlyNetworkClient.DetailsResult.ServerError -> {
                    NetworkScreenVmState.NetworkError
                }

                is FriendlyNetworkClient.DetailsResult.Unauthorized -> {
                    NetworkScreenVmState.AuthError
                }

                is FriendlyNetworkClient.DetailsResult.Success -> {
                    val friendItems = mapDetailsToFriendItems(result.details)
                    NetworkScreenVmState(
                        isNetworkFailure = false,
                        isAuthFailure = false,
                        isLoading = false,
                        isRefreshing = false,
                        friends = friendItems,
                    )
                }
            }
            _state.update { newState }
        }
    }

    private fun mapDetailsToFriendItems(
        details: NetworkDetails,
    ): List<FriendItem> = details.friends.map { friend ->
        FriendItem(
            avatar = friend.avatar?.let { avatar ->
                client.files.getEndpoint(avatar).string.toUri()
            },
            nickname = friend.nickname,
            id = friend.id,
            accessHash = friend.accessHash,
        )
    }

    fun addFriendByLink(link: String) {
        viewModelScope.launch {
            if (!FriendInviteLink.validate(link)) {
                _events.emit(FriendLinkInvalid)
            }

            val inviteLink = FriendInviteLink(link)

            val friendToken = inviteLink.asFriendToken()

            if (friendToken == null) {
                _events.emit(FriendLinkInvalid)
                return@launch
            }

            val addFriendResult = addFriendUseCase(friendToken)

            when (addFriendResult) {
                is FriendlyFriendsClient.AddResult.FriendTokenExpired -> {
                    _events.emit(FriendLinkExpired)
                }

                is FriendlyFriendsClient.AddResult.Success -> {
                    _events.emit(AddFriendByLinkSuccess)
                    refresh()
                }

                is FriendlyFriendsClient.AddResult.IOError,
                is FriendlyFriendsClient.AddResult.ServerError,
                is FriendlyFriendsClient.AddResult.Unauthorized,
                -> {
                    _events.emit(AddFriendByLinkFailure)
                }
            }
        }
    }
}
