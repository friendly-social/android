package friendly.android

import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostText
import friendly.sdk.FriendlyCommunityClient

class CreatePostUseCase(
    private val client: FriendlyCommunityClient,
    private val authStorage: AuthStorage,
) {
    suspend operator fun invoke(
        text: CommunityPostText,
        replyTo: CommunityPostDescriptor?,
    ): FriendlyCommunityClient.PostResult = client.post(
        authorization = authStorage.getAuth(),
        text = text,
        replyTo = replyTo,
    )
}
