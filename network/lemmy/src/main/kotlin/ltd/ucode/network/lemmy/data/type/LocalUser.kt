package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.LocalUserId
import ltd.ucode.network.lemmy.data.id.PersonId

@Serializable
data class LocalUser (
    val id: LocalUserId,
    @SerialName("person_id") val personId: PersonId,
    val email: String? = null,
    @SerialName("show_nsfw") val showNsfw: Boolean,
    val theme: String,
    @SerialName("default_sort_type") val defaultSortType: PostSortType,
    @SerialName("default_listing_type") val defaultListingType: PostListingType,
    @SerialName("interface_language") val interfaceLanguage: String,
    @SerialName("show_avatars") val showAvatars: Boolean,
    @SerialName("send_notifications_to_email") val sendNotificationsToEmail: Boolean,
    @SerialName("validator_time") val validatorTime: String,
    @SerialName("show_scores") val showScores: Boolean,
    @SerialName("show_bot_accounts") val showBotAccounts: Boolean,
    @SerialName("show_read_posts") val showReadPosts: Boolean,
    @SerialName("show_new_post_notifs") val showNewPostNotifs: Boolean,
    @SerialName("email_verified") val isEmailVerified: Boolean,
    @SerialName("accepted_application") val isApplicationAccepted: Boolean,
    @SerialName("totp_2fa_url") val totp2faUrl: String? = null,
)
