package friendly.android

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.compose.LocalImageTransformer
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.compose.elements.MarkdownText
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.model.rememberMarkdownState
import com.mikepenz.markdown.utils.MARKDOWN_TAG_IMAGE_URL
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
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

                val imageTransformer = remember(imageWidth) {
                    SquarePictureMarkdownImageTransformer(imageWidth)
                }

                Box {
                    MarkdownPreviewRenderer(
                        imageTransformer = imageTransformer,
                        details = details,
                        onImageClick = onImageClick,
                        modifier = Modifier
                            .fillMaxWidth(),
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
    imageTransformer: SquarePictureMarkdownImageTransformer,
    details: CommunityPostDetails.Plain,
    onImageClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val markdownState = rememberMarkdownState(
        retainState = true,
        immediate = true,
        content = details.text.string,
    )

    val isDarkTheme = isSystemInDarkTheme()

    val highlightsBuilder = remember(isDarkTheme) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
    }

    Markdown(
        markdownState = markdownState,
        annotator = markdownAnnotator(
            config = markdownAnnotatorConfig(inlineImageAsBlock = false),
        ),
        imageTransformer = imageTransformer,
        typography = markdownTypography(
            h1 = MaterialTheme.typography.displayMedium,
            h2 = MaterialTheme.typography.displaySmall,
            h3 = MaterialTheme.typography.headlineLarge,
            text = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
        ),
        components = markdownComponents(
            paragraph = { paragraphModel ->
                PreviewMarkdownParagraph(
                    model = paragraphModel,
                    onImageClick = onImageClick,
                )
            },
            codeBlock = {
                MarkdownHighlightedCodeBlock(
                    content = it.content,
                    node = it.node,
                    highlightsBuilder = highlightsBuilder,
                    showHeader = true,
                )
            },
            codeFence = {
                MarkdownHighlightedCodeFence(
                    content = it.content,
                    node = it.node,
                    highlightsBuilder = highlightsBuilder,
                    showHeader = true,
                )
            },
        ),
        animations = markdownAnimations(animateTextSize = { this }),
        modifier = modifier
            .wrapContentHeight(
                align = Alignment.Top,
                unbounded = true,
            ),
    )
}

@Composable // TODO: make this private
fun PreviewMarkdownParagraph(
    model: MarkdownComponentModel,
    onImageClick: (link: String) -> Unit,
) {
    val style = model.typography.paragraph
    val settings = annotatorSettings()
    val transformer = LocalImageTransformer.current

    val annotated = buildAnnotatedString {
        pushStyle(style.toSpanStyle())
        buildMarkdownAnnotatedString(
            content = model.content,
            node = model.node,
            annotatorSettings = settings,
        )
        pop()
    }

    val imagePrefix = "${MARKDOWN_TAG_IMAGE_URL}_"
    val images = annotated
        .getStringAnnotations(0, annotated.length)
        .filter { it.item.startsWith(imagePrefix) }
        .sortedBy { it.start }

    if (images.isEmpty()) {
        MarkdownParagraph(
            content = model.content,
            node = model.node,
            style = style,
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        var cursor = 0

        images.forEach { image ->
            if (cursor < image.start) {
                val textBefore = annotated.subSequence(
                    cursor,
                    image.start,
                )

                if (textBefore.text.isNotBlank()) {
                    MarkdownText(
                        content = textBefore,
                        node = model.node,
                        style = style,
                        sourceContent = model.content,
                    )
                }
            }

            val url = image.item.removePrefix(imagePrefix)
            val imageData = transformer.transform(url)

            if (imageData != null) {
                Image(
                    painter = imageData.painter,
                    contentDescription = imageData.contentDescription,
                    alignment = imageData.alignment,
                    contentScale = imageData.contentScale,
                    alpha = imageData.alpha,
                    colorFilter = imageData.colorFilter,
                    modifier = imageData.modifier
                        .clickable(onClick = { onImageClick(url) }),
                )
//                Spacer(Modifier.height(8.dp))
            }

            cursor = image.end
        }

        if (cursor < annotated.length) {
            val textAfter = annotated.subSequence(
                cursor,
                annotated.length,
            )

            if (textAfter.text.isNotBlank()) {
                MarkdownText(
                    content = textAfter,
                    node = model.node,
                    style = style,
                    sourceContent = model.content,
                )
            }
        }
    }
}
