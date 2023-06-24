package ltd.ucode.lemmy.repository

import android.content.Context
import ltd.ucode.lemmy.api.InstanceDataSource
import ltd.ucode.lemmy.data.type.NodeInfoResult

class InstanceRepository(
    val context: Context
) {
    private val instances: MutableMap<String, InstanceDataSource> = mutableMapOf()

    //fun getPosts(): PostView
    //fun getPost(): PostView

    suspend fun getNodeInfo(instance: String): NodeInfoResult {
        return with(instances[instance]!!) {
            nodeInfo().let {
                NodeInfoResult(
                    it,
                    it.links.first().href
                        .let { url -> nodeInfo20(url) } // lets hope
                )
            }
        }
    }
}
