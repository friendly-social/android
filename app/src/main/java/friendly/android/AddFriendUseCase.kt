package friendly.android

import friendly.sdk.FriendToken
import friendly.sdk.FriendlyFriendsClient
import friendly.sdk.UserId

class AddFriendUseCase(
    val friendsClient: FriendlyFriendsClient,
    val authStorage: AuthStorage,
    val selfProfileStorage: SelfProfileStorage,
) {
    suspend operator fun invoke(
        friendToken: FriendToken,
        userId: UserId,
    ): FriendlyFriendsClient.AddResult {
        val authorization = authStorage.getAuth()
        val result = friendsClient.add(
            authorization = authorization,
            token = friendToken,
            userId = userId,
        )
        return when (result) {
            is Success -> {
                selfProfileStorage.setHasFirstFriend()
                result
            }
            else -> result
        }
    }
}
