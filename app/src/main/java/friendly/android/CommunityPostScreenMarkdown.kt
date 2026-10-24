package friendly.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes

@Composable
fun CommunityPostMarkdown(
    markdown: String,
    imageWidth: Dp,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val markdownState = rememberMarkdownState(
        retainState = true,
        immediate = true,
        content = markdown,
    )
    val isDarkTheme = isSystemInDarkTheme()
    val highlightsBuilder = remember(isDarkTheme) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
    }
    SelectionContainer {
        Markdown(
            markdownState = markdownState,
            imageTransformer = SquarePictureMarkdownImageTransformer(imageWidth),
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
            modifier = modifier.fillMaxSize(),
        )
    }
}
