package friendly.android

import android.Manifest.permission.POST_NOTIFICATIONS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(onSignUp: () -> Unit, modifier: Modifier = Modifier) {
    RequestNotificationsPermission()
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.welcome_to_friendly),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.welcome_screen_title),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.welcome_screen_text),
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onSignUp,
                ) {
                    Text(text = stringResource(R.string.sign_up))
                }
            }
        }
    }
}

@Composable
private fun RequestNotificationsPermission() {
    val launcher = rememberLauncherForActivityResult(RequestPermission()) {}
    LaunchedEffect(Unit) {
        launcher.launch(POST_NOTIFICATIONS)
    }
}

@Preview
@Composable
private fun WelcomeScreenPreview() {
    FriendlyTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            WelcomeScreen(
                onSignUp = {},
                modifier = Modifier
                    .fillMaxSize(),
            )
        }
    }
}
