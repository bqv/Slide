package ltd.ucode.slide.util

import ltd.ucode.network.lemmy.data.type.CommentSortType
import net.dean.jraw.models.CommentSort

object CommentSortTypeExtensions {
    fun CommentSortType.Companion.from(
        commentSort: CommentSort?
    ): CommentSortType? {
        return when (commentSort) {
            CommentSort.HOT -> CommentSortType.Hot
            CommentSort.CONFIDENCE -> CommentSortType.Hot // TODO: rename
            CommentSort.LIVE -> CommentSortType.New // TODO: remove
            CommentSort.NEW -> CommentSortType.New
            CommentSort.QA -> CommentSortType.Old // TODO: rename
            CommentSort.OLD -> CommentSortType.Old
            CommentSort.TOP -> CommentSortType.Top
            CommentSort.CONTROVERSIAL -> CommentSortType.Top // TODO: rename
            CommentSort.RANDOM -> CommentSortType.Top
            null -> CommentSortType.Top
        }
    }
}
