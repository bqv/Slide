package ltd.ucode.slide.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ltd.ucode.slide.data.dao.InstanceDao
import ltd.ucode.slide.data.entity.Instance

@Database(version = 1,
    entities = [
        Instance::class
    ],
    autoMigrations = [
    ],
    exportSchema = true)
@TypeConverters(Converters::class)
abstract class ContentDatabase : RoomDatabase() {
    abstract val instances: InstanceDao

    companion object {
        const val filename: String = "content"
    }
}
