package ltd.ucode.network.lemmy.data.type.webfinger

import kotlinx.serialization.Serializable

@Serializable
data class Usage (
    val users: Users,
    val localPosts: Int? = null,
    val localComments: Int? = null,
)
