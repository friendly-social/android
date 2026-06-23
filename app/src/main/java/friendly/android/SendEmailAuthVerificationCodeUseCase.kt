package friendly.android

import friendly.sdk.Email
import friendly.sdk.FriendlyAuthClient
import friendly.sdk.FriendlyAuthClient.EmailResult

class SendEmailAuthVerificationCodeUseCase(
    private val localeRepository: LocaleRepository,
    private val authClient: FriendlyAuthClient,
) {
    sealed interface VerificationResult {
        data object Success : VerificationResult
        data object Failure : VerificationResult
    }

    suspend operator fun invoke(email: Email): VerificationResult {
        val result = authClient.email(
            email = email,
            localeCode = localeRepository.obtain(),
        )
        return when (result) {
            is EmailResult.IOError,
            is EmailResult.ServerError,
            is EmailResult.UnknownEmail -> VerificationResult.Failure
            is EmailResult.Success -> VerificationResult.Success
        }
    }
}
