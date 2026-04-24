package friendly.android

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import friendly.sdk.Interest

@Composable
fun ToggleableInterestChip(
    interest: Interest,
    selected: Boolean,
    onToggle: (Interest) -> Unit,
) {
    FilterChip(
        onClick = { onToggle(interest) },
        label = {
            Text(text = interest.string)
        },
        selected = selected,
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Done icon",
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}
