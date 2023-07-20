package ltd.ucode.network.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.type.CommentView
import ltd.ucode.network.lemmy.data.type.CommunityModeratorView
import ltd.ucode.network.lemmy.data.type.PersonView
import ltd.ucode.network.lemmy.data.type.PostView

@Serializable
data class GetPersonDetailsResponse (
    @SerialName("person_view") val personView: PersonView,
    val comments: List<CommentView>,
    val posts: List<PostView>,
    val moderates: List<CommunityModeratorView>,
) : IResponse() {
}
