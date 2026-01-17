package friendly.android

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import friendly.sdk.Nickname
import friendly.sdk.UserId

@Composable
fun UserAvatar(
    nickname: Nickname,
    userId: UserId,
    uri: Uri?,
    minNoAvatarTextSize: TextUnit = 36.sp,
    maxNoAvatarTextSize: TextUnit = 92.sp,
    noAvatarTextStepSize: TextUnit = 24.sp,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = uri,
        loading = { Box(Modifier.shimmer()) },
        error = {
            EmptyAvatar(
                nickname = nickname,
                userId = userId,
                minNoAvatarTextSize = minNoAvatarTextSize,
                maxNoAvatarTextSize = maxNoAvatarTextSize,
                noAvatarTextStepSize = noAvatarTextStepSize,
            )
        },
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape),
    )
}

/**
 * Would display circle box with first letters of
 * nickname and color based on userId
 */
@Composable
private fun EmptyAvatar(
    nickname: Nickname,
    userId: UserId,
    minNoAvatarTextSize: TextUnit,
    maxNoAvatarTextSize: TextUnit,
    noAvatarTextStepSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onBackground
        .copy(alpha = 0.7f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(
                Color.pastelFromLong(
                    long = userId.long,
                    useDark = isSystemInDarkTheme(),
                ),
            )
            .size(64.dp),
    ) {
        BasicText(
            text = getLettersFromNickname(nickname),
            color = { textColor },
            autoSize = TextAutoSize.StepBased(
                minFontSize = minNoAvatarTextSize,
                maxFontSize = maxNoAvatarTextSize,
                stepSize = noAvatarTextStepSize,
            ),
        )
    }
}

private fun getLettersFromNickname(nickname: Nickname): String =
    nickname.string.take(1).uppercase()
