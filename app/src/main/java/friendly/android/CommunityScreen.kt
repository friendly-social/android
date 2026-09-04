package friendly.android

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityScreenUiState(val posts: List<CommunityPostDetails>)

class CommunityScreenViewModel(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
) : ViewModel() {
    private val _state: MutableStateFlow<CommunityScreenUiState> =
        MutableStateFlow(CommunityScreenUiState(listOf()))

    val state: StateFlow<CommunityScreenUiState> = _state.asStateFlow()

    fun fetchAll() {
        viewModelScope.launch {
            val authorization = authStorage.getAuth()

            when (val postsList = client.community.list(authorization, null)) {
                is IOError -> {}
                is ServerError -> {}
                is Success -> {
                    val data = postsList.cursor.data
                    _state.update { old -> old.copy(posts = data) }
                }

                is Unauthorized -> TODO()
            }
        }
    }

    fun fileUri(fileDescriptor: FileDescriptor): Uri =
        client.files.getEndpoint(fileDescriptor).string.toUri()
}

@Composable
fun CommunityScreen(
    vm: CommunityScreenViewModel,
    contentPadding: PaddingValues,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.fetchAll() }

    Scaffold(
        modifier = modifier.padding(contentPadding),
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                items(state.value.posts) { item ->
                    CommunityPost(
                        details = item,
                        avatarUri = (item as? Plain)?.owner?.avatar?.let(
                            vm::fileUri,
                        ),
                        onClick = onPostClick,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}
