package ltd.ucode.slide.util

import ltd.ucode.network.lemmy.data.type.PostSortType
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod

object PostSortTypeExtensions {
    fun PostSortType.Companion.from(
        submissionSort: Sorting?,
        submissionTimePeriod: TimePeriod?
    ): PostSortType? {
        return when (submissionSort) {
            Sorting.HOT -> PostSortType.Hot
            Sorting.BEST -> PostSortType.Active // TODO: rename
            Sorting.GILDED -> null // TODO: remove
            Sorting.NEW -> PostSortType.New
            Sorting.RISING -> PostSortType.NewComments // TODO: rename
            Sorting.CONTROVERSIAL -> PostSortType.Old // TODO: rename
            Sorting.TOP -> when (submissionTimePeriod) {
                TimePeriod.HOUR -> PostSortType.TopDay // TODO: remove
                TimePeriod.DAY -> PostSortType.TopDay
                TimePeriod.WEEK -> PostSortType.TopWeek
                TimePeriod.MONTH -> PostSortType.TopMonth
                TimePeriod.YEAR -> PostSortType.TopYear
                TimePeriod.ALL -> PostSortType.TopAll
                null -> PostSortType.TopAll
            }

            Sorting.COMMENTS -> PostSortType.MostComments
            null -> null
        }
    }
}
