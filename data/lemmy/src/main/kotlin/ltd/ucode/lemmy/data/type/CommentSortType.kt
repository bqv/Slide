package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable
import net.dean.jraw.models.CommentSort

@Serializable
enum class CommentSortType {
    Hot,
    New,
    Old,
    Top;

    companion object {
        fun from(
            commentSort: CommentSort?
        ): CommentSortType? {
            return when (commentSort) {
                CommentSort.HOT -> Hot
                CommentSort.CONFIDENCE -> Hot // TODO: rename
                CommentSort.LIVE -> New // TODO: remove
                CommentSort.NEW -> New
                CommentSort.QA -> Old // TODO: rename
                CommentSort.OLD -> Old
                CommentSort.TOP -> Top
                CommentSort.CONTROVERSIAL -> Top // TODO: rename
                CommentSort.RANDOM -> Top
                null -> Top
            }
        }
    }
}

