package ltd.ucode.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.FederatedInstances
import ltd.ucode.lemmy.data.type.Language
import ltd.ucode.lemmy.data.type.MyUserInfo
import ltd.ucode.lemmy.data.type.PersonViewSafe
import ltd.ucode.lemmy.data.type.SiteView
import ltd.ucode.lemmy.data.type.Tagline

@Serializable
data class GetSiteResponse (
    @SerialName("site_view") val siteView: SiteView,
    val admins: List<PersonViewSafe>,
    val online: Int,
    val version: String,
    @SerialName("my_user") val myUser: MyUserInfo? = null,
    @SerialName("federated_instances") val federatedInstances: FederatedInstances? = null,
    @SerialName("all_languages") val allLanguages: List<Language>,
    @SerialName("discussion_languages") val discussionLanguages: List<Int>,
    val taglines: List<Tagline>? = null,
) {
    fun toResult(): GetSiteResponse {
        return this
    }
}
