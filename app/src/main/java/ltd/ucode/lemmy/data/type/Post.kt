package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post (
    @SerialName("ap_id") val apId: String,
    val body: String? = null,
    @SerialName("community_id") val communityId: Int,
    @SerialName("creator_id") val creatorId: Int,
    @SerialName("deleted") val isDeleted: Boolean,
    @SerialName("embed_description") val embedDescription: String? = null,
    @SerialName("embed_title") val embedTitle: String? = null,
    @SerialName("embed_video_url") val embedVideoUrl: String? = null,
    @SerialName("featured_community") val isFeaturedCommunity: Boolean,
    @SerialName("featured_local") val isFeaturedLocal: Boolean,
    val id: Int,
    @SerialName("language_id") val languageId: Int,
    @SerialName("local") val isLocal: Boolean,
    @SerialName("locked") val isLocked: Boolean,
    val name: String,
    @SerialName("nsfw") val isNsfw: Boolean,
    val published: LocalDateTime,
    @SerialName("removed") val isRemoved: Boolean,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val updated: LocalDateTime? = null,
    val url: String? = null,
)
