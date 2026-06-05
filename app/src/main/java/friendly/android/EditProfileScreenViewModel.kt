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
import friendly.android.EditProfileScreen.Event
import friendly.android.EditProfileScreen.Event.SnackbarEvent
import friendly.android.EditProfileScreenVmState.AvatarState
import friendly.android.EditProfileScreenVmState.CurrentProfile
import friendly.android.UnlinkEmailUseCase.UnlinkResult
import friendly.sdk.Email
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
                email = currentProfile.email,
            ),
            isSavable = isSavable,
            hasChanges = hasChanges,
            availableInterests = availableInterests,
        )
    }

    data class CurrentProfile(
        val nickname: ValidatableField<String>,
        val description: ValidatableField<String>,
        val email: EmailState,
        val avatar: AvatarState,
        val interests: List<Interest>,
        val socialLink: ValidatableField<String?>,
    ) {
        companion object {
            fun initial(initialProfile: InitialProfile): CurrentProfile =
                CurrentProfile(
                    nickname = ValidatableField(initialProfile.nickname.string),
                    description = ValidatableField(
                        initialProfile.description.string,
                    ),
                    avatar = AvatarState.Initial(initialProfile.avatar),
                    interests = initialProfile.interests.raw,
                    socialLink = ValidatableField(
                        initialProfile.socialLink?.string,
                    ),
                    email = EmailState(
                        field = ValidatableField(
                            value = initialProfile.email?.string,
                        ),
                        isUnlinkable = initialProfile.email != null,
                    ),
                )
        }
    }

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
    val email: Email?,
    val avatar: Uri?,
    val interests: InterestList,
) {
    companion object {
        fun fromRoute(route: EditProfileRoute): InitialProfile = InitialProfile(
            nickname = route.nickname.typed(),
            description = route.description.typed(),
            socialLink = route.socialLink?.typed(),
            email = route.email?.typed(),
            interests = InterestList.orThrow(route.interests.typed()),
            avatar = route.avatarUri?.toUri(),
        )
    }
}

class EditProfileScreenViewModel(
    savedStateHandle: SavedStateHandle,
    private val link: LinkEmailUseCase,
    private val unlink: UnlinkEmailUseCase,
    private val filesClient: FriendlyFilesClient,
    private val edit: EditProfileUseCase,
    private val uploadAvatar: AvatarUploadUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<EditProfileRoute>(
        typeMap = EditProfileTypeMap,
    )

    private val initialProfile = InitialProfile.fromRoute(route)

    private val _state = MutableStateFlow(
        EditProfileScreenVmState(
            userId = route.userId.typed(),
            initialNickname = initialProfile.nickname,
            currentProfile = CurrentProfile.initial(initialProfile),
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

    private val _events = MutableSharedFlow<Event>()

    val events: SharedFlow<Event> = _events.shareIn(
        scope = viewModelScope,
        started = Eagerly,
    )

    fun toggleInterest(interest: Interest) {
        _state.toggleInterest(interest, initialProfile)
    }

    fun onNickname(string: String) {
        _state.updateNickname(string, initialProfile)
    }

    fun onSocialLink(string: String) {
        _state.updateSocialLink(string, initialProfile)
    }

    fun onDescription(string: String) {
        _state.updateDescription(string, initialProfile)
    }

    fun sendEmailVerificationCode() {
        viewModelScope.launch {
            _state.updateEmailState { it.copy(isSending = true) }
            val email = _state.value.currentProfile.email.field.value
                ?.let(Email::orThrow)
                ?: error("Email address can't be null")
            link(email)
            val event = Event.VerificationCodeSent(email)
            _events.emit(event)
            _state.updateEmailState { it.copy(isSending = false) }
        }
    }

    fun unlinkEmailAddress() {
        _state.updateEmailState { it.copy(isUnlinking = true) }
        viewModelScope.launch {
            val result = unlink()
            when (result) {
                UnlinkResult.Failure -> {
                    _events.emit(SnackbarEvent.EmailUnlinkingFailure)
                }

                UnlinkResult.Success -> {
                    _state.unlinkEmail()
                    _events.emit(SnackbarEvent.EmailUnlinked)
                }
            }
        }
    }

    fun onEmail(string: String) {
        val isValid = Email.validate(string)
        val differentFromInitial = initialProfile.email?.string != string
        val isVerifiable = isValid && differentFromInitial
        _state.updateEmailState {
            EmailState(
                ValidatableField(value = string, isValid = isValid),
                isVerifiable = isVerifiable,
                isUnlinkable = false,
            )
        }
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
                    _events.emit(SnackbarEvent.AvatarUploadingFailure)
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
                    _events.emit(SnackbarEvent.SavingFailure)
                    _state.update { old -> old.copy(isSaving = false) }
                }

                is FriendlyUsersClient.EditResult.Success -> onSuccess()
            }
        }
    }

    fun onSuccessfulVerificationCodeState(
        emailConfirmationState: EmailConfirmationState,
    ) {
        viewModelScope.launch {
            if (emailConfirmationState.successful) {
                _events.emit(SnackbarEvent.EmailLinked)
                _state.confirmEmail()
            } else {
                _events.emit(SnackbarEvent.EmailLinkingFailure)
            }
        }
    }
}

