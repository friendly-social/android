package friendly.android

import androidx.core.text.isDigitsOnly
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import friendly.android.FriendlyNavGraph.Home.CodeConfirmationSheet as CodeConfirmationSheetRoute
import friendly.sdk.Email
import friendly.sdk.LoginCode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class VerifyEmailAuthCodeSheetVmState(
    val email: Email,
    val codeValue: String,
    val codeVerificationFailed: Boolean,
    val codeValid: Boolean,
    val isVerifying: Boolean,
) {
    fun toUiState(): EmailCodeSheetUiState {
        if (isVerifying) {
            return EmailCodeSheetUiState.CodeVerifying(
                email = email,
                code = codeValue,
            )
        }
        return EmailCodeSheetUiState.CodeEditing(
            email = email,
            code = codeValue,
            codeValid = codeValid,
            codeVerificationFailed = codeVerificationFailed,
        )
    }
}

class VerifyEmailAuthCodeSheetViewModel(
    savedStateHandle: SavedStateHandle,
    val login: LoginUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CodeConfirmationSheetRoute>(
        typeMap = EmailSheetTypeMap,
    )

    private val _state = MutableStateFlow(
        VerifyEmailAuthCodeSheetVmState(
            email = route.email.typed(),
            codeValue = "",
            codeVerificationFailed = false,
            codeValid = false,
            isVerifying = false,
        ),
    )

    val state = _state
        .map(VerifyEmailAuthCodeSheetVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EmailCodeSheetUiState.CodeEditing(
                email = route.email.typed(),
                code = "",
                codeValid = false,
                codeVerificationFailed = false,
            ),
        )

    private val _events = MutableSharedFlow<EmailCodeVerificationSheetEvent>()
    val events = _events.shareIn(viewModelScope, Eagerly)

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
            val loginCode = LoginCode.orThrow(_state.value.codeValue.toInt())
            val result = login(route.email.typed(), loginCode)

            when (result) {
                LoginUseCase.LoginResult.IOError,
                LoginUseCase.LoginResult.InvalidCode,
                LoginUseCase.LoginResult.UnknownError,
                    -> {
                    _state.update { it.copy(codeVerificationFailed = true) }
                }

                LoginUseCase.LoginResult.LoggedIn -> _events.emit(
                    CodeConfirmationSuccess,
                )
            }

            _state.update { old -> old.copy(isVerifying = false) }
        }
    }

    private fun validateCode(string: String): Boolean =
        string.isDigitsOnly() && string.length == 8
}
