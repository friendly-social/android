package friendly.android

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import friendly.android.AvatarUploadUseCase.UploadingPercentage
import friendly.android.EditProfileScreen.EditProfileScreenUiState
import friendly.android.EditProfileScreen.EditProfileScreenUiState.AvatarUiState
import friendly.android.EditProfileScreen.SnackbarEvent
import friendly.android.EditProfileScreenVmState.AvatarState
import friendly.android.EditProfileScreenVmState.CurrentProfile
import friendly.sdk.Field
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyFilesClient
import friendly.sdk.FriendlyUsersClient
import friendly.sdk.Interest
import friendly.sdk.InterestList
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserDescription
import friendly.sdk.UserId
import friendly.sdk.typed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import friendly.android.FriendlyNavGraph.Home.EditProfile as EditProfileRoute

private data class EditProfileScreenVmState(
    val userId: UserId,
    val initialNickname: Nickname,
    val currentProfile: CurrentProfile,
    val availableInterests: List<Interest>,
    val isSaving: Boolean,
    val isSavable: Boolean,
    val hasChanges: Boolean,
) {
    fun mapToUiState(): EditProfileScreenUiState {
        if (isSaving) {
            return EditProfileScreenUiState.Saving(
                userId = userId,
                initialNickname = initialNickname,
            )
        }

        return EditProfileScreenUiState.Edit(
            userId = userId,
            initialNickname = initialNickname,
            profile = EditProfileScreenUiState.CurrentProfileUiState(
                nickname = currentProfile.nickname,
                description = currentProfile.description,
                avatar = when (val avatarState = currentProfile.avatar) {
                    is Initial -> AvatarUiState.Present(avatarState.uri)
                    is Uploaded -> AvatarUiState.Present(avatarState.uri)
                    is None -> AvatarUiState.Present(null)
                    is Uploading -> AvatarUiState.Uploading(
                        avatarState.percentage,
                    )
                },
                interests = currentProfile.interests,
                socialLink = currentProfile.socialLink,
            ),
            isSavable = isSavable,
            hasChanges = hasChanges,
            availableInterests = availableInterests,
        )
    }

    data class CurrentProfile(
        val nickname: ValidatableField<String>,
        val description: ValidatableField<String>,
        val avatar: AvatarState,
        val interests: List<Interest>,
        val socialLink: ValidatableField<String?>,
    )

    sealed interface AvatarState {
        data class Initial(val uri: Uri?) : AvatarState

        data object None : AvatarState

        data class Uploaded(val fileDescriptor: FileDescriptor, val uri: Uri) :
            AvatarState

        data class Uploading(val percentage: UploadingPercentage) : AvatarState
    }
}

private data class InitialProfile(
    val nickname: Nickname,
    val description: UserDescription,
    val socialLink: SocialLink?,
    val avatar: Uri?,
    val interests: InterestList,
)

