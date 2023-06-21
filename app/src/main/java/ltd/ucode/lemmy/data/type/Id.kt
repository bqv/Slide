package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
@JvmInline value class SiteId(val id: Int)

@Serializable
@JvmInline value class LocalSiteId(val id: Int)

@Serializable
@JvmInline value class InstanceId(val id: Int)

@Serializable
@JvmInline value class PersonId(val id: Int)

@Serializable
@JvmInline value class LocalUserId(val id: Int)

@Serializable
@JvmInline value class CommunityId(val id: Int)

@Serializable
@JvmInline value class PostId(val id: Int)

@Serializable
@JvmInline value class CommentId(val id: Int)

@Serializable
@JvmInline value class LanguageId(val id: Int)

@Serializable
@JvmInline value class LanguageCode(val code: String)
