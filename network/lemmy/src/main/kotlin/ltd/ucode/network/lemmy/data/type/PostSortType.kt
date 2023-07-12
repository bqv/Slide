package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.Serializable
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod

@Serializable
enum class PostSortType {
    Active,
    Hot,
    MostComments,
    New,
    NewComments,
    Old,
    TopHour,
    TopSixHour,
    TopTwelveHour,
    TopDay,
    TopWeek,
    TopMonth,
    TopThreeMonths,
    TopSixMonths,
    TopNineMonths,
    TopYear,
    TopAll,
    ;

    companion object {
        fun from(
            submissionSort: Sorting?,
            submissionTimePeriod: TimePeriod?
        ): PostSortType? {
            return when (submissionSort) {
                Sorting.HOT -> Hot
                Sorting.BEST -> Active // TODO: rename
                Sorting.GILDED -> null // TODO: remove
                Sorting.NEW -> New
                Sorting.RISING -> NewComments // TODO: rename
                Sorting.CONTROVERSIAL -> Old // TODO: rename
                Sorting.TOP -> when (submissionTimePeriod) {
                    TimePeriod.HOUR -> TopDay // TODO: remove
                    TimePeriod.DAY -> TopDay
                    TimePeriod.WEEK -> TopWeek
                    TimePeriod.MONTH -> TopMonth
                    TimePeriod.YEAR -> TopYear
                    TimePeriod.ALL -> TopAll
                    null -> TopAll
                }
                Sorting.COMMENTS -> MostComments
                null -> null
            }
        }
    }
}

