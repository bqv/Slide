package ltd.ucode.lemmy

import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

object Markdown {
    private val flavour = CommonMarkFlavourDescriptor()

    fun parseToHtml(s: String?): String? {
        if (s == null) return null
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(s)
        return HtmlGenerator(s, parsedTree, flavour).generateHtml()
    }
}
