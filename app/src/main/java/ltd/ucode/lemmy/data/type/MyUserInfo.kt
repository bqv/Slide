package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyUserInfo (
    @SerialName("community_blocks") val communityBlocks: List<CommunityBlockView>,
    @SerialName("discussion_languages") val discussionLanguages: List<Int>,
    val follows: List<CommunityFollowerView>,
    @SerialName("local_user_view") val localUserView: LocalUserSettingsView,
    val moderates: List<CommunityModeratorView>,
    @SerialName("person_blocks") val personBlocks: List<PersonBlockView>,
)
