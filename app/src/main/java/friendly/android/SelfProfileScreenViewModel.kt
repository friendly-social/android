package friendly.android

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.query.QueryClient
import friendly.query.QueryFetchResult
import friendly.query.QueryKey
import friendly.query.query
import friendly.sdk.Email
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.InterestList
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserAccessHash
import friendly.sdk.UserDescription
import friendly.sdk.UserDetails
import friendly.sdk.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private data class SelfProfileScreenVmState(
    val isLoggingOut: Boolean = false,
) {
    fun toUiState(): SelfProfileScreenUiState =
        SelfProfileScreenUiState.Idle(isLoggingOut)
}

class SelfProfileScreenViewModel(
    private val authStorage: AuthStorage,
    private val logout: LogoutUseCase,
    private val client: FriendlyClient,
    private val db: FriendlyDatabase,
) : ViewModel() {
    private val _state = MutableStateFlow(SelfProfileScreenVmState())

    val state: StateFlow<SelfProfileScreenUiState> = _state
        .map(SelfProfileScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SelfProfileScreenUiState.Idle(false),
        )

    data class UserProfile(
        val nickname: Nickname,
        val userId: UserId,
        val userAccessHash: UserAccessHash?,
        val description: UserDescription,
        val avatar: Uri?,
        val interests: InterestList,
        val socialLink: SocialLink?,
        val email: Email?,
    )

    val queryClient = QueryClient<UserDetails>(
        cache = roomQueryCache(
            dao = db.queryItemDao(),
            itemSerializable = { it.serializable() },
            itemTyped = { it.typed() },
        ),
        scope = viewModelScope,
    )

    val profileQuery = queryClient.query(
        key = QueryKey("self-profile"),
        retriesOnRefresh = 3,
        retryDelay = 300.milliseconds,
        fetch = {
            when (val result = client.users.details(authStorage.getAuth())) {
                is IOError,
                is ServerError,
                is Unauthorized,
                -> {
                    QueryFetchResult.Failure
                }

                is Success -> {
                    QueryFetchResult.Success(value = result.details)
                }
            }
        },
    )

    fun logout(onSignOut: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoggingOut = true) }

            if (logout()) {
                _state.update { it.copy(isLoggingOut = false) }
                onSignOut()
            }
        }
    }

    fun fileUri(descriptor: FileDescriptor): Uri =
        client.files.getEndpoint(descriptor).string.toUri()
}
