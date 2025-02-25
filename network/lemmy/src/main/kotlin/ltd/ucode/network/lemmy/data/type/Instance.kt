package ltd.ucode.network.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.InstanceId

@Serializable
data class Instance (
    val domain: String,
    val id: InstanceId,
    val published: LocalDateTime,
    val updated: LocalDateTime? = null,
    val software: String? = null,
    val version: String? = null,
)
