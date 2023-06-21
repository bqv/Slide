package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable
import ltd.ucode.slide.SettingValues.getSubmissionSort
import ltd.ucode.slide.SettingValues.getSubmissionTimePeriod
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod

@Serializable
enum class SortType {
    Active,
    Hot,
    MostComments,
    New,
    NewComments,
    Old,
    TopAll,
    TopDay,
    TopMonth,
    TopWeek,
    TopYear;

    companion object {
        fun from(
            submissionSort: Sorting?,
            submissionTimePeriod: TimePeriod?
        ): SortType? {
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

        fun forSubreddit(subreddit: String): SortType {
            return from(
                getSubmissionSort(subreddit),
                getSubmissionTimePeriod(subreddit)
            ) ?: New
        }
    }
}

