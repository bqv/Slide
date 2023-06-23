package ltd.ucode.lemmy.api

import ltd.ucode.lemmy.data.LemmyAccount

class AccountDataSource(
    val account: LemmyAccount,
    instance: String,
    headers: Map<String, String> = mapOf(),
) : InstanceDataSource(instance, headers) {
}
