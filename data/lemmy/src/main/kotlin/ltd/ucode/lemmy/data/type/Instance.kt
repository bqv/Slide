package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.InstanceId

@Serializable
data class Instance (
    val id: InstanceId,
    val domain: String,
    val published: LocalDateTime,
    val updated: LocalDateTime? = null,
    val software: String? = null,
    val version: String? = null,
)
