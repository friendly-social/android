package friendly.android

import friendly.sdk.Authorization
import friendly.sdk.Email
import friendly.sdk.FriendlyAuthClient
import friendly.sdk.LoginCode

class ConfirmLoginCodeUseCase(private val authClient: FriendlyAuthClient) {
    sealed interface LoginResult {
        data class Success(val authorization: Authorization) : LoginResult
        data object InvalidCode : LoginResult
        data object IOError : LoginResult
        data object ServerError : LoginResult
    }

    suspend operator fun invoke(email: Email, code: LoginCode): LoginResult =
        when (val loginResult = authClient.login(email, code)) {
            is IOError -> LoginResult.IOError
            is InvalidCode -> LoginResult.InvalidCode
            is ServerError -> LoginResult.ServerError

            is Success -> {
                val authorization = loginResult.authorization
                LoginResult.Success(authorization)
            }
        }
}
