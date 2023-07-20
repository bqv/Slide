package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import ltd.ucode.network.lemmy.data.type.CommunityView
import ltd.ucode.slide.data.content.ContentDatabase
import ltd.ucode.slide.data.common.entity.Group
import ltd.ucode.slide.data.common.entity.Site
import javax.inject.Inject

class GroupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val networkRepository: NetworkRepository,
) {
    fun listCommunities(instance: String?
    ): Flow<List<CommunityView>> {
        /*
        return networkRepository.dataSource
            .getGroups(instance ?: networkRepository.defaultInstance)
         */TODO()
    }

    fun getGroupBySiteId(site: Site,
                         groupId: Int,
    ): Flow<Group> {
        /*
        return networkRepository.dataSource
            .getGroup(site.name, groupId)
         */TODO()
    }
}
