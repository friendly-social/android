package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FriendlyViewModelFactory(
    private val registerUseCase: RegisterUseCase,
    private val authStorage: AuthStorage,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterScreenViewModel::class.java)) {
            return RegisterScreenViewModel(registerUseCase) as T
        }
        if (modelClass.isAssignableFrom(ProfileScreenViewModel::class.java)) {
            return ProfileScreenViewModel(authStorage) as T
        }
        error("unknown viewmodel class")
    }
}
