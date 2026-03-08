package friendly.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import friendly.sdk.Interest

@Composable
fun InterestChip(interest: Interest, modifier: Modifier = Modifier) {
    val useDark = isSystemInDarkTheme()
    val color = remember(interest.string, useDark) {
        Color.pastelFromString(
            string = interest.string,
            useDark = useDark,
        )
    }
    val labelColor = MaterialTheme.colorScheme.onSurface
    key(interest.string) {
        SuggestionChip(
            onClick = {},
            label = { Text(interest.string) },
            colors = SuggestionChipDefaults
                .suggestionChipColors(
                    containerColor = color,
                    labelColor = labelColor,
                ),
            border = null,
            modifier = modifier.height(32.dp),
        )
    }
}
