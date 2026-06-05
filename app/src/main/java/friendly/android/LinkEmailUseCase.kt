package friendly.android

import friendly.sdk.Email
import friendly.sdk.FriendlyEmailClient

class LinkEmailUseCase(
    private val localeRepository: LocaleRepository,
    private val emailClient: FriendlyEmailClient,
    private val authStorage: AuthStorage,
) {
    suspend operator fun invoke(email: Email) {
        val authorization = authStorage.getAuth()

        emailClient.link(
            authorization = authorization,
            email = email,
            localeCode = localeRepository.obtain(),
        )
    }
}
