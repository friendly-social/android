package friendly.android

import android.util.Log
import friendly.sdk.FriendlyEmailClient

class UnlinkEmailUseCase(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val emailClient: FriendlyEmailClient,
) {
    sealed interface UnlinkResult {
        data object Failure : UnlinkResult
        data object Success : UnlinkResult
    }

    suspend operator fun invoke(): UnlinkResult {
        val authorization = authStorage.getAuth()
        return when (val result = emailClient.unlink(authorization)) {
            is Success -> {
                selfProfileStorage.storeEmail(null)
                UnlinkResult.Success
            }

            is IOError,
            is ServerError,
            -> {
                Log.e("UnlinkEmailUseCase", "Unlinking error occurred: $result")
                UnlinkResult.Failure
            }
        }
    }
}
