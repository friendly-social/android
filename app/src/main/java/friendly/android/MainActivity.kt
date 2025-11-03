package friendly.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import friendly.sdk.FriendlyClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val context = this
        val client = FriendlyClient.meetacy()
        val authStorage = AuthStorage(context)

        val viewModelFactory = FriendlyViewModelFactory(
            registerUseCase = RegisterUseCase(
                client = client,
                authStorage = authStorage,
            ),
            avatarUploadUseCase = AvatarUploadUseCase(
                client = client,
                context = context,
            ),
            authStorage = authStorage,
        )

        setContent {
            FriendlyApp(viewModelFactory)
        }
    }
}
