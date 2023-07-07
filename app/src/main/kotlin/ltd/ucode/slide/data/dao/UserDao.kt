package ltd.ucode.slide.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ltd.ucode.slide.data.entity.User

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

    @Query("SELECT * FROM users AS u " +
            "INNER JOIN instances AS i ON i.rowid = u.instance_rowid " +
            "WHERE u.name LIKE :name AND i.name LIKE :instanceName ")
    fun get(name: String, instanceName: String): List<User>

    @Query("SELECT * FROM users AS u " +
            "INNER JOIN instances AS i ON i.rowid = u.instance_rowid " +
            "WHERE u.name LIKE :name AND i.name LIKE :instanceName ")
    suspend fun query(name: String, instanceName: String): List<User>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(instance: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(instance: User)

    @Insert
    suspend fun addAll(users: List<User>)

    @Update
    suspend fun update(instance: User)

    @Delete
    suspend fun delete(instance: User)
}
