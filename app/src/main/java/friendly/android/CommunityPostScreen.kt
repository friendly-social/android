package friendly.android

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyCommunityClient.DetailsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommunityPostScreenViewModel(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
) : ViewModel() {
    private val _state = MutableStateFlow<DetailsResult.Success?>(null)

    val state: StateFlow<DetailsResult.Success?> = _state.asStateFlow()

    fun fetch(descriptor: CommunityPostDescriptor) {
        viewModelScope.launch {
            val authorization = authStorage.getAuth()
            val postDetails = client.community.details(
                authorization = authorization,
                descriptor = descriptor,
            )
            _state.update { postDetails.orThrow() }
        }
    }

    fun fileUri(fileDescriptor: FileDescriptor): Uri =
        client.files.getEndpoint(fileDescriptor).string.toUri()
}

@Composable
fun CommunityPostScreen(
    vm: CommunityPostScreenViewModel,
    descriptor: CommunityPostDescriptor,
    contentPadding: PaddingValues,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: this bro looks strange
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.fetch(descriptor) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = state) {
                null -> {
                    CircularProgressIndicator()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.upstream) { post ->
                            CommunityPost(
                                details = post,
                                avatarUri = (post as? Plain)?.owner?.avatar
                                    ?.let(vm::fileUri),
                                onClick = { onPostClick(post.descriptor) },
                                modifier = Modifier,
                            )
                        }

                        item {
                            CommunityPost(
                                details = state.post,
                                avatarUri = (state.post as? Plain)
                                    ?.owner
                                    ?.avatar
                                    ?.let(vm::fileUri),
                                onClick = {},
                                modifier = Modifier,
                            )
                        }

                        items(state.replies.data) { reply ->
                            CommunityPost(
                                details = reply,
                                avatarUri = (reply as? Plain)?.owner?.avatar
                                    ?.let(vm::fileUri),
                                onClick = { onPostClick(reply.descriptor) },
                                modifier = Modifier,
                            )
                        }
                    }
                }
            }
        }
    }
}
