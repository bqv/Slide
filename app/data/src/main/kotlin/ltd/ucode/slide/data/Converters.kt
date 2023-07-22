package ltd.ucode.slide.data

import androidx.room.TypeConverter
import kotlinx.datetime.Instant

object Converters {
    @TypeConverter
    @JvmStatic
    fun fetchInstant(value: Long?): Instant? = value?.let(Instant::fromEpochMilliseconds)

    @TypeConverter
    @JvmStatic
    fun storeInstant(value: Instant?): Long? = value?.toEpochMilliseconds()
}
