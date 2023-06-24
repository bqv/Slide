package ltd.ucode.slide.repository

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
            getNodeInfo()
        }
    }
}
