package friendly.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import friendly.android.NetworkScreenUiState.Success.FriendItem
import friendly.sdk.UserAccessHash
import friendly.sdk.UserId

sealed interface NetworkScreenUiState {
    data object AuthFailure : NetworkScreenUiState

    data object NetworkFailure : NetworkScreenUiState

    data object Other : NetworkScreenUiState

    data object Loading : NetworkScreenUiState

    data class Success(val friends: List<FriendItem>) : NetworkScreenUiState {
        data class FriendItem(
            val avatar: String?,
            val nickname: String,
            val id: UserId,
            val accessHash: UserAccessHash,
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun NetworkScreen(
    vm: NetworkScreenViewModel,
    onProfile: (UserId, UserAccessHash) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        vm.getFriends()
    }

    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.network)) },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        when (val state = state) {
            is NetworkScreenUiState.AuthFailure -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(stringResource(R.string.network_auth_failure_text))
                }
            }

            is NetworkScreenUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LoadingIndicator(Modifier.size(64.dp))
                }
            }

            is NetworkScreenUiState.NetworkFailure -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(stringResource(R.string.network_error_occurred))

                    Spacer(Modifier.height(16.dp))

                    FilledTonalButton(onClick = vm::getFriends) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            is NetworkScreenUiState.Other ->
                Text(stringResource(R.string.unknown_error_occurred))

            is NetworkScreenUiState.Success -> {
                Success(
                    state = state,
                    onFriendClick = { friend ->
                        onProfile(friend.id, friend.accessHash)
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun Success(
    state: NetworkScreenUiState.Success,
    onFriendClick: (FriendItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier) {
        items(state.friends) { friend ->
            FriendItem(
                avatarUri = friend.avatar,
                name = friend.nickname,
                onClick = { onFriendClick(friend) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = "Add more friends to expand your network!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun FriendItem(
    avatarUri: String?,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        SubcomposeAsyncImage(
            model = avatarUri,
            loading = {
                Box(modifier = Modifier.shimmer())
            },
            error = {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .size(64.dp),
                )
            },
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
