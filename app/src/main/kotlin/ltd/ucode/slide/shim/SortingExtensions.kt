package ltd.ucode.slide.shim

import ltd.ucode.slide.data.value.Sorting
import net.dean.jraw.paginators.Sorting as RedditSorting

object SortingExtensions {
    fun Sorting.Companion.from(
        submissionSort: RedditSorting?,
    ): Sorting? {
        return when (submissionSort) {
            RedditSorting.HOT -> Sorting.Hot(active = false)
            RedditSorting.BEST -> Sorting.Hot(active = true) // TODO: rename
            RedditSorting.GILDED -> null // TODO: remove
            RedditSorting.NEW -> Sorting.New(comments = false)
            RedditSorting.RISING -> Sorting.New(comments = true) // TODO: rename
            RedditSorting.CONTROVERSIAL -> Sorting.Old(controversial = true) // TODO: rename
            RedditSorting.TOP -> Sorting.Top(comments = false)
            RedditSorting.COMMENTS -> Sorting.Top(comments = true)
            null -> null
        }
    }
}
