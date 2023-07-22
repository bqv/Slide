package ltd.ucode.slide.data.common.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.common.entity.Group

@Dao
interface GroupDao {
    @Query("SELECT * FROM _group " +
            "WHERE rowid = :rowId ")
    fun get(rowId: Long): Group?

    @Query("SELECT * FROM _group " +
            "WHERE rowid = :rowid ")
    suspend fun query(rowid: Long): Group?

    @Query("SELECT * FROM _group_image AS gi " +
            "INNER JOIN _group AS g ON g.rowid = gi.group_rowid " +
            "INNER JOIN _site AS s ON s.rowid = gi.site_rowid " +
            "WHERE gi.group_id = :groupId AND s.rowid = :siteRowId ")
    fun flow(groupId: Int, siteRowId: Long): Flow<Group>

    @Query("SELECT g.* FROM _group_image AS gi " +
            "INNER JOIN _group AS g ON g.rowid = gi.group_rowid " +
            "INNER JOIN _site AS s ON s.rowid = gi.site_rowid " +
            "WHERE gi.group_id = :groupId AND s.name LIKE :siteName ")
    fun get(groupId: Int, siteName: String): Group?

    @Query("SELECT * FROM _group AS g " +
            "INNER JOIN _site AS s ON s.rowid = g.site_rowid " +
            "WHERE g.rowid = :groupId AND s.name LIKE :instanceName ")
    suspend fun query(groupId: Int, instanceName: String): List<Group>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(group: Group)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(group: Group)

    @Insert
    suspend fun addAll(groups: List<Group>)

    @Update
    suspend fun update(group: Group)

    @Delete
    suspend fun delete(group: Group)
}
