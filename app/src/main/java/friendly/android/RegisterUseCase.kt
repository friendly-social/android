package friendly.android

import friendly.sdk.FriendlyClient
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription

class RegisterUseCase(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
) {
    suspend operator fun invoke(
        nickname: Nickname,
        description: UserDescription,
        interests: List<Interest>,
    ) {
        val authorization = client.auth.generate(
            nickname = nickname,
            description = description,
            interests = interests,
        )
        authStorage.store(authorization)
    }
}
