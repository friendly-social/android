package friendly.android

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import friendly.sdk.Nickname
import friendly.sdk.UserId

private val avatarShapes = listOf(
    MaterialShapes.Circle,
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Pentagon,
    MaterialShapes.Sunny,
    MaterialShapes.Cookie4Sided,
    MaterialShapes.Square,
    MaterialShapes.Arch,
    MaterialShapes.Slanted,
    MaterialShapes.Gem,
    MaterialShapes.Ghostish,
)

class UserAvatarStyle(
    val size: Dp,
    // The size of single letter, it must not be affected by font settings, so
    // it uses dp instead of sp
    val noAvatarSize: Dp,
) {
    companion object {
        val Large: UserAvatarStyle = UserAvatarStyle(
            size = 128.dp,
            noAvatarSize = 54.dp,
        )
        val Medium: UserAvatarStyle = UserAvatarStyle(
            size = 64.dp,
            noAvatarSize = 24.dp,
        )
        val Small: UserAvatarStyle = UserAvatarStyle(
            size = 40.dp,
            noAvatarSize = 16.dp,
        )
    }
}

@Composable
fun UserAvatar(
    nickname: Nickname,
    userId: UserId,
    uri: Uri?,
    style: UserAvatarStyle,
    modifier: Modifier = Modifier,
) {
    val shape = getShapeByUserId(userId)
    SubcomposeAsyncImage(
        model = uri,
        loading = { Box(Modifier.shimmer()) },
        error = {
            EmptyAvatar(
                nickname = nickname,
                userId = userId,
                style = style,
            )
        },
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .size(style.size)
            .clip(shape),
    )
}

/**
 * Displays circle box with first letters of
 * nickname and color based on userId
 */
@Composable
fun EmptyAvatar(
    nickname: Nickname?,
    userId: UserId,
    style: UserAvatarStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(getShapeByUserId(userId)) // todo still looks strange tbh
            .background(
                Color.pastelFromLong(
                    long = userId.long,
                    useDark = isSystemInDarkTheme(),
                ),
            )
            .size(style.size),
    ) {
        if (nickname != null) { // todo experiment
            AvatarText(style, nickname)
        }
    }
}

@Composable
private fun AvatarText(style: UserAvatarStyle, nickname: Nickname) {
    val textColor = MaterialTheme.colorScheme.onBackground
        .copy(alpha = 0.7f)
    val fontSize = with(LocalDensity.current) {
        style.noAvatarSize.toSp()
    }
    BasicText(
        text = getLettersFromNickname(nickname),
        color = { textColor },
        style = TextStyle(fontSize = fontSize),
    )
}

@Composable
private fun getShapeByUserId(id: UserId): Shape {
    val hash = (id.long * KnuthHashConstant) and 0x7FFFFFFF
    val shapeIndex = (hash % avatarShapes.size).toInt()
    return avatarShapes[shapeIndex].toShape()
}

private fun getLettersFromNickname(nickname: Nickname): String =
    nickname.string.take(1).uppercase()
