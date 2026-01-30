package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

// TODO: adjust UI accordingly to guidelines
@Composable
fun NetworkErrorBox(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_network_error),
            contentDescription = null,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.network_error_occurred),
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(16.dp))

        OutlinedButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