class EditProfileScreenViewModel(
    savedStateHandle: SavedStateHandle,
    private val filesClient: FriendlyFilesClient,
    private val edit: EditProfileUseCase,
    private val uploadAvatar: AvatarUploadUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<EditProfileRoute>(
        typeMap = EditProfileTypeMap,
    )

    private val initialProfile = InitialProfile(
        nickname = route.nickname.typed(),
        description = route.description.typed(),
        socialLink = route.socialLink?.typed(),
        interests = InterestList.orThrow(route.interests.typed()),
        avatar = route.avatarUri?.toUri(),
    )

    private val _state = MutableStateFlow(
        EditProfileScreenVmState(
            userId = route.userId.typed(),
            initialNickname = initialProfile.nickname,
            currentProfile = CurrentProfile(
                nickname = ValidatableField(
                    initialProfile.nickname.string,
                ),
                description = ValidatableField(
                    initialProfile.description.string,
                ),
                avatar = AvatarState.Initial(
                    initialProfile.avatar,
                ),
                interests = initialProfile.interests.raw,
                socialLink = ValidatableField(
                    initialProfile.socialLink?.string,
                ),
            ),
            availableInterests = interests,
            isSavable = false,
            isSaving = false,
            hasChanges = false,
        ),
    )

    val state: StateFlow<EditProfileScreenUiState> = _state
        .map(EditProfileScreenVmState::mapToUiState)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            EditProfileScreenUiState.Saving(
                userId = route.userId.typed(),
                initialNickname = route.nickname.typed(),
            ),
        )

    private val _events = MutableSharedFlow<SnackbarEvent>()

    val events: SharedFlow<SnackbarEvent> = _events.shareIn(
        scope = viewModelScope,
        started = Eagerly,
    )

    fun toggleInterest(interest: Interest) {
        val picked = interest in _state.value.currentProfile.interests
        val currentProfile = _state.value.currentProfile
        val newProfile = if (picked) {
            currentProfile.copy(
                interests = currentProfile.interests.minus(interest),
            )
        } else {
            currentProfile.copy(
                interests = currentProfile.interests.plus(interest),
            )
        }
        _state.updateCurrentProfile(newProfile, initialProfile)
    }

    fun onNickname(string: String) {
        val isValid = Nickname.validate(string)
        val newProfile = _state.value.currentProfile.copy(
            nickname = ValidatableField(string, isValid),
        )
        _state.updateCurrentProfile(newProfile, initialProfile)
    }

    fun onSocialLink(string: String) {
        val isValid = SocialLink.validate(string)
        val newProfile = _state.value.currentProfile.copy(
            socialLink = ValidatableField(string.ifBlank { null }, isValid),
        )
        _state.updateCurrentProfile(newProfile, initialProfile)
    }

    fun onDescription(string: String) {
        val isValid = UserDescription.validate(string)
        val newProfile = _state.value.currentProfile.copy(
            description = ValidatableField(string, isValid),
        )
        _state.updateCurrentProfile(newProfile, initialProfile)
    }

    fun pickAvatar(uri: Uri?) {
        uri ?: return

        _state.setStartAvatarUploadingState(
            percentage = UploadingPercentage(0f),
            initialProfile = initialProfile,
        )

        viewModelScope.launch {
            val uploadingResult = uploadAvatar(
                avatarUri = uri,
                block = { flow ->
                    flow.collect { update ->
                        _state.setStartAvatarUploadingState(
                            percentage = update,
                            initialProfile = initialProfile,
                        )
                    }
                },
            )
            when (uploadingResult) {
                is Success -> {
                    _state.setStartAvatarUploadedState(
                        fileDescriptor = uploadingResult.fileDescriptor,
                        initialProfile = initialProfile,
                        filesClient = filesClient,
                    )
                }

                else -> {
                    _state.resetToInitialAvatarState(initialProfile)
                    _events.emit(SnackbarEvent.AvatarUploadingError)
                }
            }
        }
    }

    fun removeAvatar() {
        val newProfile = _state.value.currentProfile.copy(avatar = None)
        _state.updateCurrentProfile(newProfile, initialProfile)
    }

    fun save(onSuccess: () -> Unit) {
        val currentProfile = _state.value.currentProfile

        if (!currentProfile.isSavable(initialProfile)) return

        val nickname = fieldElseNull(
            initial = initialProfile.nickname,
            new = Nickname.orThrow(currentProfile.nickname.value),
        )
        val description = fieldElseNull(
            initial = initialProfile.description,
            new = UserDescription.orThrow(currentProfile.description.value),
        )
        val socialLink: Field<SocialLink?>? = fieldElseNull(
            initial = initialProfile.socialLink,
            new = currentProfile.socialLink.value?.let(SocialLink::orThrow),
        )
        val interests = fieldElseNull(
            initial = initialProfile.interests,
            new = InterestList.orThrow(currentProfile.interests),
        )
        val newAvatar = when (val avatarState = currentProfile.avatar) {
            is Uploaded -> Field(avatarState.fileDescriptor)
            is None -> Field(null)
            else -> null
        }

        viewModelScope.launch {
            _state.update { old -> old.copy(isSaving = true) }

            val editResult = edit(
                newNickname = nickname,
                newDescription = description,
                newInterests = interests,
                newAvatar = newAvatar,
                newSocialLink = socialLink,
            )

            when (editResult) {
                is IOError,
                is ServerError,
                is Unauthorized,
                -> {
                    _events.emit(SnackbarEvent.SavingError)
                    _state.update { old -> old.copy(isSaving = false) }
                }

                is FriendlyUsersClient.EditResult.Success -> onSuccess()
            }
        }
    }
}

