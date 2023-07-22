package ltd.ucode.slide.data.common.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.slide.data.common.entity.Tagline
import ltd.ucode.slide.data.common.partial.SiteImageMetadataPartial
import ltd.ucode.slide.data.common.partial.SiteMetadataPartial

@Dao
interface SiteDao {
    @Query("SELECT * FROM _site ")
    fun flowAll(): Flow<List<Site>>

    @Query("SELECT * FROM _site ")
    suspend fun queryAll(): List<Site>

    @Query("SELECT * FROM _site ")
    fun pagingSource(): PagingSource<Int, Site>

    @Query("SELECT * FROM _site " +
            "ORDER BY users_monthly DESC ")
    fun flowAllByUsers(): Flow<List<Site>>

    @Query("SELECT * FROM _site " +
            "ORDER BY users_monthly DESC ")
    suspend fun queryAllByUsers(): List<Site>

    @Query("SELECT * FROM _site " +
            "WHERE software = :software " +
            "ORDER BY users_monthly DESC ")
    fun flowAllBySoftware(software: String): Flow<List<Site>>

    @Query("SELECT * FROM _site " +
            "WHERE rowid = :rowId ")
    fun flow(rowId: Long): Flow<Site>

    @Query("SELECT * FROM _site " +
            "WHERE rowid = :rowId ")
    fun get(rowId: Long): Site?

    @Query("SELECT * FROM _site " +
            "WHERE rowid = :rowId ")
    suspend fun query(rowId: Long): Site?

    @Query("SELECT * FROM _site " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC " +
            "LIMIT 1 ")
    fun flow(name: String): Flow<Site>

    @Query("SELECT * FROM _site " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC ")
    fun get(name: String): Site?

    @Query("SELECT * FROM _site " +
            "WHERE name LIKE :name " +
            "ORDER BY users_monthly DESC ")
    suspend fun query(name: String): Site?

    @Query("SELECT s2.* FROM _site s1 " +
        "INNER JOIN _site_image si ON si.local_site_rowid = s1.rowid " +
        "INNER JOIN _site s2 ON s2.rowid = si.remote_site_rowid " +
        "WHERE s1.name LIKE :siteName AND si.remote_site_id = :siteId ")
    fun flow(siteId: Int, siteName: String): Flow<Site>

    @Query("SELECT s2.* FROM _site s1 " +
        "INNER JOIN _site_image si ON si.local_site_rowid = s1.rowid " +
        "INNER JOIN _site s2 ON s2.rowid = si.remote_site_rowid " +
        "WHERE s1.name LIKE :siteName AND si.remote_site_id = :siteId ")
    fun get(siteId: Int, siteName: String): Site?

    @Query("SELECT s2.* FROM _site s1 " +
        "INNER JOIN _site_image si ON si.local_site_rowid = s1.rowid " +
        "INNER JOIN _site s2 ON s2.rowid = si.remote_site_rowid " +
        "WHERE s1.name LIKE :siteName AND si.remote_site_id = :siteId ")
    suspend fun query(siteId: Int, siteName: String): Site?

    @Query("SELECT * FROM _tagline " +
            "WHERE site_rowid = :rowId ")
    suspend fun queryTaglines(rowId: Long): List<Tagline>

    @Query("SELECT * FROM _site s " +
            "INNER JOIN _tagline t ON t.site_rowid = s.rowid " +
            "WHERE s.rowid = :rowId ")
    suspend fun queryWithTaglines(rowId: Long): Map<Site, List<Tagline>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(site: Site)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(site: Site): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun upsert(image: Site.Image): Long

    @Insert(entity = Site.Image::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun ensureAll(sites: List<SiteImageMetadataPartial>): List<Long>

    @Insert
    suspend fun insert(site: Site)

    suspend fun upsertManual(site: Site) {
        try {
            insert(site)
        } catch (ex: SQLiteConstraintException) {
            update(site.copy(rowId = query(site.name)!!.rowId))
        }
    }

    @Update
    suspend fun update(site: Site)

    @Insert(entity = Site::class)
    suspend fun insert(site: SiteMetadataPartial)

    @Insert(entity = Site::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(site: SiteMetadataPartial): Long

    @Insert(entity = Site.Image::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(site: SiteImageMetadataPartial): Long

    suspend fun upsertManual(site: SiteMetadataPartial) {
        try {
            insert(site)
        } catch (ex: SQLiteConstraintException) {
            update(site.copy(rowId = query(site.name)!!.rowId))
        }
    }

    @Update(entity = Site::class)
    suspend fun update(site: SiteMetadataPartial)

    @Update
    suspend fun update(taglines: List<Tagline>)

    @Delete
    suspend fun delete(site: Site)
}
