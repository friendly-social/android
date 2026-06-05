package friendly.android

import friendly.sdk.ConfirmationCode
import friendly.sdk.Email
import friendly.sdk.FriendlyEmailClient

class ConfirmCodeUseCase(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val emailClient: FriendlyEmailClient,
) {
    sealed interface ConfirmationResult {
        data object Failure : ConfirmationResult
        data object Success : ConfirmationResult
    }

    suspend operator fun invoke(
        email: Email,
        code: ConfirmationCode,
    ): ConfirmationResult {
        val auth = authStorage.getAuth()
        val result = emailClient.confirm(authorization = auth, code = code)
        return when (result) {
            is Success -> {
                selfProfileStorage.storeEmail(email)
                ConfirmationResult.Success
            }
            is IOError,
            is InvalidCode,
            is ServerError,
            -> ConfirmationResult.Failure
        }
    }
}
