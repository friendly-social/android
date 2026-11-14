package friendly.android

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.mikepenz.markdown.model.rememberMarkdownState

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
    SelectionContainer {
        FriendlyMarkdown(
            markdownState = markdownState,
            onImageClick = onImageClick,
            imageWidth = imageWidth,
            modifier = modifier,
        )
    }
}
