package friendly.android

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.model.rememberMarkdownState

@Composable
fun ProfileMarkdown(
    markdown: String,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val imageWidth = maxWidth
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
                modifier = Modifier,
            )
        }
    }
}
