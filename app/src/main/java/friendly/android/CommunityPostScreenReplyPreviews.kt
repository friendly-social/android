package friendly.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import friendly.sdk.UserDetails

private const val REPLY_PREVIEWS_LIMIT = 8

@Composable
fun CommunityPostScreenReplyPreviews(
    vm: CommunityPostScreenViewModel,
    replyPreviews: List<UserDetails>,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = (-8).dp,
            alignment = Alignment.CenterHorizontally,
        ),
        modifier = modifier,
    ) {
        val moreThanLimit = replyPreviews.size > REPLY_PREVIEWS_LIMIT

        for (commonFriend in replyPreviews.take(REPLY_PREVIEWS_LIMIT)) {
            UserAvatar(
                userId = commonFriend.id,
                nickname = commonFriend.nickname,
                uri = commonFriend.avatar?.let(vm::fileUri),
                style = UserAvatarStyle(24.dp, noAvatarSize = 14.dp),
            )
        }

        if (moreThanLimit) {
            MorePreviewsStub(replyPreviews.size - REPLY_PREVIEWS_LIMIT)
        }
    }
}

@Composable
private fun MorePreviewsStub(
    previewsLeft: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Center,
        modifier = modifier
            .size(32.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ),
    ) {
        Text(
            text = "$previewsLeft+",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
