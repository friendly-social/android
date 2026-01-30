package friendly.android

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.ProfileScreenViewModel.UserProfile
import friendly.sdk.Authorization
import friendly.sdk.FriendlyFilesClient
import friendly.sdk.FriendlyFriendsClient
import friendly.sdk.FriendlyUsersClient
import friendly.sdk.Interest
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

private data class ProfileScreenVmState(
    val profile: UserProfile? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = true,
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
    private val friendsClient: FriendlyFriendsClient,
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
        val userId: UserId,
        val userAccessHash: UserAccessHash?,
        val description: UserDescription,
        val avatar: Uri?,
        val interests: InterestList,
        val socialLink: SocialLink?,
    )

    fun load(source: ProfileScreenSource) {
        _state.update { old -> old.copy(isLoading = true) }

        val auth = authStorage.getAuthOrNull()

        if (auth == null) {
            _state.update { old -> old.copy(isError = true) }
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

    fun removeFriend(onSuccess: () -> Unit) {
        val authorization = authStorage.getAuthOrNull() ?: return
        val userId = _state.value.profile?.userId ?: return
        val userAccessHash = _state.value.profile?.userAccessHash ?: return

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = friendsClient.decline(
                authorization = authorization,
                userId = userId,
                userAccessHash = userAccessHash,
            )

            _state.update { it.copy(isLoading = false) }

            if (result is FriendlyFriendsClient.DeclineResult.Success) {
                onSuccess()
            }
        }
    }

    private suspend fun getUserProfile(
        auth: Authorization,
        source: ProfileScreenSource,
    ): UserProfile? {
        when (source) {
            is ProfileScreenSource.SelfProfile -> {
                val cache = selfProfileStorage.getCache() ?: return null

                val avatarUrl = cache.avatar?.let { avatar ->
                    Uri.parse(filesClient.getEndpoint(avatar).string)
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
                    userId = details.id,
                    description = details.description,
                    avatar = details.avatar?.let { avatar ->
                        Uri.parse(filesClient.getEndpoint(avatar).string)
                    },
                    interests = details.interests,
                    socialLink = details.socialLink,
                    userAccessHash = details.accessHash,
                )
            }
        }
    }
}
