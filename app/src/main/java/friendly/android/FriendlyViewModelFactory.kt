package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import friendly.sdk.FriendlyClient
import kotlin.reflect.KClass

class FriendlyViewModelFactory(
    private val linkEmailUseCase: LinkEmailUseCase,
    private val unlinkEmailUseCase: UnlinkEmailUseCase,
    private val registerUseCase: RegisterUseCase,
    private val avatarUploadUseCase: AvatarUploadUseCase,
    private val confirmCodeUseCase: ConfirmCodeUseCase,
    private val sendAuthCodeUseCase: SendEmailAuthVerificationCodeUseCase,
    private val signInUseCase: SignInUseCase,
    private val confirmLoginCodeUseCase: ConfirmLoginCodeUseCase,
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val client: FriendlyClient,
) : ViewModelProvider.Factory {

    // todo refactor this thing
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: KClass<T>,
        extras: CreationExtras,
    ): T {
        if (modelClass == RegisterScreenViewModel::class) {
            return RegisterScreenViewModel(
                register = registerUseCase,
                uploadAvatar = avatarUploadUseCase,
            ) as T
        }
        if (modelClass == ProfileScreenViewModel::class) {
            return ProfileScreenViewModel(
                authStorage = authStorage,
                filesClient = client.files,
                usersClient = client.users,
                friendsClient = client.friends,
            ) as T
        }
        val isSelfProfileVm = modelClass == SelfProfileScreenViewModel::class
        if (isSelfProfileVm) {
            return SelfProfileScreenViewModel(
                authStorage = authStorage,
                selfProfileStorage = selfProfileStorage,
                filesClient = client.files,
                logout = LogoutUseCase(
                    authStorage = authStorage,
                    selfProfileStorage = selfProfileStorage,
                ),
            ) as T
        }
        val isEditProfileVm = modelClass == EditProfileScreenViewModel::class
        if (isEditProfileVm) {
            val savedStateHandle = extras.createSavedStateHandle()
            return EditProfileScreenViewModel(
                filesClient = client.files,
                savedStateHandle = savedStateHandle,
                edit = EditProfileUseCase(
                    authStorage = authStorage,
                    client = client.users,
                    selfProfileStorage = selfProfileStorage,
                ),
                link = linkEmailUseCase,
                unlink = unlinkEmailUseCase,
                uploadAvatar = avatarUploadUseCase,
            ) as T
        }
        if (modelClass == NetworkScreenViewModel::class) {
            return NetworkScreenViewModel(
                client = client,
                authStorage = authStorage,
            ) as T
        }
        if (modelClass == ConfirmEmailCodeSheetViewModel::class) {
            val savedStateHandle = extras.createSavedStateHandle()
            return ConfirmEmailCodeSheetViewModel(
                savedStateHandle = savedStateHandle,
                confirm = confirmCodeUseCase,
            ) as T
        }
        val isShareProfileVm = modelClass == ShareProfileScreenViewModel::class
        if (isShareProfileVm) {
            return ShareProfileScreenViewModel(
                authStorage = authStorage,
                client = client,
            ) as T
        }
        if (modelClass == FeedScreenViewModel::class) {
            return FeedScreenViewModel(
                like = SendFriendshipRequestUseCase(
                    friendsClient = client.friends,
                    authStorage = authStorage,
                ),
                dislike = DeclineFriendshipUseCase(
                    friendsClient = client.friends,
                    authStorage = authStorage,
                ),
                loadFeedQueue = LoadFeedQueueUseCase(
                    feedClient = client.feed,
                    authStorage = authStorage,
                ),
                filesClient = client.files,
            ) as T
        }
        if (modelClass == SignInScreenViewModel::class) {
            return SignInScreenViewModel(
                sendCode = sendAuthCodeUseCase,
                signIn = signInUseCase,
            ) as T
        }
        if (modelClass == VerifyEmailAuthCodeSheetViewModel::class) {
            val savedStateHandle = extras.createSavedStateHandle()
            return VerifyEmailAuthCodeSheetViewModel(
                savedStateHandle = savedStateHandle,
                login = confirmLoginCodeUseCase,
            ) as T
        }
        val isAddFriendByTokenVm =
            modelClass == AddFriendByTokenScreenViewModel::class
        if (isAddFriendByTokenVm) {
            return AddFriendByTokenScreenViewModel(
                client = client,
                authStorage = authStorage,
            ) as T
        }
        error("unknown viewmodel class")
    }
}
