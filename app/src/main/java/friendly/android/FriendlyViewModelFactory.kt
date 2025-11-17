package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import friendly.sdk.FriendlyClient

class FriendlyViewModelFactory(
    private val registerUseCase: RegisterUseCase,
    private val avatarUploadUseCase: AvatarUploadUseCase,
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val client: FriendlyClient,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterScreenViewModel::class.java)) {
            return RegisterScreenViewModel(
                register = registerUseCase,
                uploadAvatar = avatarUploadUseCase,
            ) as T
        }
        if (modelClass.isAssignableFrom(ProfileScreenViewModel::class.java)) {
            return ProfileScreenViewModel(
                authStorage = authStorage,
                selfProfileStorage = selfProfileStorage,
                filesClient = client.files,
            ) as T
        }
        error("unknown viewmodel class")
    }
}
