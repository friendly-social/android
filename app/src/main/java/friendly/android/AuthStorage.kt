package friendly.android

import android.content.Context
import androidx.core.content.edit
import friendly.sdk.Authorization
import friendly.sdk.Token
import friendly.sdk.UserAccessHash
import friendly.sdk.UserId

private const val AUTH_PREFERENCES = "AuthPreferences"
private const val TOKEN = "token"
private const val USER_ID = "user_id"
private const val ACCESS_HASH = "access_hash"

class AuthStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        AUTH_PREFERENCES,
        Context.MODE_PRIVATE,
    )

    fun store(auth: Authorization) {
        preferences.edit {
            putString(TOKEN, auth.token.string)
            putLong(USER_ID, auth.id.long)
            putString(ACCESS_HASH, auth.accessHash.string)
            commit()
        }
    }

    fun getToken(): Token? {
        val tokenString = preferences.getString(TOKEN, null)
        return tokenString?.let { string -> Token.orThrow(string) }
    }

    fun getUserId(): UserId? {
        val long = preferences.getLong(USER_ID, 0)
        return UserId(long)
    }

    fun getAccessHash(): UserAccessHash? {
        val string = preferences.getString(ACCESS_HASH, null)
        return string?.let { UserAccessHash.orThrow(it) }
    }

    fun getAuth(): Authorization? {
        return Authorization(
            id = getUserId() ?: return null,
            accessHash = getAccessHash() ?: return null,
            token = getToken() ?: return null
        )
    }
}
