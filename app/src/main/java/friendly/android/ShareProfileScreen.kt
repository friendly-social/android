package friendly.android

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
                    QrCode(
                        state = state,
                        modifier = Modifier,
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

@Composable
private fun QrCode(
    state: ShareProfileScreenUiState.Share,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberQrBitmapPainter(
            content = state.shareUrl,
            size = 300.dp,
            padding = 0.dp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            backgroundColor = Color.Unspecified,
        ),
        contentDescription = null,
        modifier = modifier
            .size(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .padding(16.dp),
    )
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
