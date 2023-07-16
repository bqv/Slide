package ltd.ucode.slide.data

import androidx.room.TypeConverter
import kotlinx.datetime.Instant

class Converters {
    @TypeConverter
    fun fetchInstant(value: Long): Instant = value.let(Instant::fromEpochMilliseconds)

    @TypeConverter
    fun storeInstant(value: Instant): Long = value.toEpochMilliseconds()
}
