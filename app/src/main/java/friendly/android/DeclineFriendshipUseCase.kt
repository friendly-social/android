package friendly.android

import friendly.sdk.FriendlyFriendsClient
import friendly.sdk.UserAccessHash
import friendly.sdk.UserId

class DeclineFriendshipUseCase(
    private val friendsClient: FriendlyFriendsClient,
    private val authStorage: AuthStorage,
) {
    suspend operator fun invoke(
        userId: UserId,
        accessHash: UserAccessHash,
    ): FriendlyFriendsClient.DeclineResult {
        val requestResult = friendsClient.decline(
            authorization = authStorage.getAuth(),
            userId = userId,
            userAccessHash = accessHash,
        )
        return requestResult
    }
}
