package ltd.ucode.slide.data.common.auth

import ltd.ucode.slide.data.auth.Credential

interface ICredentialDatabase {
    val accounts: Set<String>

    fun get(username: String, instance: String): Credential

    fun set(username: String, instance: String, credential: Credential)

    fun delete(username: String, instance: String)
}
