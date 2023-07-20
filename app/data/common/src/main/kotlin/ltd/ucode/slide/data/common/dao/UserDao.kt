package ltd.ucode.slide.data.common.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.common.entity.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users " +
            "WHERE rowid = :rowId ")
    fun get(rowId: Int): User

    @Query("SELECT * FROM users " +
            "WHERE rowid = :rowid ")
    suspend fun query(rowid: Int): User

    @Query("SELECT * FROM users " +
            "WHERE name LIKE :name ")
    fun get(name: String): List<User>

    @Query("SELECT * FROM users " +
            "WHERE name LIKE :name ")
    suspend fun query(name: String): List<User>

    @Query("SELECT * FROM user_images AS ui " +
            "INNER JOIN users AS u ON u.rowid = ui.user_rowid " +
            "INNER JOIN sites AS s ON s.rowid = ui.site_rowid " +
            "WHERE ui.person_id = :userId AND s.rowid = :siteId ")
    fun flow(userId: Int, siteId: Int): Flow<User>

    @Query("SELECT u.rowid FROM user_images AS ui " +
            "INNER JOIN users AS u ON u.rowid = ui.user_rowid " +
            "INNER JOIN sites AS s ON s.rowid = ui.site_rowid " +
            "WHERE ui.person_id = :userId AND s.name LIKE :siteName ")
    fun get(userId: Int, siteName: String): Int

    @Query("SELECT * FROM users AS u " +
            "INNER JOIN sites AS s ON s.rowid = u.site_rowid " +
            "WHERE u.name LIKE :name AND s.name LIKE :instanceName ")
    suspend fun query(name: String, instanceName: String): List<User>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(user: User)

    @Insert
    suspend fun addAll(users: List<User>)

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)
}
