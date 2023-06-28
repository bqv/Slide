package ltd.ucode.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object InstantExtensions {
    val Instant.ago: Duration
        get() = DurationUnit.SECONDS.let { unit ->
            minus(Clock.System.now()).absoluteValue.toLong(unit).toDuration(unit)
        }
}
