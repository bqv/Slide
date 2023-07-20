package ltd.ucode.slide.data.common.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Entity(tableName = "timestamps")
data class Timestamp(
    @PrimaryKey val table: String, // just to be difficult :)
    val stamp: Instant = Clock.System.now(),
) {
    // Check table against (SELECT name FROM sqlite_master WHERE type='table')
}
