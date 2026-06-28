package friendly.android

import friendly.sdk.Authorization
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyUsersClient
import friendly.sdk.FriendlyUsersClient.DetailsResult.IOError
import friendly.sdk.FriendlyUsersClient.DetailsResult.ServerError
import friendly.sdk.FriendlyUsersClient.DetailsResult.Unauthorized

/**
 * Loads the profile from the server, stores the authorization and
 * a personal profile data.
 *
 * TODO: consider making a generic component for these actions later
 *  (for syncing/signing up purposes).
 */
class SignInUseCase(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val usersClient: FriendlyUsersClient,
) {
    sealed interface Result {
        data object IOError : Result
        data object ServerError : Result
        data object Unauthorized : Result
        data object Success : Result
    }

    suspend operator fun invoke(authorization: Authorization): Result {
        return when (val detailsResult = usersClient.details(authorization)) {
            is IOError -> Result.IOError
            is ServerError -> Result.ServerError
            is Unauthorized -> Result.Unauthorized

            is FriendlyUsersClient.DetailsResult.Success -> {
                authStorage.store(authorization)
                val details = detailsResult.details
                selfProfileStorage.store(
                    nickname = details.nickname,
                    userId = details.id,
                    description = details.description,
                    avatar = details.avatar,
                    interests = details.interests,
                    socialLink = details.socialLink,
                    email = details.email,
                )
                Result.Success
            }
        }
    }
}
