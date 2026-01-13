package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.NetworkScreenUiState.Success.FriendItem
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyNetworkClient
import friendly.sdk.NetworkDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class NetworkScreenVmState(
    val isNetworkFailure: Boolean,
    val isAuthFailure: Boolean,
    val isLoading: Boolean,
    val friends: List<FriendItem>?,
) {
    fun toUiState(): NetworkScreenUiState {
        if (isLoading) return NetworkScreenUiState.Loading
        if (isNetworkFailure) return NetworkScreenUiState.NetworkFailure
        if (isAuthFailure) return NetworkScreenUiState.AuthFailure
        if (friends == null) return NetworkScreenUiState.Other
        return NetworkScreenUiState.Success(friends)
    }

    companion object {
        val Initial = NetworkScreenVmState(
            isNetworkFailure = false,
            isAuthFailure = false,
            isLoading = true,
            friends = null,
        )

        val NetworkError = NetworkScreenVmState(
            isNetworkFailure = true,
            isAuthFailure = false,
            isLoading = false,
            friends = null,
        )

        val AuthError = NetworkScreenVmState(
            isNetworkFailure = false,
            isAuthFailure = true,
            isLoading = false,
            friends = null,
        )
    }
}

class NetworkScreenViewModel(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
) : ViewModel() {
    private val _state = MutableStateFlow(NetworkScreenVmState.Initial)

    val state = _state
        .map(NetworkScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = NetworkScreenUiState.Loading,
        )

    fun getFriends() {
        _state.update { NetworkScreenVmState.Initial }

        viewModelScope.launch {
            val auth = authStorage.getAuthOrNull() ?: error("no auth bro")
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
                client.files.getEndpoint(avatar)
            }?.string,
            nickname = friend.nickname.string,
            id = friend.id,
            accessHash = friend.accessHash,
        )
    }
}
