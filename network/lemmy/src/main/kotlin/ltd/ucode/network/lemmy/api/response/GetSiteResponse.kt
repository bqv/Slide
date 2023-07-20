package ltd.ucode.network.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.type.FederatedInstances
import ltd.ucode.network.lemmy.data.type.Language
import ltd.ucode.network.lemmy.data.type.MyUserInfo
import ltd.ucode.network.lemmy.data.type.PersonView
import ltd.ucode.network.lemmy.data.type.SiteView
import ltd.ucode.network.lemmy.data.type.Tagline

@Serializable
data class GetSiteResponse (
    @SerialName("site_view") val siteView: SiteView,
    val admins: List<PersonView>,
    val online: Int = -1, // REMOVED in 0.18.0
    val version: String,
    @SerialName("my_user") val myUser: MyUserInfo? = null,
    @SerialName("federated_instances") val federatedInstances: FederatedInstances? = null,
    @SerialName("all_languages") val allLanguages: List<Language>,
    @SerialName("discussion_languages") val discussionLanguages: List<Int>,
    val taglines: List<Tagline>? = null,
) : IResponse() {
    val domain: String
        get() = siteView.site.actorId.host
}
