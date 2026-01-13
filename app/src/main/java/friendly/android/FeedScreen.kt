package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

sealed interface FeedScreenUiState {
    data object NetworkError : FeedScreenUiState

    data object ServerError : FeedScreenUiState

    data object AuthorizationError : FeedScreenUiState

    data object EmptyFeed : FeedScreenUiState

    data object Refreshing : FeedScreenUiState

    data class Idle(val currentFeedItem: FeedItem) : FeedScreenUiState
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun FeedScreen(vm: FeedScreenViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadFeed()
//        vm.refreshFeed() // todo
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            when (val state = state) {
                is FeedScreenUiState.Idle -> {
                    IndicatedCardFeed(
                        currentItem = state.currentFeedItem,
                        like = vm::like,
                        dislike = vm::dislike,
                    )
                }

                is FeedScreenUiState.NetworkError -> {
                    Text("network err")
                }

                is FeedScreenUiState.AuthorizationError -> {
                    Text("auth err")
                }

                is FeedScreenUiState.Refreshing -> {
                    LoadingIndicator(Modifier.size(96.dp))
                }

                is FeedScreenUiState.EmptyFeed -> {
                    Text(
                        text = "The feed is empty",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Add more friends and ask them to " +
                            "add their friends to make feed more interesting",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = vm::refreshFeed,
                        contentPadding =
                        ButtonDefaults.ButtonWithIconContentPadding,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Refresh",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                is FeedScreenUiState.ServerError -> {
                    Text("server err")
                }
            }
        }
    }
}