private fun EditProfileScreenVmStateFlow.toggleInterest(
    interest: Interest,
    initialProfile: InitialProfile,
) {
    val state = this
    val picked = interest in state.value.currentProfile.interests
    val previousProfile = state.value.currentProfile
    val newInterests = if (picked) {
        previousProfile.interests.minus(interest)
    } else {
        previousProfile.interests.plus(interest)
    }
    val newProfile = previousProfile.copy(interests = newInterests)
    updateCurrentProfile(newProfile, initialProfile)
}

private fun EditProfileScreenVmStateFlow.unlinkEmail() {
    val state = this
    state.updateEmailState { previous ->
        previous.copy(
            field = ValidatableField(null),
            isUnlinkable = false,
            isVerifiable = false,
            isUnlinking = false,
        )
    }
}

private fun EditProfileScreenVmStateFlow.confirmEmail() {
    updateEmailState { old ->
        old.copy(isUnlinkable = true, isVerifiable = false)
    }
}

private fun EditProfileScreenVmStateFlow.updateEmailState(
    block: (previous: EmailState) -> EmailState,
) {
    val state = this
    state.update { old ->
        val oldProfile = old.currentProfile
        old.copy(
            currentProfile = oldProfile.copy(email = block(oldProfile.email)),
        )
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

private fun EditProfileScreenVmStateFlow.updateDescription(
    string: String,
    initialProfile: InitialProfile,
) {
    val state = this
    val isValid = UserDescription.validate(string)
    val newProfile = state.value.currentProfile.copy(
        description = ValidatableField(string, isValid),
    )
    state.updateCurrentProfile(newProfile, initialProfile)
}

private fun EditProfileScreenVmStateFlow.updateSocialLink(
    string: String,
    initialProfile: InitialProfile,
) {
    val state = this
    val isValid = SocialLink.validate(string)
    val newProfile = state.value.currentProfile.copy(
        socialLink = ValidatableField(string.ifBlank { null }, isValid),
    )
    state.updateCurrentProfile(newProfile, initialProfile)
}

private fun EditProfileScreenVmStateFlow.updateNickname(
    string: String,
    initialProfile: InitialProfile,
) {
    val state = this
    val isValid = Nickname.validate(string)
    val newProfile = state.value.currentProfile.copy(
        nickname = ValidatableField(string, isValid),
    )
    state.updateCurrentProfile(newProfile, initialProfile)
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
    val hasChanges = newProfile.hasChanges(initialProfile)
    val allFieldsAreValid = newProfile.validateAll()
    val emailIsNotVerifiable = !newProfile.email.isVerifiable

    return hasChanges && allFieldsAreValid && emailIsNotVerifiable
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
        profile.avatar is Initial,
    ).any { !it }
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
