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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

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
            Text(
                style = MaterialTheme.typography.labelSmall,
                text = formatDateTime(post.instant),
                modifier = Modifier,
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

// TODO: make normal global date-time formatting utils
private fun formatDateTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val format = LocalDateTime.Format {
        year()
        chars("-")
        monthNumber()
        chars("-")
        day()

        chars(" ")

        hour()
        chars(":")
        minute()
    }
    val formattedDateTime = localDateTime.format(format)
    return formattedDateTime
}
