package ltd.ucode.slide.data.common.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.datetime.Instant
import ltd.ucode.slide.data.common.entity.Post
import ltd.ucode.slide.data.common.entity.Timestamp
import kotlin.reflect.KClass

@Dao
abstract class TimestampDao(val database: RoomDatabase) {
    @Query("SELECT * FROM timestamps t " +
            "INNER JOIN sqlite_master m ON m.name = t.[table] AND m.type = 'table' " +
            "WHERE t.[table] = :table ")
    protected abstract fun get(table: String): Timestamp

    @Query("SELECT * FROM timestamps t " +
            "INNER JOIN sqlite_master m ON m.name = t.[table] AND m.type = 'table' " +
            "WHERE t.[table] = :table ")
    protected abstract suspend fun query(table: String): Timestamp

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun add(timestamp: Timestamp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(timestamp: Timestamp)

    var post: Instant
        get() = get(Post::class.entityAnnotation!!.tableName).stamp
        set(value) = add(Timestamp(Post::class.entityAnnotation!!.tableName, value))

    private val <T : Any> KClass<T>.entityAnnotation: Entity?
        get() = this.annotations.find { it.annotationClass == Entity::class } as? Entity
}
