package friendly.android

import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.InterestList
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserDescription

class RegisterUseCase(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
    private val profileStorage: SelfProfileStorage,
) {
    suspend operator fun invoke(
        nickname: Nickname,
        description: UserDescription,
        interests: InterestList,
        socialLink: SocialLink,
        avatar: FileDescriptor?,
    ) {
        val authorization = client.auth.generate(
            nickname = nickname,
            description = description,
            interests = interests,
            avatar = avatar,
            socialLink = socialLink,
        )
        val authorizationSuccess = authorization.orThrow()
        authStorage.store(authorizationSuccess)
        profileStorage.store(
            nickname = nickname,
            userId = authorizationSuccess.id,
            description = description,
            avatar = avatar,
            interests = interests,
            socialLink = socialLink,
        )
    }
}
