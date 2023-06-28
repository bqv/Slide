package ltd.ucode.lemmy.data.id

import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.Instance
import ltd.ucode.lemmy.data.type.Language
import ltd.ucode.lemmy.data.type.LocalSite
import ltd.ucode.lemmy.data.type.LocalUser
import ltd.ucode.lemmy.data.type.Person
import ltd.ucode.lemmy.data.type.Site
import ltd.ucode.slide.data.IComment
import ltd.ucode.slide.data.IGroup
import ltd.ucode.slide.data.IIdentifier
import ltd.ucode.slide.data.IPost

@Serializable
@JvmInline value class SiteId(override val id: Int) : IIdentifier<Site>

@Serializable
@JvmInline value class LocalSiteId(override val id: Int) : IIdentifier<LocalSite>

@Serializable
@JvmInline value class InstanceId(override val id: Int) : IIdentifier<Instance>

@Serializable
@JvmInline value class PersonId(override val id: Int) : IIdentifier<Person>

@Serializable
@JvmInline value class LocalUserId(override val id: Int) : IIdentifier<LocalUser>

@Serializable
@JvmInline value class CommunityId(override val id: Int) : IIdentifier<IGroup>

@Serializable
@JvmInline value class PostId(override val id: Int) : IIdentifier<IPost>

@Serializable
@JvmInline value class CommentId(override val id: Int) : IIdentifier<IComment>

@Serializable
@JvmInline value class LanguageId(override val id: Int) : IIdentifier<Language>

@Serializable
@JvmInline value class LanguageCode(val code: String)
