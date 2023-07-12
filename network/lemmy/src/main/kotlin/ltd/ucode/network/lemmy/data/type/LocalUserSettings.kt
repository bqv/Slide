package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.LocalUserId
import ltd.ucode.network.lemmy.data.id.PersonId

@Serializable
data class LocalUserSettings (
    @SerialName("accepted_application") val isApplicationAccepted: Boolean,
    @SerialName("default_listing_type") val defaultListingType: PostListingType,
    @SerialName("default_sort_type") val defaultSortType: PostSortType,
    val email: String? = null,
    @SerialName("email_verified") val isEmailVerified: Boolean,
    val id: LocalUserId,
    @SerialName("interface_language") val interfaceLanguage: String,
    @SerialName("person_id") val personId: PersonId,
    @SerialName("send_notifications_to_email") val isEmailNotificationsEnabled: Boolean,
    @SerialName("show_avatars") val isShowAvatarsEnabled: Boolean,
    @SerialName("show_bot_accounts") val isShowBotAccountsEnabled: Boolean,
    @SerialName("show_new_post_notifs") val isShowNewPostNotifsEnabled: Boolean,
    @SerialName("show_nsfw") val isShowNsfwEnabled: Boolean,
    @SerialName("show_read_posts") val isShowReadPostsEnabled: Boolean,
    @SerialName("show_scores") val isShowScoresEnabled: Boolean,
    val theme: String,
    @SerialName("validator_time") val validatorTime: String,
)
