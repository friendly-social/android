package friendly.android

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.error
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.LocalImageTransformer
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.compose.elements.MarkdownText
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.ImageWidth
import com.mikepenz.markdown.model.PlaceholderConfig
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.utils.MARKDOWN_TAG_IMAGE_URL
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Instant

private class PostPreviewImageTransformer(private val imageWidth: Dp) :
    ImageTransformer by Coil3ImageTransformerImpl {
    // Plans on aspect ratio here:
    // 1. to make pictures fit this square with blurred background
    // 2. make pictures possible for taking several possible aspect ratios,
    // and force them take specified ratios when they stop looking convenient
    val ratio = 1f

    @Composable
    override fun transform(link: String): ImageData {
        val context = LocalPlatformContext.current
        val density = LocalDensity.current

        val widthPx = with(density) {
            imageWidth.roundToPx().coerceAtLeast(1)
        }

        val heightPx = (widthPx / ratio).roundToInt().coerceAtLeast(1)

        val request = remember(link, context, widthPx, heightPx) {
            ImageRequest.Builder(context)
                .data(link)
                .size(widthPx, heightPx)
                .error(R.drawable.ic_error)
                .build()
        }

        val painter = rememberAsyncImagePainter(model = request)

        val imageData = ImageData(
            alignment = Alignment.Center,
            painter = painter,
            modifier = Modifier
                .width(imageWidth)
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        return imageData
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size = Size.Unspecified

    override fun placeholderConfig(
        link: String,
        density: Density,
        containerSize: Size,
        imageWidth: ImageWidth,
        imageSize: Size,
        imageSizeChanged: ((String, Size) -> Unit)?,
    ): PlaceholderConfig = PlaceholderConfig(
        size = Size(
            width = this.imageWidth.value,
            height = this.imageWidth.value / ratio,
        ),
    )
}

@Composable
fun CommunityPostPreview(
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
    details: CommunityPostDetails.Plain,
    avatarUri: Uri?,
    onClick: (CommunityPostDescriptor) -> Unit,
    onImageClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = { onClick(details.descriptor) },
        modifier = modifier.padding(horizontal = 6.dp),
    ) {
        Row(Modifier.padding(8.dp)) {
            UserAvatar(
                userId = details.owner.id,
                nickname = details.owner.nickname,
                uri = avatarUri,
                style = UserAvatarStyle.Small,
                modifier = Modifier
                    .clickable(
                        onClick = { avatarUri?.toString()?.let(onImageClick) },
                        indication = null,
                        interactionSource = null,
                    ),
            )

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Row {
                    Text(
                        text = details.owner.nickname.string,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = formatDateTime(details.instant),
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    Spacer(Modifier.width(6.dp))

                    if (details.edited) {
                        Text(
                            text = "[ed1t3d]",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(max = 640.dp)
                        .clipToBounds(),
                ) {
                    val imageWidth = maxWidth

                    val imageTransformer = remember(imageWidth) {
                        PostPreviewImageTransformer(imageWidth)
                    }

                    MarkdownPreviewRenderer(
                        imageTransformer = imageTransformer,
                        details = details,
                        onImageClick = onImageClick,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownPreviewRenderer(
    imageTransformer: PostPreviewImageTransformer,
    details: CommunityPostDetails.Plain,
    onImageClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()

    val highlightsBuilder = remember(isDarkTheme) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
    }

    Markdown(
        content = details.text.string,
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

// TODO: make normal global date-time formatting utils
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

@Composable
private fun PreviewMarkdownParagraph(
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
                    modifier = imageData.modifier
                        .clickable(onClick = { onImageClick(url) }),
                    alignment = imageData.alignment,
                    contentScale = imageData.contentScale,
                    alpha = imageData.alpha,
                    colorFilter = imageData.colorFilter,
                )
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
