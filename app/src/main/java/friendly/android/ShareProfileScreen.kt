package friendly.android

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightspark.composeqr.QrCodeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface ShareProfileScreenUiState {
    data object Generating : ShareProfileScreenUiState

    data class Share(val shareUrl: String) : ShareProfileScreenUiState
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareProfileScreen(
    vm: ShareProfileScreenViewModel,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.generateUrl()
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.share_your_profile),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(16.dp))

        Crossfade(
            targetState = state,
            modifier = Modifier.size(300.dp),
        ) { state ->
            when (state) {
                is ShareProfileScreenUiState.Generating -> {
                    LoadingIndicator(
                        modifier = Modifier.size(300.dp),
                    )
                }

                is ShareProfileScreenUiState.Share -> {
                    QrCodeView(
                        data = state.shareUrl,
                        modifier = Modifier.size(300.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.height(48.dp),
        ) {
            AnimatedVisibility(
                visible = state is ShareProfileScreenUiState.Share,
                enter = fadeIn(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(onClick = onHome) {
                        Text(stringResource(R.string.back))
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            copyToClipboardIn(scope, state, clipboardManager)
                        },
                    ) {
                        Text(text = stringResource(R.string.copy))
                    }
                }
            }
        }
    }
}

private fun copyToClipboardIn(
    scope: CoroutineScope,
    state: ShareProfileScreenUiState,
    clipboardManager: Clipboard,
) {
    val shareUrl = (state as? ShareProfileScreenUiState.Share)
        ?.shareUrl
        ?: return
    scope.launch {
        val entry = ClipEntry(ClipData.newPlainText("share url", shareUrl))
        clipboardManager.setClipEntry(entry)
    }
}
