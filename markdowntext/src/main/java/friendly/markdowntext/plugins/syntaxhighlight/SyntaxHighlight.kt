package friendly.markdowntext.plugins.syntaxhighlight

import org.commonmark.node.CustomNode
import org.commonmark.node.Delimited

class SyntaxHighlight(val textLiteral: String) :
    CustomNode(),
    Delimited {
    override fun getOpeningDelimiter(): String = DELIMITER

    override fun getClosingDelimiter(): String = DELIMITER

    companion object {
        private const val DELIMITER = "=="
    }
}
