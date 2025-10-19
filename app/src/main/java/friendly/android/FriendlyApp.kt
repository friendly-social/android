package friendly.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FriendlyApp(modifier: Modifier = Modifier) {
    FriendlyAndroidTheme {
        Scaffold(
            modifier = modifier
                .padding(28.dp)
                .fillMaxSize(),
        ) { innerPadding ->
            FriendlyNavGraph(Modifier.padding(innerPadding))
        }
    }
}
