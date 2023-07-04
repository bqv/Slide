package ltd.ucode.slide.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.entity.Instance
import ltd.ucode.slide.data.entity.InstanceWithTaglines
import ltd.ucode.slide.data.entity.Tagline

@Dao
interface InstanceDao {
    @Query("SELECT * FROM instances")
    fun flowAll(): Flow<Instance>

    @Query("SELECT * FROM instances")
    suspend fun queryAll(): List<Instance>

    @Query("SELECT * FROM instances " +
            "ORDER BY users_monthly DESC")
    fun flowAllByUsers(): Flow<Instance>

    @Query("SELECT * FROM instances " +
            "ORDER BY users_monthly DESC")
    suspend fun queryAllByUsers(): List<Instance>

    @Query("SELECT * FROM instances " +
            "WHERE rowid = :id")
    fun get(id: Int): Instance

    @Query("SELECT * FROM instances " +
            "WHERE rowid = :id")
    suspend fun query(id: Int): Instance

    @Query("SELECT * FROM instances " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC")
    fun get(name: String): List<Instance>

    @Query("SELECT * FROM instances " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC")
    suspend fun query(name: String): List<Instance>

    @Query("SELECT * FROM taglines " +
            "WHERE instance_id = :id ")
    suspend fun queryTaglines(id: Int): List<Tagline>

    @Query("SELECT * FROM instances " +
            "WHERE rowid = :id ")
    fun getWithTaglines(id: Int): InstanceWithTaglines

    @Query("SELECT * FROM instances " +
            "WHERE rowid = :id ")
    suspend fun queryWithTaglines(id: Int): Map<Instance, List<Tagline>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(instance: Instance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(instance: Instance)

    @Insert
    suspend fun addAll(instances: List<Instance>)

    @Update
    suspend fun update(instance: Instance)

    @Delete
    suspend fun delete(instance: Instance)
}
