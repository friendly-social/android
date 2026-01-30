package friendly.android

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.RegisterScreenUiState.AvatarState
import friendly.android.RegisterScreenUiState.AvatarState.None
import friendly.android.RegisterScreenUiState.AvatarState.Uploaded
import friendly.sdk.FileDescriptor
import friendly.sdk.Interest
import friendly.sdk.InterestList
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class RegisterState(
    val nickname: String,
    val description: String,
    val socialLink: String,
    val availableInterests: List<Interest>,
    val pickedInterests: List<Interest>,
    val isGenerating: Boolean,
    val avatar: AvatarState,
    val avatarFileDescriptor: FileDescriptor?,
) {
    fun toUiState(): RegisterScreenUiState {
        if (isGenerating) return RegisterScreenUiState.Generating
        return RegisterScreenUiState.Editing(
            nickname = nickname,
            description = description,
            availableInterests = availableInterests,
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
            availableInterests = interests,
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
                availableInterests = interests,
                pickedInterests = listOf(),
                avatar = None,
                socialLink = "",
            ),
        )

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

    fun toggleInterest(interest: Interest) {
        val picked = interest in _state.value.pickedInterests
        if (picked) {
            _state.update {
                it.copy(
                    pickedInterests = it.pickedInterests.minus(interest),
                )
            }
        } else {
            _state.update {
                it.copy(
                    pickedInterests = it.pickedInterests.plus(interest),
                )
            }
        }
    }

    private fun stateIsValid(): Boolean {
        val nicknameIsValid = Nickname.validate(_state.value.nickname)
        val descriptionIsValid =
            UserDescription.validate(_state.value.description)
        val interestsAreValid = _state.value.pickedInterests.isNotEmpty()
        // TODO: use sdk validation for socialLink
        val socialLinkIsValid = _state.value.socialLink.isNotEmpty()

        return nicknameIsValid &&
            descriptionIsValid &&
            interestsAreValid &&
            socialLinkIsValid
    }

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (!stateIsValid()) return@launch

            _state.update { it.copy(isGenerating = true) }

            val nickname = Nickname.orThrow(_state.value.nickname)
            val description = UserDescription.orThrow(_state.value.description)
            val socialLink = SocialLink.orThrow(_state.value.socialLink)
            val interests = InterestList.orThrow(_state.value.pickedInterests)
            val avatarFileDescriptor = _state.value.avatarFileDescriptor
            register(
                nickname = nickname,
                description = description,
                interests = interests,
                socialLink = socialLink,
                avatar = avatarFileDescriptor,
            )
            FirebaseKit.onLogin()
            onSuccess()
        }
    }

    fun pickAvatar(uri: Uri?) {
        if (uri == null) return

        _state.update { old ->
            old.copy(
                avatar = AvatarState.Uploading(
                    progress = AvatarUploadUseCase.UploadingPercentage(0.0),
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
                    old.copy(avatar = None)
                }
            }

            is AvatarUploadUseCase.UploadingResult.IOError -> {
                _state.update { old ->
                    old.copy(avatar = None)
                }
            }

            is AvatarUploadUseCase.UploadingResult.ServerError -> {
                _state.update { old ->
                    old.copy(avatar = None)
                }
            }
        }

        Log.d("avatar", "[vm] uploadingResult: $uploadingResult")
    }
}
