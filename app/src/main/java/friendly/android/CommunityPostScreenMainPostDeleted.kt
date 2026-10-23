package friendly.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import friendly.sdk.CommunityPostDetails

@Composable
fun CommunityPostScreenMainPostDeleted(
    post: CommunityPostDetails.Deleted,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        shapes = endingListItemShape(),
        colors = ListItemDefaults.segmentedColors(
            containerColor =
            MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier,
    ) {
        Column {
            RelativeTimestampText(
                instant = post.instant,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.post_deleted_by_author_text),
                fontStyle = FontStyle.Italic,
                modifier = Modifier,
            )
        }
    }
}
