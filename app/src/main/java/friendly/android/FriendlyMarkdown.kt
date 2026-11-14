package friendly.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
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
import com.mikepenz.markdown.model.MarkdownState
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.utils.MARKDOWN_TAG_IMAGE_URL
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes

/**
 * A common component used to render markdown everywhere
 */
@Composable
fun FriendlyMarkdown(
    markdownState: MarkdownState,
    onImageClick: (String) -> Unit,
    imageWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val highlightsBuilder = remember(isDarkTheme) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
    }
    Markdown(
        markdownState = markdownState,
        imageTransformer = SquarePictureMarkdownImageTransformer(
            imageWidth,
        ),
        annotator = markdownAnnotator(
            config = markdownAnnotatorConfig(
                inlineImageAsBlock = false,
                eolAsNewLine = true,
            ),
        ),
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
        modifier = modifier,
    )
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
                    alignment = imageData.alignment,
                    contentScale = imageData.contentScale,
                    alpha = imageData.alpha,
                    colorFilter = imageData.colorFilter,
                    modifier = imageData.modifier
                        .clickable(onClick = { onImageClick(url) }),
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
