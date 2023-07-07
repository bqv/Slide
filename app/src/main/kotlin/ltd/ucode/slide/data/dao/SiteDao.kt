package ltd.ucode.slide.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.entity.Tagline
import ltd.ucode.slide.data.unified.InstanceJoin

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ")
    fun flowAll(): Flow<Site>

    @Query("SELECT * FROM sites ")
    suspend fun queryAll(): List<Site>

    @Query("SELECT * FROM sites " +
            "ORDER BY users_monthly DESC ")
    fun flowAllByUsers(): Flow<Site>

    @Query("SELECT * FROM sites " +
            "ORDER BY users_monthly DESC ")
    suspend fun queryAllByUsers(): List<Site>

    @Query("SELECT * FROM sites " +
            "WHERE rowid = :rowId ")
    fun get(rowId: Int): Site

    @Query("SELECT * FROM sites " +
            "WHERE rowid = :rowId ")
    suspend fun query(rowId: Int): Site

    @Query("SELECT * FROM sites " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC ")
    fun get(name: String): List<Site>

    @Query("SELECT * FROM sites " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC ")
    suspend fun query(name: String): List<Site>

    @Query("SELECT * FROM taglines " +
            "WHERE instance_rowid = :rowId ")
    suspend fun queryTaglines(rowId: Int): List<Tagline>

    @Query("SELECT * FROM sites " +
            "WHERE rowid = :rowId ")
    fun getWithTaglines(rowId: Int): InstanceJoin

    @Query("SELECT * FROM sites s " +
            "INNER JOIN taglines t ON t.instance_rowid = s.rowid " +
            "WHERE rowid = :rowId ")
    suspend fun queryWithTaglines(rowId: Int): Map<Site, List<Tagline>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(instance: Site)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(instance: Site)

    @Insert
    suspend fun addAll(sites: List<Site>)

    @Update
    suspend fun update(instance: Site)

    @Delete
    suspend fun delete(instance: Site)
}
