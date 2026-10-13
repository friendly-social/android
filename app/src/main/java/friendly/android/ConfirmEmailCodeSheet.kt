package friendly.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

sealed interface EmailCodeConfirmationSheetEvent {
    data object CodeConfirmationSuccess : EmailCodeConfirmationSheetEvent
}

@Composable
fun ConfirmEmailCodeSheet(
    vm: ConfirmEmailCodeSheetViewModel,
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
        onPastedCode = vm::pasteCode,
        onDismiss = onDismiss,
        submitCode = vm::verifyCode,
        state = state,
        modifier = modifier,
    )
}
