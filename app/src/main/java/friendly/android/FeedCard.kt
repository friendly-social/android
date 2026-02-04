package friendly.android

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults.elevatedCardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription
import friendly.sdk.UserId

@Composable
fun FeedCard(
    entry: FeedEntry,
    like: (FeedEntry) -> Unit,
    dislike: (FeedEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ElevatedCard(
            modifier = Modifier.fillMaxSize(),
            elevation = elevatedCardElevation(),
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
            AvatarWithOverlay(
                nickname = entry.nickname,
                userId = entry.id,
                avatarUri = entry.avatarUri,
                isExtendedNetwork = entry.isExtendedNetwork,
                isRequest = entry.isRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column {
                    ExpandableDescription(
                        description = entry.description,
                        collapsedMaxLine = 7,
                        expandText = stringResource(R.string.expand),
                        modifier = Modifier.fillMaxWidth(),
                        interests = entry.interests,
                    )

                    Spacer(Modifier.height(96.dp))
                }
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
                    containerColor =
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
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
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                onClick = { like(entry) },
                modifier = Modifier.size(
                    IconButtonDefaults.mediumContainerSize(
                        widthOption =
                        IconButtonDefaults.IconButtonWidthOption.Wide,
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
fun ExpandableDescription(
    description: UserDescription,
    collapsedMaxLine: Int = 7,
    expandText: String,
    interests: List<Interest>,
    modifier: Modifier = Modifier,
) {
    val text = description.string
    var isExpanded by remember { mutableStateOf(false) }
    var isClickable by remember { mutableStateOf(false) }
    var lastCharacterIndex by remember { mutableStateOf(0) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clickable(
                onClick = { isExpanded = true },
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .clip(MaterialTheme.shapes.medium),
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Interests(interests)

            Spacer(Modifier.height(8.dp))

            val annotatedText = buildAnnotatedString {
                if (isClickable) {
                    if (isExpanded) {
                        append(text)
                    } else {
                        val adjustText = text
                            .take(lastCharacterIndex)
                            .dropLast(expandText.length)
                            .dropLastWhile { it.isWhitespace() || it == '.' }

                        append(adjustText)
                    }
                } else {
                    append(text)
                }
            }

            Text(
                text = annotatedText,
                maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLine,
                onTextLayout = { textLayoutResult ->
                    if (!isExpanded && textLayoutResult.hasVisualOverflow) {
                        isClickable = true
                        lastCharacterIndex = textLayoutResult
                            .getLineEnd(collapsedMaxLine - 1)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .animateContentSize(),
            )
            if (isClickable && !isExpanded) {
                Text(
                    text = expandText,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun AvatarWithOverlay(
    userId: UserId,
    nickname: Nickname,
    avatarUri: Uri?,
    isExtendedNetwork: Boolean,
    isRequest: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Avatar(
            userId = userId,
            nickname = nickname,
            avatarUri = avatarUri,
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(top = 16.dp)
                    .padding(bottom = 8.dp)
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = nickname.string,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isRequest) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(stringResource(R.string.friend_request))
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(36.dp),
                    )
                } else if (isExtendedNetwork) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(stringResource(R.string.extended_network))
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Interests(interests: List<Interest>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(interests) { i, interest ->
            val useDark = isSystemInDarkTheme()
            val color = remember(interest.string, useDark) {
                Color.pastelFromString(
                    string = interest.string,
                    useDark = useDark,
                )
            }
            if (i == 0) {
                Spacer(Modifier.width(20.dp))
            } else {
                Spacer(Modifier.width(8.dp))
            }
            SuggestionChip(
                onClick = {},
                label = { Text(interest.string) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = color,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = null,
                modifier = Modifier.height(28.dp),
            )
            if (i == interests.lastIndex) {
                Spacer(Modifier.width(20.dp))
            }
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
