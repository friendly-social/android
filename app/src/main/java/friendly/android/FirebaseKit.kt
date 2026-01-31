package friendly.android

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import friendly.sdk.FirebaseToken
import friendly.sdk.FriendlyAuthClient.FirebaseResult
import friendly.sdk.FriendlyClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

object FirebaseKit {
    private val scope = MainScope()
    private val firebase = FirebaseMessaging.getInstance()
    private lateinit var prefs: SharedPreferences
    private lateinit var client: FriendlyClient
    private lateinit var authStorage: AuthStorage

    fun onAppCreate(
        context: Context,
        client: FriendlyClient,
        authStorage: AuthStorage,
    ) {
        this.prefs = context.getSharedPreferences(
            "friendly.android.firebase",
            Context.MODE_PRIVATE,
        )
        this.client = client
        this.authStorage = authStorage
        uploadIfNeed()
    }

    fun onNewToken() {
        resetUploadedToken()
        uploadIfNeed()
    }

    /**
     * Call this function **before** logout. And only if it succeeded, you can
     * safely reset current login.
     *
     * @return whether it succeeded or not
     */
    suspend fun onLogout(): Boolean {
        val authorization = authStorage.getAuthOrNull()
        if (authorization == null) {
            resetUploadedToken()
            return true
        }
        if (client.auth.logout(authorization) !is Success) {
            return false
        }
        resetUploadedToken()
        return true
    }

    fun onLogin() {
        uploadIfNeed()
    }

    private fun uploadIfNeed() {
        val authorization = authStorage.getAuthOrNull() ?: return
        scope.launch {
            val tokenString = firebase.getToken().await()
            val token = FirebaseToken.orThrow(tokenString)
            if (getUploadedToken() != token) {
                foreverTry {
                    client.auth.firebase(
                        authorization = authorization,
                        firebaseToken = token,
                    )
                }
                setUploadedToken(token)
            }
        }
    }

    private suspend inline fun foreverTry(block: () -> FirebaseResult) {
        while (true) {
            val result = block()
            when (result) {
                is Success -> break
                is IOError -> {
                    delay(1.seconds)
                    continue
                }
                is ServerError -> {
                    delay(1.hours)
                    continue
                }
            }
        }
    }

    private fun getUploadedToken(): FirebaseToken? {
        val string = prefs.getString("uploaded_token", null) ?: return null
        return FirebaseToken.orThrow(string)
    }

    private fun resetUploadedToken() {
        prefs.edit { remove("uploaded_token") }
    }

    private fun setUploadedToken(token: FirebaseToken) {
        prefs.edit {
            putString("uploaded_token", token.string)
        }
    }
}
