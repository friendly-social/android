package friendly.android

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import friendly.sdk.CommunityPostDetails

@Composable
fun CommunityPostScreenMainPostContent(
    isRoot: Boolean,
    post: CommunityPostDetails.Plain,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        when (isRoot) {
            true -> {
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier,
                ) {
                    UserAvatar(
                        nickname = post.owner.nickname,
                        userId = post.owner.id,
                        uri = post.owner.avatar?.let(vm::fileUri),
                        style = UserAvatarStyle.Medium,
                    )

                    Spacer(Modifier.width(8.dp))

                    Column {
                        Text(
                            text = post.owner.nickname.string,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(Modifier.height(4.dp))

                        RelativeTimestampText(
                            instant = post.instant,
                            style = MaterialTheme.typography.bodyMedium,
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
                        markdown = post.text.string,
                        imageWidth = imageWidth,
                        onImageClick = onImageClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            false -> {
                SegmentedListItem(
                    shapes = endingListItemShape(),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor =
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(
                                horizontal = 4.dp,
                                vertical = 4.dp,
                            )
                            .fillMaxSize(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            UserAvatar(
                                nickname = post.owner.nickname,
                                userId = post.owner.id,
                                uri = post.owner.avatar?.let(vm::fileUri),
                                style = UserAvatarStyle.Small,
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = post.owner.nickname.string,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Spacer(Modifier.height(4.dp))
                                RelativeTimestampText(
                                    instant = post.instant,
                                    style = MaterialTheme.typography.bodyMedium,
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
                                markdown = post.text.string,
                                imageWidth = imageWidth,
                                onImageClick = onImageClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
