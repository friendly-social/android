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
    sealed interface RegistrationResult {
        data object NetworkError : RegistrationResult
        data object ServerError : RegistrationResult
        data object Success : RegistrationResult
    }

    suspend operator fun invoke(
        nickname: Nickname,
        description: UserDescription,
        interests: InterestList,
        socialLink: SocialLink,
        avatar: FileDescriptor?,
    ): RegistrationResult {
        val authorizationResult = client.auth.generate(
            nickname = nickname,
            description = description,
            interests = interests,
            avatar = avatar,
            socialLink = socialLink,
        )
        return when (authorizationResult) {
            is IOError -> RegistrationResult.NetworkError
            is ServerError -> RegistrationResult.ServerError

            is Success -> {
                val authorization = authorizationResult.orThrow()
                authStorage.store(authorization)
                profileStorage.store(
                    nickname = nickname,
                    userId = authorization.id,
                    description = description,
                    avatar = avatar,
                    interests = interests,
                    socialLink = socialLink,
                )
                FirebaseKit.onLogin()
                RegistrationResult.Success
            }
        }
    }
}
