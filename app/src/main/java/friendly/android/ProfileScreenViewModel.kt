package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.ProfileScreenViewModel.UserProfile
import friendly.sdk.Authorization
import friendly.sdk.FriendlyEndpoint
import friendly.sdk.FriendlyFilesClient
import friendly.sdk.FriendlyUsersClient
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class ProfileScreenVmState(
    val profile: UserProfile? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = false,
) {
    fun toUiState(): ProfileScreenUiState {
        if (isLoading) return ProfileScreenUiState.Loading
        if (isError) return ProfileScreenUiState.Error

        if (profile != null) {
            return ProfileScreenUiState.Present(profile)
        }

        return ProfileScreenUiState.Error
    }
}

class ProfileScreenViewModel(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val logout: LogoutUseCase,
    private val filesClient: FriendlyFilesClient,
    private val usersClient: FriendlyUsersClient,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileScreenVmState())
    val state: StateFlow<ProfileScreenUiState> = _state
        .map(ProfileScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ProfileScreenUiState.Loading,
        )

    data class UserProfile(
        val nickname: Nickname,
        val description: UserDescription,
        val avatar: FriendlyEndpoint?,
        val interests: List<Interest>,
    )

    fun load(source: ProfileScreenSource) {
        _state.update { old -> old.copy(isLoading = true) }

        val auth = authStorage.getAuthOrNull()

        if (auth == null) {
            _state.update { old ->
                old.copy(isError = true)
            }
            return
        }
        viewModelScope.launch {
            val profile: UserProfile? = getUserProfile(auth, source)

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
        logout()
        onSignOut()
    }

    private suspend fun getUserProfile(
        auth: Authorization,
        source: ProfileScreenSource,
    ): UserProfile? {
        when (source) {
            is ProfileScreenSource.SelfProfile -> {
                val (nickname, description, avatar, interests) =
                    selfProfileStorage.getCache() ?: return null

                val avatarUrl = avatar?.let { avatar ->
                    filesClient.getEndpoint(avatar)
                }

                return UserProfile(nickname, description, avatarUrl, interests)
            }

            is ProfileScreenSource.FriendProfile -> {
                val result = usersClient
                    .details(auth, source.id, source.accessHash)

                val details = when (result) {
                    is FriendlyUsersClient.DetailsResult.IOError,
                    is FriendlyUsersClient.DetailsResult.ServerError,
                    is FriendlyUsersClient.DetailsResult.Unauthorized,
                    -> return null

                    is FriendlyUsersClient.DetailsResult.Success ->
                        result.details
                }

                return UserProfile(
                    nickname = details.nickname,
                    description = details.description,
                    avatar = details.avatar?.let { avatar ->
                        filesClient.getEndpoint(avatar)
                    },
                    interests = details.interests,
                )
            }
        }
    }
}
