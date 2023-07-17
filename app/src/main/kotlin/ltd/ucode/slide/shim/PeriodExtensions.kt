package ltd.ucode.slide.shim

import ltd.ucode.slide.data.value.Period
import net.dean.jraw.paginators.TimePeriod as RedditTimePeriod

object PeriodExtensions {
    fun Period.Companion.from(
        submissionTimePeriod: RedditTimePeriod?,
    ): Period? {
        return when (submissionTimePeriod) {
            RedditTimePeriod.HOUR -> Period.Hour
            RedditTimePeriod.DAY -> Period.Day
            RedditTimePeriod.WEEK -> Period.Week
            RedditTimePeriod.MONTH -> Period.Month
            RedditTimePeriod.YEAR -> Period.Year
            RedditTimePeriod.ALL -> Period.All
            null -> null
        }
    }
}
