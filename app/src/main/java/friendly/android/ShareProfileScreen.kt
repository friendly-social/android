package friendly.android

import android.content.ClipData
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import com.lightspark.composeqr.DotShape
import com.lightspark.composeqr.QrCodeView
import com.lightspark.composeqr.QrEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface ShareProfileScreenUiState {
    data object Generating : ShareProfileScreenUiState

    data class Share(val shareUrl: String) : ShareProfileScreenUiState
}

private object BasicQrEncoder : QrEncoder {
    override fun encode(qrData: String): ByteMatrix? {
        val errorCorrectionLevel = ErrorCorrectionLevel.L
        return Encoder.encode(
            qrData,
            errorCorrectionLevel,
            mapOf(
                EncodeHintType.CHARACTER_SET to "ASCII",
                EncodeHintType.MARGIN to 16,
                EncodeHintType.ERROR_CORRECTION to errorCorrectionLevel,
            ),
        ).matrix
    }
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

        Text(
            text = stringResource(R.string.share_profile_qr_code_description),
        )

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
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                    }

                    is ShareProfileScreenUiState.Share -> {
                        QrCodeView(
                            data = state.shareUrl,
                            dotShape = DotShape.Square,
                            encoder = BasicQrEncoder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .holdToCopy(
                                    string = state.shareUrl,
                                    clipboardManager = clipboardManager,
                                    scope = scope,
                                ),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    text = stringResource(R.string.hold_to_copy),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            contentPadding = ButtonDefaults.MediumContentPadding,
            shape = ButtonDefaults.squareShape,
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.done))
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun Modifier.holdToCopy(
    string: String,
    clipboardManager: Clipboard,
    scope: CoroutineScope,
): Modifier = this then Modifier
    .pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                scope.launch {
                    val entry = ClipEntry(
                        ClipData.newPlainText("share url", string),
                    )
                    clipboardManager.setClipEntry(entry)
                }
            },
        )
    }
