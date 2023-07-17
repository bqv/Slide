package ltd.ucode.slide.data.value

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

sealed class Period {
    private val afterFixed: Instant?
    private val beforeFixed: Instant?
    private val duration: Duration?

    constructor(after: Instant? = null, before: Instant? = null) {
        this.afterFixed = after
        this.beforeFixed = before
        this.duration = null
    }

    private constructor(duration: Duration? = null) {
        this.afterFixed = null
        this.beforeFixed = null
        this.duration = duration
    }

    val after: Instant?
        get() = duration?.let(Clock.System.now()::minus) ?: afterFixed

    val before: Instant?
        get() = duration?.let { Clock.System.now() } ?: beforeFixed

    object Hour : Period(1.hours)
    object SixHour : Period(6.hours)
    object TwelveHour : Period(12.hours)
    object Day : Period(1.days)
    object Week : Period(7.days)
    object Month : Period(31.days)
    object ThreeMonths : Period((31 * 3).days)
    object SixMonths : Period((31 * 6).days)
    object NineMonths : Period((31 * 9).days)
    object Year : Period(365.days)
    object All : Period()

    companion object {}
}
