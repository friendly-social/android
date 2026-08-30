package friendly.android

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import friendly.markdowntext.MarkdownText
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun CommunityPost(
    details: CommunityPostDetails,
    avatarUri: Uri?,
    modifier: Modifier = Modifier,
    onClick: (CommunityPostDescriptor) -> Unit = {},
) {
    Card(
        onClick = { onClick(details.descriptor) },
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(
                vertical = 12.dp,
                horizontal = 12.dp,
            ),
        ) {
            UserAvatar(
                userId = details.owner.id,
                nickname = details.owner.nickname,
                // todo idk move to the separate model
                uri = avatarUri,
                style = UserAvatarStyle.Small,
                modifier = Modifier,
            )
            Spacer(Modifier.width(4.dp))
            Column(
                modifier = Modifier,
            ) {
                Row {
                    Text(
                        text = details.owner.nickname.string,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatDateTime(details.instant),
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Light,
                    )
                    Spacer(Modifier.width(6.dp))
                    if (details.edited) {
                        Text(
                            text = "[ed1t3d]",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                MarkdownText(
                    markdown = details.text.string,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// TODO: incorrect temp formatting
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
