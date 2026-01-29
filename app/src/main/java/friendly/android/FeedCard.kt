package friendly.android

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserId

@Composable
fun FeedCard(
    entry: FeedEntry,
    like: (FeedEntry) -> Unit,
    dislike: (FeedEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        OutlinedCard(
            modifier = Modifier.fillMaxSize(),
        ) {
            FeedCardContent(
                entry = entry,
                like = like,
                dislike = dislike,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeedCardContent(
    entry: FeedEntry,
    like: (FeedEntry) -> Unit,
    dislike: (FeedEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
        ) {
            AvatarWithInterests(
                nickname = entry.nickname,
                userId = entry.id,
                avatarUri = entry.avatarUri,
                interests = entry.interests,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxSize(),
            ) {
                Text(
                    text = entry.nickname.string,
                    style = MaterialTheme.typography.headlineMedium,
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

        Row(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            FilledTonalIconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                onClick = { dislike(entry) },
                modifier = Modifier
                    .size(
                        IconButtonDefaults.mediumContainerSize(
                            IconButtonDefaults.IconButtonWidthOption.Wide,
                        ),
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_thumb_down),
                    contentDescription = null,
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                )
            }

            Spacer(Modifier.weight(1f))

            FilledTonalIconButton(
                onClick = { like(entry) },
                modifier = Modifier.size(
                    IconButtonDefaults.mediumContainerSize(
                        widthOption = IconButtonDefaults.IconButtonWidthOption.Wide,
                    ),
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_thumb_up),
                    contentDescription = null,
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                )
            }
        }
    }
}

@Composable
private fun AvatarWithInterests(
    userId: UserId,
    nickname: Nickname,
    avatarUri: Uri?,
    interests: List<Interest>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Avatar(
            userId = userId,
            nickname = nickname,
            avatarUri = avatarUri,
            modifier = Modifier.fillMaxSize(),
        )

        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
        ) {
            item { Spacer(Modifier.width(8.dp)) }

            items(interests) { interest ->
                Text(
                    text = interest.string,
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            color = Color.pastelFromString(
                                string = interest.string,
                                useDark = isSystemInDarkTheme(),
                            ),
                        )
                        .padding(4.dp),
                )
            }

            item { Spacer(Modifier.width(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Avatar(
    userId: UserId,
    nickname: Nickname,
    avatarUri: Uri?,
    modifier: Modifier = Modifier,
) {
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
                        ).darken(),
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
        modifier = modifier,
    )
}
