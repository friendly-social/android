package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.Token
import friendly.sdk.UserAccessHash
import friendly.sdk.UserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class State(
    val id: UserId,
    val accessHash: UserAccessHash,
    val token: Token,
)

class ProfileScreenViewModel(private val authStorage: AuthStorage) :
    ViewModel() {
    private val _state = MutableStateFlow<State?>(value = null)
    val state: StateFlow<State?> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            delay(2500L)
            _state.update {
                State(
                    id = authStorage.getUserId() ?: return@launch,
                    accessHash = authStorage.getAccessHash() ?: return@launch,
                    token = authStorage.getToken() ?: return@launch,
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: ProfileScreenViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.load()
    }

    when (val state = state) {
        null -> {
            CircularProgressIndicator()
        }

        else -> {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = modifier
                    .fillMaxSize(),
            ) {
                Text("userId: ${state.id}")
                Spacer(Modifier.height(16.dp))
                Text("accessHash ${state.accessHash}")
                Spacer(Modifier.height(16.dp))
                Text("token: ${state.token}")
            }
        }
    }
}
