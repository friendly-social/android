package friendly.android

import android.net.Uri
import friendly.android.AvatarUploadUseCase.UploadingPercentage
import friendly.android.RegisterScreenUiState.AvatarState
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription

val RegisterScreenUiState.isFirstPageValid: Boolean
    get() = when (this) {
        is RegisterScreenUiState.Generating -> true

        is RegisterScreenUiState.Editing -> {
            val nicknameIsValid = Nickname.validate(nickname)
            val descriptionIsValid = UserDescription.validate(description)
            val avatarIsValid = avatar is AvatarState.Uploaded
            nicknameIsValid && descriptionIsValid && avatarIsValid
        }
    }

sealed interface RegisterScreenUiState {
    data class Editing(
        val availableInterests: List<Interest>,
        val pickedInterests: List<Interest>,
        val nickname: String,
        val description: String,
        val avatar: AvatarState,
    ) : RegisterScreenUiState

    sealed interface AvatarState {
        data object None : AvatarState

        data class Uploaded(val uri: Uri) : AvatarState

        data class Uploading(
            val progress: UploadingPercentage,
            val uri: Uri?,
        ) : AvatarState
    }

    data object Generating : RegisterScreenUiState
}

val AvatarState.uriOrNull: Uri?
    get() = when (this) {
        is AvatarState.None -> null
        is AvatarState.Uploaded -> this.uri
        is AvatarState.Uploading -> this.uri
    }
