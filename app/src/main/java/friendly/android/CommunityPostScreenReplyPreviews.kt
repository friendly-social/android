package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import friendly.sdk.UserDetails

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
        for (commonFriend in replyPreviews) {
            UserAvatar(
                userId = commonFriend.id,
                nickname = commonFriend.nickname,
                uri = commonFriend.avatar?.let(vm::fileUri),
                style = UserAvatarStyle(24.dp, noAvatarSize = 14.dp),
            )
        }
    }
}
