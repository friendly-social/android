package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed interface FeedScreenUiState {
    data object NetworkError : FeedScreenUiState

    data class Idle(val currentFeedItem: FeedItem) : FeedScreenUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(vm: FeedScreenViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
        vm.loadFeed()
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
            IndicatedCardFeed(
                currentItem = FeedItem.Loading,
                like = vm::like,
                dislike = vm::dislike,
            )
        }
    }
}
