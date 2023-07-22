package ltd.ucode.slide.data.common.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ltd.ucode.slide.data.common.entity.Seen

@Dao
interface SeenDao {
    @Query("SELECT * FROM _seen " +
            "WHERE [key] = :key ")
    fun select(key: String): Seen?

    operator fun get(key: String): String {
        var value: String? = null
        runBlocking {
            withContext(Dispatchers.IO) {
                value = select(key)?.value
            }
        }
        return value.orEmpty()
    }

    @Query("SELECT * FROM _seen " +
            "WHERE [key] LIKE '%' || :needle || '%' ")
    fun selectByContains(needle: String): List<Seen>

    fun getByContains(needle: String): Map<String, String> {
        var value: List<Pair<String, String>>
        runBlocking {
            withContext(Dispatchers.IO) {
                value = selectByContains(needle).map {
                    Pair(it.key, it.value)
                }
            }
        }
        return value.toMap()
    }

    @Query("SELECT * FROM _seen " +
        "WHERE [key] LIKE :needle || '%' ")
    fun selectByPrefix(needle: String): List<Seen>

    fun getByPrefix(needle: String): Map<String, String> {
        var value: List<Pair<String, String>>
        runBlocking {
            withContext(Dispatchers.IO) {
                value = selectByPrefix(needle).map {
                    Pair(it.key, it.value)
                }
            }
        }
        return value.toMap()
    }

    @Query("SELECT 1 FROM _seen " +
            "WHERE [key] LIKE '%' || :needle || '%' " +
            "LIMIT 1")
    fun contains(needle: String): Boolean?

    fun has(needle: String): Boolean {
        var value: Boolean = false
        runBlocking {
            withContext(Dispatchers.IO) {
                value = contains(needle) ?: false
            }
        }
        return value
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(kv: Seen)

    fun insert(key: String, value: String): Long {
        var ok = true
        runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    insert(Seen(key, value))
                } catch (ex: SQLiteConstraintException) {
                    ok = false
                }
            }
        }
        return if (ok) 0 else -1L
    }

    @Upsert
    fun upsert(kv: Seen)

    fun insertOrUpdate(key: String, value: String): Long {
        var ok = true
        runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    upsert(Seen(key, value))
                } catch (ex: SQLiteConstraintException) {
                    ok = false
                }
            }
        }
        return if (ok) 0 else -1L
    }

    @Update
    fun update(kv: Seen)

    fun update(key: String, value: String): Long {
        var ok = true
        runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    update(Seen(key, value))
                } catch (ex: SQLiteConstraintException) {
                    ok = false
                }
            }
        }
        return if (ok) 0 else -1L
    }

    @Delete
    fun delete(kv: Seen)

    fun delete(key: String): Long {
        var ok = true
        runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    select(key)?.let(::delete)
                } catch (ex: SQLiteConstraintException) {
                    ok = false
                }
            }
        }
        return if (ok) 0 else -1L
    }

    @Query("DELETE FROM _seen")
    fun delete()

    fun clearTable(): Long {
        var ok = true
        runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    delete()
                } catch (ex: SQLiteConstraintException) {
                    ok = false
                }
            }
        }
        return if (ok) 0 else -1L
    }

    @RawQuery
    fun raw(query: SupportSQLiteQuery): String
}
