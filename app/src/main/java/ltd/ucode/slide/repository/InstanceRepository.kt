package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import info.`the-federation`.FediverseStats
import ltd.ucode.Util
import ltd.ucode.lemmy.api.InstanceDataSource
import ltd.ucode.lemmy.data.type.NodeInfoResult
import ltd.ucode.slide.table.Instance
import okhttp3.OkHttpClient
import javax.inject.Inject

class InstanceRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val okHttpClient: OkHttpClient,
) {
    private val instances: Map<String, InstanceDataSource> = InstanceMap()

    inner class InstanceMap : HashMap<String, InstanceDataSource>() {
        override fun get(key: String): InstanceDataSource {
            return super.get(key)
                ?: InstanceDataSource(context, key, okHttpClient)
                    .also { value -> put(key, value) }
        }
    }

    suspend fun getInstanceList(limit: Int? = null): List<Instance> {
        val nodeList = FediverseStats.getLemmyServers(Util.userAgent, limit)
            ?.thefederation_node.orEmpty()

        return nodeList.map {
            val stat = it.thefederation_stats.firstOrNull()
            Instance(it.name, it.version, it.country, stat?.local_posts, stat?.local_comments,
                    stat?.users_total, stat?.users_half_year, stat?.users_monthly, stat?.users_weekly)
        }
    }

    //fun getPosts(): PostView
    //fun getPost(): PostView

    suspend fun getNodeInfo(instance: String): NodeInfoResult {
        return with(instances[instance]!!) {
            getNodeInfo()
        }
    }

    suspend fun getFederatedInstances(instance: String = "lemmy.ml"): Set<String> {
        return instances[instance]!!.getFederatedInstances().federatedInstances?.run {
            linked.toSortedSet().also {
                it.addAll(allowed.orEmpty())
                it.addAll(blocked.orEmpty())
            }
        }.orEmpty()
    }
}
