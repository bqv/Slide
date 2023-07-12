package ltd.ucode.network.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.type.CommunityModeratorView
import ltd.ucode.network.lemmy.data.type.CommunityView
import ltd.ucode.network.lemmy.data.id.LanguageId
import ltd.ucode.network.lemmy.data.type.Site

@Serializable
data class GetCommunityResponse (
    @SerialName("community_view") val communityView: CommunityView,
    val site: Site? = null,
    val moderators: List<CommunityModeratorView>,
    val online: Int = -1, // REMOVED in 0.18.0
    @SerialName("discussion_languages") val discussionLanguages: List<LanguageId>,
    @SerialName("default_post_language") val defaultPostLanguage: LanguageId? = null,
)
