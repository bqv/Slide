package ltd.ucode.slide.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ltd.ucode.slide.data.dao.SiteDao
import ltd.ucode.slide.data.dao.UserDao
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.entity.Tagline
import ltd.ucode.slide.data.entity.User

@Database(version = 1,
    entities = [
        Site::class,
        Site.Image::class,
        Tagline::class,
        User::class,
        User.Image::class,
        //Post::class,
        //Post.Image::class,
    ],
    autoMigrations = [
    ],
    exportSchema = true)
@TypeConverters(Converters::class)
abstract class ContentDatabase : RoomDatabase() {
    abstract val instances: SiteDao
    abstract val users: UserDao

    companion object {
        const val filename: String = "content"
    }
}
