package ltd.ucode.slide.data.common.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.slide.data.common.entity.Tagline
import ltd.ucode.slide.data.common.partial.SiteMetadataPartial

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ")
    fun flowAll(): Flow<List<Site>>

    @Query("SELECT * FROM sites ")
    suspend fun queryAll(): List<Site>

    @Query("SELECT * FROM sites ")
    fun pagingSource(): PagingSource<Int, Site>

    @Query("SELECT * FROM sites " +
            "ORDER BY users_monthly DESC ")
    fun flowAllByUsers(): Flow<List<Site>>

    @Query("SELECT * FROM sites " +
            "ORDER BY users_monthly DESC ")
    suspend fun queryAllByUsers(): List<Site>

    @Query("SELECT * FROM sites " +
            "WHERE software = :software " +
            "ORDER BY users_monthly DESC ")
    fun flowAllBySoftware(software: String): Flow<List<Site>>

    @Query("SELECT * FROM sites " +
            "WHERE rowid = :rowId ")
    fun flow(rowId: Int): Flow<Site>

    @Query("SELECT * FROM sites " +
            "WHERE rowid = :rowId ")
    fun get(rowId: Int): Site

    @Query("SELECT * FROM sites " +
            "WHERE rowid = :rowId ")
    suspend fun query(rowId: Int): Site

    @Query("SELECT * FROM sites " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC " +
            "LIMIT 1 ")
    fun flow(name: String): Flow<Site>

    @Query("SELECT * FROM sites " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC ")
    fun get(name: String): List<Site>

    @Query("SELECT * FROM sites " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC ")
    suspend fun query(name: String): List<Site>

    @Query("SELECT s2.rowid FROM sites s1 " +
        "INNER JOIN site_images si ON si.local_site_rowid = s1.rowid " +
        "INNER JOIN sites s2 ON s2.rowid = si.remote_site_rowid " +
        "WHERE s1.name LIKE :siteName AND si.remote_site_id = :siteId ")
    fun flow(siteId: Int, siteName: String): Flow<Int>

    @Query("SELECT s2.rowid FROM sites s1 " +
        "INNER JOIN site_images si ON si.local_site_rowid = s1.rowid " +
        "INNER JOIN sites s2 ON s2.rowid = si.remote_site_rowid " +
        "WHERE s1.name LIKE :siteName AND si.remote_site_id = :siteId ")
    fun get(siteId: Int, siteName: String): Int

    @Query("SELECT s2.rowid FROM sites s1 " +
        "INNER JOIN site_images si ON si.local_site_rowid = s1.rowid " +
        "INNER JOIN sites s2 ON s2.rowid = si.remote_site_rowid " +
        "WHERE s1.name LIKE :siteName AND si.remote_site_id = :siteId ")
    suspend fun query(siteId: Int, siteName: String): Int

    @Query("SELECT * FROM taglines " +
            "WHERE site_rowid = :rowId ")
    suspend fun queryTaglines(rowId: Int): List<Tagline>

    @Query("SELECT * FROM sites s " +
            "INNER JOIN taglines t ON t.site_rowid = s.rowid " +
            "WHERE s.rowid = :rowId ")
    suspend fun queryWithTaglines(rowId: Int): Map<Site, List<Tagline>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(site: Site)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(site: Site)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureAll(sites: List<Site>)

    @Upsert
    suspend fun upsert(site: Site)

    @Upsert(entity = Site::class)
    suspend fun upsert(site: SiteMetadataPartial)

    @Upsert
    suspend fun upsert(taglines: List<Tagline>)

    @Update
    suspend fun update(site: Site)

    @Delete
    suspend fun delete(site: Site)
}
