package friendly.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

sealed interface EmailCodeVerificationSheetEvent {
    data object CodeConfirmationSuccess : EmailCodeVerificationSheetEvent
}

@Composable
fun VerifyEmailAuthCodeSheet(
    vm: VerifyEmailAuthCodeSheetViewModel,
    onDismiss: () -> Unit,
    onVerification: (EmailCodeSubmissionState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val lifecycleOwner = rememberLifecycleOwner()

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                when (event) {
                    CodeConfirmationSuccess -> {
                        onVerification(
                            EmailCodeSubmissionState(state.email, true),
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
