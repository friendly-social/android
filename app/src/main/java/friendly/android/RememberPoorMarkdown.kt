package friendly.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily.Companion.Monospace
import androidx.compose.ui.text.withStyle
import com.mikepenz.markdown.annotator.DefaultAnnotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotator
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode

@Composable
fun rememberPoorMarkdown(markdown: String, style: TextStyle): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    return remember(markdown, style, linkColor) {
        markdown.buildMarkdownAnnotatedString(
            style = style,
            annotatorSettings = DefaultAnnotatorSettings(
                linkTextSpanStyle = TextLinkStyles(
                    SpanStyle(color = linkColor),
                ),
                codeSpanStyle = SpanStyle(fontFamily = Monospace),
                highlightSpanStyle = SpanStyle(),
                annotator = poorMarkdownAnnotator(linkColor),
            ),
        )
    }
}

private fun poorMarkdownAnnotator(linkColor: Color): MarkdownAnnotator {
    return markdownAnnotator { content, child ->
        return@markdownAnnotator when (child.type) {
            MarkdownElementTypes.IMAGE -> {
                withStyle(SpanStyle(color = linkColor)) {
                    append("\uD83D\uDCCE attachment")
                }
                true
            }

            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            -> {
                val textNode = child
                    .findChildOfType(MarkdownElementTypes.LINK_TEXT)
                    ?: child.findChildOfType(MarkdownElementTypes.LINK_LABEL)
                val text = (textNode ?: child)
                    .getTextInNode(content).toString()
                    .removeSurrounding("[", "]")
                withStyle(SpanStyle(color = linkColor)) { append(text) }
                true
            }

            else -> false
        }
    }
}
