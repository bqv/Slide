package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocalUserSettingsView (
    val counts: PersonAggregates,
    @SerialName("local_user") val localUser: LocalUserSettings,
    val person: PersonSafe,
)
