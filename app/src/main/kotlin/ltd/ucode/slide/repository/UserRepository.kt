package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.content.ContentDatabase
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.slide.data.common.entity.User
import javax.inject.Inject

class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val networkRepository: NetworkRepository,
) {
    fun getUser(userRowId: Int,
    ): Flow<User> {
        /*
        return networkRepository.dataSource
            .getUser(userRowId)
         */TODO()
    }

    fun getUserBySite(site: Site,
                      userId: Int,
    ): Flow<User> {
        /*
        return networkRepository.dataSource
            .getUser(site.name, userId)
         */TODO()
    }
}
