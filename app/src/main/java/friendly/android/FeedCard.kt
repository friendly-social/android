package friendly.android

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserId

@Composable
fun FeedCard(
    xOffset: Float,
    indicatorSize: Dp,
    entry: FeedEntry,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // todo
        if (xOffset < 0) {
            DislikeIndicator(indicatorSize)
        }

        if (xOffset > 0) {
            LikeIndicator(indicatorSize)
        }

        OutlinedCard(
            modifier = Modifier.fillMaxSize(),
        ) {
            FeedCardContent(
                entry = entry,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FeedCardContent(entry: FeedEntry, modifier: Modifier = Modifier) {
    Column(modifier) {
        ProfileImage(
            nickname = entry.nickname,
            userId = entry.id,
            avatarUri = entry.avatarUri,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = entry.nickname.string,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            FeedCardInterests(
                interests = entry.interests,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = entry.description.string,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BoxScope.LikeIndicator(size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .size(size)
            .align(Alignment.CenterStart),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_thumb_up),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size / 2),
        )
    }
}

@Composable
private fun BoxScope.DislikeIndicator(size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .size(size)
            .align(Alignment.CenterEnd),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_thumb_down),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(size / 2),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileImage(userId: UserId, nickname: Nickname, avatarUri: Uri?) {
    SubcomposeAsyncImage(
        model = avatarUri,
        loading = {
            Box(modifier = Modifier.shimmer())
        },
        error = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.pastelFromLong(
                            long = userId.long,
                            useDark = isSystemInDarkTheme(),
                        ),
                    )
                    .aspectRatio(1f),
            ) {
                Text(
                    text = nickname.string.take(1).uppercase(),
                    style = MaterialTheme.typography.displayLargeEmphasized,
                    fontSize = 192.sp,
                    color = MaterialTheme.colorScheme.onBackground
                        .copy(alpha = 0.7f),
                )
            }
        },
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    )
}

@Composable
private fun FeedCardInterests(
    interests: List<Interest>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        for (interest in interests) {
            Text(
                text = interest.string,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        color = Color.pastelFromString(
                            string = interest.string,
                            useDark = isSystemInDarkTheme(),
                        ),
                    )
                    .padding(4.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}
