package friendly.android

import androidx.core.text.isDigitsOnly
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import friendly.sdk.ConfirmationCode
import friendly.sdk.Email
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import friendly.android.FriendlyNavGraph.Home.CodeVerificationSheet as CodeVerificationSheetRoute

private data class EmailCodeVerificationSheetVmState(
    val email: Email,
    val codeValue: String,
    val codeVerificationFailed: Boolean,
    val codeValid: Boolean,
    val isVerifying: Boolean,
) {
    fun toUiState(): EmailCodeVerificationSheetUiState {
        if (isVerifying) {
            return EmailCodeVerificationSheetUiState.CodeVerifying(
                email = email,
                code = codeValue,
            )
        }
        return EmailCodeVerificationSheetUiState.CodeEditing(
            email = email,
            code = codeValue,
            codeValid = codeValid,
            codeVerificationFailed = codeVerificationFailed,
        )
    }
}

class ConfirmEmailCodeSheetViewModel(
    savedStateHandle: SavedStateHandle,
    private val confirm: ConfirmCodeUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CodeVerificationSheetRoute>(
        typeMap = CodeVerificationSheetTypeMap,
    )

    private val _state = MutableStateFlow(
        EmailCodeVerificationSheetVmState(
            email = route.email.typed(),
            codeValue = "",
            codeValid = false,
            isVerifying = false,
            codeVerificationFailed = false,
        ),
    )

    val state: StateFlow<EmailCodeVerificationSheetUiState> = _state
        .map(EmailCodeVerificationSheetVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EmailCodeVerificationSheetUiState.CodeEditing(
                email = route.email.typed(),
                code = "",
                codeValid = false,
                codeVerificationFailed = false,
            ),
        )

    private val _events = MutableSharedFlow<EmailCodeVerificationSheetEvent>()

    val events: SharedFlow<EmailCodeVerificationSheetEvent> =
        _events.shareIn(viewModelScope, Eagerly)

    fun updateCode(new: String) {
        if (!new.isDigitsOnly() || new.length > 8) return

        _state.update { old ->
            old.copy(
                codeVerificationFailed = false,
                codeValue = new,
                codeValid = validateCode(new),
            )
        }
    }

    fun verifyCode() {
        _state.update { old ->
            old.copy(isVerifying = true)
        }

        viewModelScope.launch {
            val confirmationCode = ConfirmationCode.orThrow(
                int = _state.value.codeValue.toInt(),
            )
            val result = confirm(route.email.typed(), confirmationCode)

            when (result) {
                is Failure -> {
                    _state.update { it.copy(codeVerificationFailed = true) }
                }
                is Success -> _events.emit(CodeVerificationSuccess)
            }

            _state.update { old -> old.copy(isVerifying = false) }
        }
    }

    private fun validateCode(string: String): Boolean =
        string.isDigitsOnly() && string.length == 8
}
