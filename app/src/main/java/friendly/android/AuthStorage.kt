package friendly.android

import android.content.Context
import androidx.core.content.edit
import friendly.sdk.Authorization
import friendly.sdk.Token
import friendly.sdk.UserAccessHash
import friendly.sdk.UserId

private const val AuthPreferences = "AuthPreferences"
private const val TokenPreference = "token"
private const val UserId = "user_id"
private const val AccessHash = "access_hash"

class AuthStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        AuthPreferences,
        Context.MODE_PRIVATE,
    )

    fun store(auth: Authorization) {
        preferences.edit {
            putString(TokenPreference, auth.token.string)
            putLong(UserId, auth.id.long)
            putString(AccessHash, auth.accessHash.string)
            commit()
        }
    }

    fun clear() {
        preferences.edit {
            clear()
            commit()
        }
    }

    fun getToken(): Token? {
        val tokenString = preferences.getString(Token, null)
        return tokenString?.let { string -> Token.orThrow(string) }
    }

    fun getUserId(): UserId? {
        val long = preferences.getLong(UserId, 0)
        return UserId(long)
    }

    fun getAccessHash(): UserAccessHash? {
        val string = preferences.getString(AccessHash, null)
        return string?.let { UserAccessHash.orThrow(it) }
    }

    fun getAuth(): Authorization = Authorization(
        id = getUserId() ?: error("UserId is required for Authorization"),
        accessHash =
        getAccessHash()
            ?: error("UserAccessHash is required for Authorization"),
        token = getToken() ?: error("Token is required for Authorization"),
    )

    fun getAuthOrNull(): Authorization? {
        return Authorization(
            id = getUserId() ?: return null,
            accessHash = getAccessHash() ?: return null,
            token = getToken() ?: return null,
        )
    }
}
