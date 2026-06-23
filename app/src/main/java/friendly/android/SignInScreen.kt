package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import friendly.sdk.Email

sealed interface SignInScreenUiState {
    data class Idle(val email: ValidatableField<String>) : SignInScreenUiState

    data object Loading : SignInScreenUiState
}

@Composable
fun SignInScreen(
    vm: SignInScreenViewModel,
    onHome: () -> Unit,
    onConfirm: (Email) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        modifier = modifier.padding(contentPadding),
    ) { innerPadding ->
        when (val state = state) {
            is Idle -> {
                IdleState(
                    onConfirm = { onConfirm(Email.orThrow(state.email.value)) },
                    vm = vm,
                    state = state,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }

            is Loading -> {
                Box(
                    contentAlignment = Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LoadingIndicator(Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun IdleState(
    onConfirm: () -> Unit,
    state: SignInScreenUiState.Idle,
    vm: SignInScreenViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.your_email_address),
            style = MaterialTheme.typography.headlineLargeEmphasized,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "We will send you a verification code",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.email.value,
            onValueChange = vm::onEmail,
            label = { Text("Email address") },
            placeholder = { Text("email@domain.com") },
            isError = state.email.invalidAndNotBlank,
            supportingText = {
                if (state.email.invalidAndNotBlank) {
                    Text(stringResource(R.string.email_validation_text))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        Button(
            contentPadding = ButtonDefaults.MediumContentPadding,
            shape = ButtonDefaults.squareShape,
            enabled = state.email.isValid,
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.verify))
        }

        Spacer(Modifier.height(16.dp))
    }
}
