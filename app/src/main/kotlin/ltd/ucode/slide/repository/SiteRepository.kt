package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.entity.Site
import javax.inject.Inject

class SiteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val networkRepository: NetworkRepository,
) {
    fun getSiteByName(name: String): Flow<Site> {
        return networkRepository.dataSource.getSite(name)
    }
}
