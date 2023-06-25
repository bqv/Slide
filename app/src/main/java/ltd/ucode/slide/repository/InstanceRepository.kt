package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.lemmy.api.InstanceDataSource
import ltd.ucode.lemmy.data.type.NodeInfoResult
import javax.inject.Inject

class InstanceRepository @Inject constructor(
    @ApplicationContext val context: Context
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
