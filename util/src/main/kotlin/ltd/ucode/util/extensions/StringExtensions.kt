package ltd.ucode.util.extensions

import java.util.regex.Pattern

object StringExtensions {
    private var htmlPattern: Pattern = Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL)

    val String.isHTML: Boolean get() = htmlPattern.matcher(this).matches()
}
