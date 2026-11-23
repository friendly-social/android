package friendly.android

import android.content.ClipData
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

sealed interface ShareProfileScreenUiState {
    data object Generating : ShareProfileScreenUiState

    data object Error : ShareProfileScreenUiState

    data class Share(val shareUrl: String, val expiration: Instant) :
        ShareProfileScreenUiState
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareProfileScreen(
    onDone: () -> Unit,
    vm: ShareProfileScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.generateUrl() }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.share_your_profile),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Start),
        )

        Spacer(Modifier.height(16.dp))

        QrCodeDescription(state)

        Spacer(Modifier.height(16.dp))

        Crossfade(state) { state ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(Modifier.height(16.dp))

                when (state) {
                    is ShareProfileScreenUiState.Generating -> {
                        LoadingIndicator(
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f),
                        )
                    }

                    is ShareProfileScreenUiState.Share -> {
                        QrCode(
                            state = state,
                            clipboardManager = clipboardManager,
                            scope = scope,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f),
                        )
                    }

                    is ShareProfileScreenUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f),
                        ) {
                            NetworkErrorBox(
                                onRetry = { vm.generateUrl() },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        BottomButtons(
            state = state,
            scope = scope,
            clipboardManager = clipboardManager,
            onDone = onDone,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun BottomButtons(
    state: ShareProfileScreenUiState,
    scope: CoroutineScope,
    clipboardManager: Clipboard,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Button(
            contentPadding = ButtonDefaults.MediumContentPadding,
            shape = ButtonDefaults.squareShape,
            onClick = onDone,
            modifier = Modifier
                .heightIn(ButtonDefaults.MediumContainerHeight)
                .weight(1f),
        ) {
            Text(
                text = stringResource(R.string.done),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (state is ShareProfileScreenUiState.Share) {
            Spacer(Modifier.width(8.dp))

            FilledTonalIconButton(
                shape = IconButtonDefaults.mediumSquareShape,
                onClick = {
                    scope.launch {
                        copyUrl(clipboardManager, state.shareUrl)
                    }
                },
                modifier = Modifier.size(
                    IconButtonDefaults.mediumContainerSize(),
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = null,
                    modifier = Modifier
                        .size(ButtonDefaults.MediumIconSize),
                )
            }
        }
    }
}

@Composable
private fun QrCodeDescription(state: ShareProfileScreenUiState) {
    Text(
        text = when (val state = state) {
            is ShareProfileScreenUiState.Error,
            is ShareProfileScreenUiState.Generating,
            -> stringResource(
                R.string.share_profile_qr_code_description,
            )

            is ShareProfileScreenUiState.Share,
            -> {
                val now = rememberNow(interval = 1.seconds)
                val timeLeft = state.expiration - now
                val timeLeftFormatted = timeLeft
                    .toComponents { hours, minutes, seconds, _ ->
                        "%d:%02d:%02d".format(hours, minutes, seconds)
                    }
                stringResource(
                    R.string.share_profile_qr_code_description_with_expiration,
                    timeLeftFormatted,
                )
            }
        },
    )
}

@Composable
private fun QrCode(
    state: ShareProfileScreenUiState.Share,
    clipboardManager: Clipboard,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val onBackground =
        MaterialTheme.colorScheme.onBackground
    val painter = rememberQrCodePainter(state.shareUrl) {
        shapes {
            ball = QrBallShape.circle()
            darkPixel = QrPixelShape.roundCorners()
            frame = QrFrameShape.roundCorners(.25f)
        }
        errorCorrectionLevel = QrErrorCorrectionLevel.Low
        colors {
            dark = QrBrush.solid(onBackground)
            frame = QrBrush.solid(onBackground)
        }
    }
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .holdToCopy(
                string = state.shareUrl,
                clipboardManager = clipboardManager,
                scope = scope,
            ),
    )
}

private fun Modifier.holdToCopy(
    string: String,
    clipboardManager: Clipboard,
    scope: CoroutineScope,
): Modifier = this then Modifier
    .pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                scope.launch { copyUrl(clipboardManager, string) }
            },
        )
    }

private suspend fun copyUrl(clipboardManager: Clipboard, url: String) {
    val entry = ClipEntry(ClipData.newPlainText("share url", url))
    clipboardManager.setClipEntry(entry)
}
