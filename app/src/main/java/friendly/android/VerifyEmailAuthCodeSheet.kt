package friendly.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import friendly.android.EmailCodeVerificationSheetEvent.CodeVerificationSuccess
import friendly.sdk.Authorization

sealed interface EmailCodeVerificationSheetEvent {
    data class CodeVerificationSuccess(val authorization: Authorization) :
        EmailCodeVerificationSheetEvent
}

@Composable
fun VerifyEmailAuthCodeSheet(
    vm: VerifyEmailAuthCodeSheetViewModel,
    onDismiss: () -> Unit,
    onVerification: (EmailCodeLoginState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val lifecycleOwner = rememberLifecycleOwner()

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                when (event) {
                    is CodeVerificationSuccess -> {
                        onVerification(
                            EmailCodeLoginState(
                                email = state.email,
                                authorization = event.authorization,
                                success = true,
                            ),
                        )
                    }
                }
            }
        }
    }

    EmailCodeSheet(
        onCode = vm::updateCode,
        onDismiss = onDismiss,
        submitCode = vm::verifyCode,
        state = state,
        modifier = modifier,
    )
}
