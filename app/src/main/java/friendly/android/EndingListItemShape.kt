package friendly.android

import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable

@Composable
fun endingListItemShape(): ListItemShapes =
    ListItemDefaults.segmentedShapes(1, 2)
