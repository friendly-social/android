package friendly.android

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import friendly.android.ProfileScreenViewModel.UserProfile
import friendly.sdk.Authorization
import friendly.sdk.FriendlyEndpoint
import friendly.sdk.FriendlyFilesClient
import friendly.sdk.FriendlyUsersClient
import friendly.sdk.Interest
import friendly.sdk.Nickname
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

data class ProfileScreenVmState(
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

sealed interface ProfileScreenUiState {
    data class Present(val profile: UserProfile) : ProfileScreenUiState

    data object Loading : ProfileScreenUiState

    data object Error : ProfileScreenUiState
}

class ProfileScreenViewModel(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
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

        val auth = authStorage.getAuth()

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

    private suspend fun getUserProfile(
        auth: Authorization,
        source: ProfileScreenSource,
    ): UserProfile? {
        when (source) {
            is ProfileScreenSource.SelfProfile -> {
                val (nickname, description, avatar, interests) =
                    selfProfileStorage.getCache() ?: return null

                val avatarUrl = filesClient.getEndpoint(avatar)

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

sealed interface ProfileScreenSource {
    data object SelfProfile : ProfileScreenSource
    data class FriendProfile(val id: UserId, val accessHash: UserAccessHash) :
        ProfileScreenSource
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun ProfileScreen(
    source: ProfileScreenSource,
    onShare: () -> Unit,
    onHome: () -> Unit,
    vm: ProfileScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.load(source)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    if (source is ProfileScreenSource.FriendProfile) {
                        IconButton(onClick = onHome) {
                            Icon(
                                painter =
                                painterResource(R.drawable.ic_arrow_back),
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = {
                    if (source is ProfileScreenSource.SelfProfile) {
                        IconButton(onClick = onShare) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        when (val state = state) {
            is ProfileScreenUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    LoadingIndicator(Modifier.size(48.dp))
                }
            }

            is ProfileScreenUiState.Present -> {
                LoadedProfileState(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(innerPadding),
                )
            }

            is ProfileScreenUiState.Error ->
                Text(
                    text = stringResource(R.string.profile_screen_error_text),
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }
}

@Composable
fun LoadedProfileState(
    state: ProfileScreenUiState.Present,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(128.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AsyncImage(
                    model = state.profile.avatar?.string,
                    contentDescription = null,
                    error = painterResource(R.drawable.ic_person),
                    placeholder = painterResource(R.drawable.ic_person),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape),
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = state.profile.nickname.string,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            state.profile.interests.forEach { interest ->
                Text(
                    text = interest.string,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            color = Color.pastelFromString(
                                string = interest.string,
                                useDark = isSystemInDarkTheme(),
                            ),
                        )
                        .padding(4.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = state.profile.description.string,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun Modifier.disablePointerInput(): Modifier = this.pointerInput(
    key1 = Unit,
    block = {
        awaitEachGesture {
            val pointerEvent = this
                .awaitPointerEvent(
                    pass = PointerEventPass.Initial,
                )
            for (change in pointerEvent.changes) {
                change.consume()
            }
        }
    },
)
