package friendly.android

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.model.rememberMarkdownState
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.UserDetails

private val MaxPreviewHeight = 640.dp

@Composable
fun CommunityPostPreview(
    vm: CommunityScreenViewModel,
    details: CommunityPostDetails,
    avatarUri: Uri?,
    onClick: (CommunityPostDescriptor) -> Unit,
    onImageClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (details) {
        is CommunityPostDetails.Deleted -> {
            Text("Deleted, TODO")
        }

        is CommunityPostDetails.Plain -> {
            PlainPostViewer(
                vm = vm,
                details = details,
                onClick = onClick,
                onImageClick = onImageClick,
                avatarUri = avatarUri,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun PlainPostViewer(
    vm: CommunityScreenViewModel,
    details: CommunityPostDetails.Plain,
    avatarUri: Uri?,
    onClick: (CommunityPostDescriptor) -> Unit,
    onImageClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(
                onClick = { onClick(details.descriptor) },
            )
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                UserAvatar(
                    userId = details.owner.id,
                    nickname = details.owner.nickname,
                    uri = avatarUri,
                    style = UserAvatarStyle.Small,
                    modifier = Modifier
                        .clickable(
                            onClick = {
                                avatarUri?.toString()?.let(onImageClick)
                            },
                            indication = null,
                            interactionSource = null,
                        ),
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = details.owner.nickname.string,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.width(4.dp))
                Text("·", color = Color.Gray)
                Spacer(Modifier.width(4.dp))

                RelativeTimestampText(
                    instant = details.instant,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )

                if (details.edited) {
                    Spacer(Modifier.width(4.dp))
                    Text("·", color = Color.Gray)
                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = stringResource(R.string.edited),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            var previewBoxHeight by remember { mutableIntStateOf(0) }

            Spacer(Modifier.height(8.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = MaxPreviewHeight)
                    .clipToBounds()
                    .onSizeChanged { previewBoxHeight = it.height },
            ) {
                val imageWidth = maxWidth
                Box {
                    MarkdownPreviewRenderer(
                        imageWidth = imageWidth,
                        details = details,
                        onImageClick = onImageClick,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    with(LocalDensity.current) {
                        if (previewBoxHeight.toDp() == MaxPreviewHeight) {
                            FadeGradientOverlay()
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (details.replyPreviews.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                ReplyPreviews(vm, details.replyPreviews)

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.replied),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BoxScope.FadeGradientOverlay() {
    val topFade = Brush.verticalGradient(
        0f to Color.Transparent,
        1f to Color.Red,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .fadingEdges(topFade)
            .background(
                color = MaterialTheme.colorScheme.background,
            )
            .align(Alignment.BottomCenter),
    )
}

fun Modifier.fadingEdges(brush: Brush): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

@Composable
private fun ReplyPreviews(
    vm: CommunityScreenViewModel,
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
                style = UserAvatarStyle(
                    30.dp,
                    noAvatarSize = 20.dp,
                ),
            )
        }
    }
}

@Composable
private fun MarkdownPreviewRenderer(
    imageWidth: Dp,
    details: CommunityPostDetails.Plain,
    onImageClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val markdownState = rememberMarkdownState(
        retainState = true,
        immediate = true,
        content = details.text.string,
    )
    FriendlyMarkdown(
        markdownState = markdownState,
        onImageClick = onImageClick,
        imageWidth = imageWidth,
        modifier = modifier,
    )
}
