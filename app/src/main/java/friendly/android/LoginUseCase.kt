package friendly.android

import friendly.sdk.ConfirmationCode
import friendly.sdk.Email
import friendly.sdk.FriendlyAuthClient
import friendly.sdk.LoginCode

class LoginUseCase(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val authClient: FriendlyAuthClient,
) {
    sealed interface LoginResult {
        data object LoggedIn : LoginResult
        data object InvalidCode : LoginResult
        data object IOError : LoginResult
        data object UnknownError : LoginResult
    }

    suspend operator fun invoke(
        email: Email,
        code: LoginCode,
    ): LoginResult {
        val loginResult = authClient.login(email, code)

        return when (loginResult) {
            is IOError -> LoginResult.IOError
            is InvalidCode -> LoginResult.InvalidCode
            is ServerError -> LoginResult.UnknownError

            is Success -> {
                println("so called \"successful login\"")

                // todo

                LoginResult.LoggedIn
            }
        }
    }
}
