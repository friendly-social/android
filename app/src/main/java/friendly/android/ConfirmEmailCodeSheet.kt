package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import friendly.sdk.Email
import kotlin.text.isNotBlank

private val EmailCodeVerificationSheetUiState.isCodeFieldError: Boolean
    get() {
        return when (val state = this) {
            is CodeEditing -> {
                val isInvalid = !state.codeValid
                val isNotBlank = state.code.isNotBlank()
                val codeVerificationFailed = state.codeVerificationFailed
                isInvalid && isNotBlank || codeVerificationFailed
            }

            is CodeVerifying -> false
        }
    }

sealed interface EmailCodeVerificationSheetEvent {
    data object CodeVerificationSuccess : EmailCodeVerificationSheetEvent
}

sealed interface EmailCodeVerificationSheetUiState {
    val email: Email
    val code: String

    data class CodeEditing(
        override val email: Email,
        override val code: String,
        val codeValid: Boolean,
        val codeVerificationFailed: Boolean,
    ) : EmailCodeVerificationSheetUiState

    data class CodeVerifying(
        override val email: Email,
        override val code: String,
    ) : EmailCodeVerificationSheetUiState
}

@Composable
fun ConfirmEmailCodeSheet(
    vm: ConfirmEmailCodeSheetViewModel,
    onDismiss: () -> Unit,
    onVerification: (EmailConfirmationState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val lifecycleOwner = rememberLifecycleOwner()

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                when (event) {
                    CodeVerificationSuccess -> {
                        onVerification(
                            EmailConfirmationState(state.email, true),
                        )
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        onDismissRequest = { onDismiss() },
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.confirm_email),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
            )

            Text(
                text = stringResource(
                    R.string.we_sent_you_verification_code_text,
                    state.email.string,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
            )

            Spacer(Modifier.height(8.dp))

            when (val state = state) {
                is CodeEditing -> {
                    OutlinedTextField(
                        label = {
                            Text(stringResource(R.string.verification_code))
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(
                                    R.string.eight_digit_verification_code,
                                ),
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                        value = state.code,
                        onValueChange = vm::updateCode,
                        isError = state.isCodeFieldError,
                        supportingText = {
                            if (state.codeVerificationFailed) {
                                Text(
                                    text = stringResource(
                                        R.string.code_validation_failed,
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        contentPadding = ButtonDefaults.MediumContentPadding,
                        shape = ButtonDefaults.squareShape,
                        enabled = state.codeValid,
                        onClick = { vm.verifyCode() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.verify))
                    }
                }

                is CodeVerifying -> {
                    LoadingIndicator(Modifier.size(64.dp))
                }
            }
        }
    }
}
