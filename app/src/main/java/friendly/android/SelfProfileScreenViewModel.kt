package friendly.android

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.FriendlyFilesClient
import friendly.sdk.InterestList
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserAccessHash
import friendly.sdk.UserDescription
import friendly.sdk.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class SelfProfileScreenVmState(
    val profile: SelfProfileScreenViewModel.UserProfile? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = true,
) {
    fun toUiState(): SelfProfileScreenUiState {
        if (isLoading) return SelfProfileScreenUiState.Loading
        if (isError) return SelfProfileScreenUiState.Error

        if (profile != null) {
            return SelfProfileScreenUiState.Present(profile)
        }

        return SelfProfileScreenUiState.Error
    }
}

class SelfProfileScreenViewModel(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val logout: LogoutUseCase,
    private val filesClient: FriendlyFilesClient,
) : ViewModel() {
    private val _state = MutableStateFlow(SelfProfileScreenVmState())
    val state: StateFlow<SelfProfileScreenUiState> = _state
        .map(SelfProfileScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SelfProfileScreenUiState.Loading,
        )

    data class UserProfile(
        val nickname: Nickname,
        val userId: UserId,
        val userAccessHash: UserAccessHash?,
        val description: UserDescription,
        val avatar: Uri?,
        val interests: InterestList,
        val socialLink: SocialLink?,
    )

    fun load() {
        _state.update { old -> old.copy(isLoading = true) }

        val auth = authStorage.getAuthOrNull()

        if (auth == null) {
            _state.update { old -> old.copy(isError = true) }
            return
        }
        viewModelScope.launch {
            val profile: UserProfile? = getUserProfile()

            if (profile == null) {
                _state.update { old ->
                    old.copy(isError = true)
                }
            }

            _state.update { old ->
                old.copy(
                    profile = profile,
                    isError = false,
                    isLoading = false,
                )
            }
        }
    }

    fun logout(onSignOut: () -> Unit) {
        viewModelScope.launch {
            if (logout()) {
                onSignOut()
            }
        }
    }

    private fun getUserProfile(): UserProfile? {
        val cache = selfProfileStorage.getCache() ?: return null

        val avatarUrl = cache.avatar?.let { avatar ->
            filesClient.getEndpoint(avatar).string.toUri()
        }

        return UserProfile(
            nickname = cache.nickname,
            userId = cache.userId,
            description = cache.description,
            avatar = avatarUrl,
            interests = cache.interests,
            socialLink = cache.socialLink,
            userAccessHash = null,
        )
    }
}
