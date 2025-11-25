package friendly.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import friendly.android.ProfileScreenViewModel.UserProfile
import friendly.sdk.FriendlyEndpoint
import friendly.sdk.FriendlyFilesClient
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

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
        val avatar: FriendlyEndpoint,
        val interests: List<Interest>,
    )

    fun load(source: ProfileScreenSource) {
        _state.update { old -> old.copy(isLoading = true) }

        val profile: UserProfile? = getUserProfile(source)

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

    private fun getUserProfile(source: ProfileScreenSource): UserProfile? {
        when (source) {
            is ProfileScreenSource.SelfProfile -> {
                val (nickname, description, avatar, interests) =
                    selfProfileStorage.getCache() ?: return null

                val avatarUrl = filesClient.getEndpoint(avatar)

                return UserProfile(nickname, description, avatarUrl, interests)
            }
        }
    }
}

sealed interface ProfileScreenSource {
    data object SelfProfile : ProfileScreenSource
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    source: ProfileScreenSource,
    onShare: () -> Unit,
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
                title = { Text(text = "Profile") },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        when (val state = state) {
            is ProfileScreenUiState.Loading -> {
                CircularProgressIndicator(Modifier.padding(innerPadding))
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
                    text = "An error occurred during loading the profile screen",
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
                println(state.profile.avatar.string)
                AsyncImage(
                    model = state.profile.avatar.string,
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.ic_photo_camera),
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
            modifier = Modifier.fillMaxWidth(),
        ) {
            state.profile.interests.forEach { interest ->
                SuggestionChip(
                    onClick = {},
                    interactionSource = null,
                    label = {
                        Text(interest.string)
                    },
                    modifier = Modifier.disablePointerInput(),
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
