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
                usersClient = client.users,
                logout = LogoutUseCase(
                    authStorage = authStorage,
                    selfProfileStorage = selfProfileStorage,
                ),
            ) as T
        }
        if (modelClass.isAssignableFrom(NetworkScreenViewModel::class.java)) {
            return NetworkScreenViewModel(
                client = client,
                authStorage = authStorage,
            ) as T
        }
        val isShareProfileVm = modelClass
            .isAssignableFrom(ShareProfileScreenViewModel::class.java)
        if (isShareProfileVm) {
            return ShareProfileScreenViewModel(
                authStorage = authStorage,
                client = client,
            ) as T
        }
        if (modelClass.isAssignableFrom(FeedScreenViewModel::class.java)) {
            return FeedScreenViewModel(
                sendRequest = SendFriendshipRequestUseCase(
                    friendsClient = client.friends,
                    authStorage = authStorage,
                ),
                decline = DeclineFriendshipUseCase(
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
        val isAddFriendByTokenVm = modelClass
            .isAssignableFrom(AddFriendByTokenScreenViewModel::class.java)
        if (isAddFriendByTokenVm) {
            return AddFriendByTokenScreenViewModel(
                client = client,
                authStorage = authStorage,
            ) as T
        }
        error("unknown viewmodel class")
    }
}
