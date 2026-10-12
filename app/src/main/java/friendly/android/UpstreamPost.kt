package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.model.rememberMarkdownState
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun UpstreamPost(
    index: Int,
    count: Int,
    upstream: CommunityPostDetails,
    onClick: (CommunityPostDescriptor) -> Unit,
    onImageClick: (String) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    when (upstream) {
        is CommunityPostDetails.Deleted -> {
            SegmentedListItem(
                onClick = { onClick(upstream.descriptor) },
                shapes = ListItemDefaults.segmentedShapes(index, count + 1),
                colors = ListItemDefaults.segmentedColors(
                    containerColor =
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                DeletedSingleUpstreamPost(
                    deleted = upstream,
                    vm = vm,
                    modifier = Modifier,
                )
            }
        }

        is CommunityPostDetails.Plain -> {
            SegmentedListItem(
                onClick = { onClick(upstream.descriptor) },
                shapes = ListItemDefaults.segmentedShapes(index, count + 1),
                colors = ListItemDefaults.segmentedColors(
                    containerColor =
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        UserAvatar(
                            nickname = upstream.owner.nickname,
                            userId = upstream.owner.id,
                            uri = upstream.owner.avatar?.let(vm::fileUri),
                            style = UserAvatarStyle.Small,
                        )
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(
                                text = upstream.owner.nickname.string,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatDateTime(upstream.instant),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    BoxWithConstraints(
                        modifier = modifier
                            .fillMaxWidth(),
                    ) {
                        val imageWidth = maxWidth

                        CommunityPostMarkdown(
                            markdown = upstream.text.string,
                            imageWidth = imageWidth,
                            onImageClick = onImageClick,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
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

@Composable
private fun DeletedSingleUpstreamPost(
    deleted: CommunityPostDetails.Deleted,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            style = MaterialTheme.typography.labelMedium,
            text = formatDateTime(deleted.instant),
            modifier = Modifier,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.post_deleted_by_author_text),
            fontStyle = FontStyle.Italic,
            modifier = Modifier,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CommunityPostScreenReplyPreviews(
                vm = vm,
                replyPreviews = deleted.replyPreviews,
                modifier = Modifier,
            )
        }
    }
}
