package friendly.android

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.RegisterScreenUiEvent.SnackbarEvent
import friendly.android.RegisterScreenUiEvent.SnackbarEvent.SnackbarEventKind
import friendly.android.RegisterScreenUiState.AvatarState
import friendly.android.RegisterScreenUiState.AvatarState.None
import friendly.android.RegisterScreenUiState.AvatarState.Uploaded
import friendly.android.RegisterUseCase.RegistrationResult.NetworkError
import friendly.android.RegisterUseCase.RegistrationResult.ServerError
import friendly.android.RegisterUseCase.RegistrationResult.Success
import friendly.sdk.FileDescriptor
import friendly.sdk.Interest
import friendly.sdk.InterestList
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserDescription
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class RegisterState(
    val nickname: String,
    val description: String,
    val socialLink: String,
    val pickedInterests: List<String>,
    val isGenerating: Boolean,
    val avatar: AvatarState,
    val avatarFileDescriptor: FileDescriptor?,
) {
    fun toUiState(): RegisterScreenUiState {
        if (isGenerating) return RegisterScreenUiState.Generating
        return RegisterScreenUiState.Editing(
            nickname = nickname,
            description = description,
            pickedInterests = pickedInterests,
            avatar = avatar,
            socialLink = socialLink,
        )
    }
}

class RegisterScreenViewModel(
    private val uploadAvatar: AvatarUploadUseCase,
    private val register: RegisterUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(
        value = RegisterState(
            nickname = "",
            description = "",
            pickedInterests = listOf(),
            isGenerating = false,
            avatar = None,
            avatarFileDescriptor = null,
            socialLink = "",
        ),
    )
    val state = _state
        .map(RegisterState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RegisterScreenUiState.Editing(
                nickname = "",
                description = "",
                pickedInterests = listOf(),
                avatar = None,
                socialLink = "",
            ),
        )

    private val _events = MutableSharedFlow<RegisterScreenUiEvent>()
    val events = _events.shareIn(viewModelScope, Eagerly)

    fun updateNickname(new: String) {
        _state.update {
            it.copy(nickname = new)
        }
    }

    fun updateDescription(new: String) {
        _state.update {
            it.copy(description = new)
        }
    }

    fun updateSocialLink(new: String) {
        _state.update {
            it.copy(socialLink = new)
        }
    }

    fun onPickedInterests(newPickedInterests: List<String>) {
        _state.update { old -> old.copy(pickedInterests = newPickedInterests) }
    }

    private fun stateIsValid(): Boolean {
        val nicknameIsValid = Nickname.validate(_state.value.nickname)
        val descriptionIsValid =
            UserDescription.validate(_state.value.description)
        val interestsAreValid = _state.value.pickedInterests.isNotEmpty()
        val socialLinkIsValid = SocialLink.validate(_state.value.socialLink)

        return nicknameIsValid &&
            descriptionIsValid &&
            interestsAreValid &&
            socialLinkIsValid
    }

    fun register() {
        viewModelScope.launch {
            if (!stateIsValid()) return@launch

            _state.update { it.copy(isGenerating = true) }

            val nickname = Nickname.orThrow(_state.value.nickname)
            val description = UserDescription.orThrow(_state.value.description)
            val socialLink = SocialLink.orThrow(_state.value.socialLink)
            val interests = InterestList
                .orThrow(_state.value.pickedInterests.map(Interest::orThrow))
            val avatarFileDescriptor = _state.value.avatarFileDescriptor
            val result = register(
                nickname = nickname,
                description = description,
                interests = interests,
                socialLink = socialLink,
                avatar = avatarFileDescriptor,
            )
            when (result) {
                NetworkError -> {
                    _events.emit(SnackbarEvent(SnackbarEventKind.NetworkError))
                    _state.update { it.copy(isGenerating = false) }
                }

                ServerError -> {
                    _events.emit(SnackbarEvent(SnackbarEventKind.ServerError))
                    _state.update { it.copy(isGenerating = false) }
                }

                Success -> {
                    _events.emit(RegisterScreenUiEvent.SuccessfulRegistration)
                }
            }
        }
    }

    fun pickAvatar(uri: Uri?) {
        if (uri == null) return

        _state.update { old ->
            old.copy(
                avatar = AvatarState.Uploading(
                    progress = AvatarUploadUseCase.UploadingPercentage(0.0f),
                    uri = uri,
                ),
            )
        }

        viewModelScope.launch {
            uploadAvatar(uri)
        }
    }

    private suspend fun uploadAvatar(uri: Uri) {
        val uploadingResult = uploadAvatar(
            avatarUri = uri,
        ) { flow ->
            flow.collect { progress ->
                _state.update { old ->
                    old.copy(
                        avatar = AvatarState.Uploading(
                            progress = progress,
                            uri = uri,
                        ),
                    )
                }
            }
        }

        when (uploadingResult) {
            is AvatarUploadUseCase.UploadingResult.Success -> {
                _state.update { old ->
                    old.copy(
                        avatar = Uploaded(uri),
                        avatarFileDescriptor = uploadingResult.fileDescriptor,
                    )
                }
            }

            is AvatarUploadUseCase.UploadingResult.CompressionFailure -> {
                _state.update { old ->
                    _events.emit(
                        SnackbarEvent(SnackbarEventKind.CompressionFailure),
                    )
                    old.copy(avatar = None)
                }
            }

            is AvatarUploadUseCase.UploadingResult.IOError -> {
                _state.update { old ->
                    _events.emit(SnackbarEvent(SnackbarEventKind.NetworkError))
                    old.copy(avatar = None)
                }
            }

            is AvatarUploadUseCase.UploadingResult.ServerError -> {
                _state.update { old ->
                    _events.emit(SnackbarEvent(SnackbarEventKind.ServerError))
                    old.copy(avatar = None)
                }
            }
        }

        Log.d("avatar", "[vm] uploadingResult: $uploadingResult")
    }
}
