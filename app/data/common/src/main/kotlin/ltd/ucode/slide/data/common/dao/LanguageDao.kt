package ltd.ucode.slide.data.common.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.common.entity.Language

@Dao
interface LanguageDao {
    @Query("SELECT * FROM _language " +
            "WHERE rowid = :rowId ")
    fun get(rowId: Long): Language?

    @Query("SELECT * FROM _language " +
            "WHERE rowid = :rowid ")
    suspend fun query(rowid: Long): Language?

    @Query("SELECT * FROM _language_image AS li " +
            "INNER JOIN _language AS l ON l.rowid = li.language_rowid " +
            "INNER JOIN _site AS s ON s.rowid = li.site_rowid " +
            "WHERE li.language_id = :languageId AND s.rowid = :siteRowId ")
    fun flow(languageId: Int, siteRowId: Long): Flow<Language>

    @Query("SELECT l.* FROM _language_image AS li " +
            "INNER JOIN _language AS l ON l.rowid = li.language_rowid " +
            "INNER JOIN _site AS s ON s.rowid = li.site_rowid " +
            "WHERE li.language_id = :languageId AND s.name LIKE :siteName ")
    fun get(languageId: Int, siteName: String): Language?

    @Query("SELECT * FROM _language_image AS li " +
            "INNER JOIN _language AS l ON l.rowid = li.language_rowid " +
            "INNER JOIN _site AS s ON s.rowid = li.site_rowid " +
            "WHERE li.language_id = :languageId AND s.name LIKE :siteName ")
    suspend fun query(languageId: Int, siteName: String): List<Language>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(language: Language)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(language: Language)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureAll(languages: List<Language>)

    @Upsert
    suspend fun upsert(language: Language, image: Language.Image)

    @Update
    suspend fun update(language: Language)

    @Delete
    suspend fun delete(language: Language)
}