private typealias EditProfileScreenVmStateFlow =
    MutableStateFlow<EditProfileScreenVmState>

private fun EditProfileScreenVmStateFlow.setStartAvatarUploadingState(
    percentage: UploadingPercentage,
    initialProfile: InitialProfile,
) {
    val currentProfile = this.value.currentProfile
    val newProfile = currentProfile.copy(
        avatar = AvatarState.Uploading(percentage),
    )
    updateCurrentProfile(newProfile, initialProfile)
}

private fun EditProfileScreenVmStateFlow.setStartAvatarUploadedState(
    fileDescriptor: FileDescriptor,
    initialProfile: InitialProfile,
    filesClient: FriendlyFilesClient,
) {
    val currentProfile = this.value.currentProfile
    val endpoint = filesClient.getEndpoint(
        descriptor = fileDescriptor,
    )
    val newProfile = currentProfile.copy(
        avatar = AvatarState.Uploaded(
            fileDescriptor = fileDescriptor,
            uri = endpoint.string.toUri(),
        ),
    )
    updateCurrentProfile(newProfile, initialProfile)
}

private fun EditProfileScreenVmStateFlow.resetToInitialAvatarState(
    initialProfile: InitialProfile,
) {
    val currentProfile = this.value.currentProfile
    val newProfile = currentProfile.copy(
        avatar = AvatarState.Initial(initialProfile.avatar),
    )
    updateCurrentProfile(newProfile, initialProfile)
}

private fun <T> fieldElseNull(initial: T, new: T): Field<T>? {
    if (initial != new) {
        return Field(new)
    }
    return null
}

private fun EditProfileScreenVmStateFlow.updateCurrentProfile(
    new: CurrentProfile,
    initialProfile: InitialProfile,
) {
    val oldState = this
    oldState.update { old ->
        old.copy(
            currentProfile = new,
            isSavable = new.isSavable(initialProfile),
            hasChanges = new.hasChanges(initialProfile),
        )
    }
}

private fun CurrentProfile.isSavable(initialProfile: InitialProfile): Boolean {
    val newProfile = this
    return newProfile.hasChanges(initialProfile) && newProfile.validateAll()
}

private fun CurrentProfile.hasChanges(
    initialProfile: InitialProfile,
): Boolean {
    val profile = this
    return listOf(
        initialProfile.nickname.string == profile.nickname.value,
        initialProfile.description.string == profile.description.value,
        initialProfile.socialLink?.string == profile.socialLink.value,
        initialProfile.interests.raw == profile.interests,
        profile.avatar !is Initial,
    ).any { it }
}

private fun CurrentProfile.validateAll(): Boolean {
    val profile = this
    val nicknameIsValid = profile.nickname.isValid
    val descriptionIsValid = profile.description.isValid
    val socialLinkIsValid = profile.socialLink.isValid
    val interestsAreValid = profile.interests.isNotEmpty()

    val allIsValid = listOf(
        nicknameIsValid,
        descriptionIsValid,
        socialLinkIsValid,
        interestsAreValid,
    ).all { it }

    return allIsValid
}
