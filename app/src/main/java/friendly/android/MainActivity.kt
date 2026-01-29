package friendly.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import friendly.sdk.FriendlyClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val context = this
        val client = FriendlyClient.production(
            HttpClient(CIO) {
                install(Logging) {
                    logger = Logger.ANDROID
                    level = LogLevel.ALL
                }
            },
        )
        val authStorage = AuthStorage(context)
        val selfProfileStorage = SelfProfileStorage(context)

        val viewModelFactory = FriendlyViewModelFactory(
            registerUseCase = RegisterUseCase(
                client = client,
                authStorage = authStorage,
                profileStorage = selfProfileStorage,
            ),
            avatarUploadUseCase = AvatarUploadUseCase(
                client = client,
                context = context,
            ),
            authStorage = authStorage,
            selfProfileStorage = selfProfileStorage,
            client = client,
        )

        val authorization = authStorage.getAuthOrNull()

        setContent {
            FriendlyApp(
                viewModelFactory = viewModelFactory,
                authorization = authorization,
            )
        }
    }
}
