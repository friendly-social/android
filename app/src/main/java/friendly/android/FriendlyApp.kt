package friendly.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FriendlyApp(
    viewModelFactory: FriendlyViewModelFactory,
    modifier: Modifier = Modifier,
) {
    FriendlyTheme {
        Scaffold(
            modifier = modifier
                .fillMaxSize(),
        ) { innerPadding ->
            FriendlyNavGraph(
                viewModelFactory = viewModelFactory,
                Modifier.padding(innerPadding),
            )
        }
    }
}
