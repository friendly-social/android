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
        FirebaseKit.onAppCreate(context, client, authStorage)
        val localeRepository = LocaleRepository(context.applicationContext)
        val selfProfileStorage = SelfProfileStorage(context)

        val viewModelFactory = FriendlyViewModelFactory(
            unlinkEmailUseCase = UnlinkEmailUseCase(
                emailClient = client.email,
                selfProfileStorage = selfProfileStorage,
                authStorage = authStorage,
            ),
            linkEmailUseCase = LinkEmailUseCase(
                localeRepository = localeRepository,
                emailClient = client.email,
                authStorage = authStorage,
            ),
            confirmCodeUseCase = ConfirmCodeUseCase(
                authStorage = authStorage,
                selfProfileStorage = selfProfileStorage,
                emailClient = client.email,
            ),
            registerUseCase = RegisterUseCase(
                client = client,
                authStorage = authStorage,
                profileStorage = selfProfileStorage,
            ),
            avatarUploadUseCase = AvatarUploadUseCase(
                client = client,
                context = context,
            ),
            sendAuthCodeUseCase = SendEmailAuthVerificationCodeUseCase(
                localeRepository = localeRepository,
                authClient = client.auth,
            ),
            loginUseCase = LoginUseCase(
                authStorage = authStorage,
                selfProfileStorage = selfProfileStorage,
                authClient = client.auth,
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
