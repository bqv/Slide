package ltd.ucode.slide.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.slide.data.common.auth.ICredentialDatabase
import ltd.ucode.slide.data.content.ContentDatabase
import javax.inject.Inject

class CredentialDatabase @Inject constructor(
    @ApplicationContext val context: Context,
    private val contentDatabase: ContentDatabase,
) : ICredentialDatabase {
    private val tokenStore: SharedPreferences by lazy {
        val mainKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context, "accounts", mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override val accounts: Set<String>
        get() = tokenStore.all.keys

    override fun get(username: String, instance: String): Credential
        = tokenStore.getString("${username}@${instance}", null)?.let(::Credential)
        ?: throw NoSuchElementException("${username}@${instance}")

    override fun set(username: String, instance: String, credential: Credential)
        = tokenStore.edit { putString("${username}@${instance}", credential.string) }

    override fun delete(username: String, instance: String)
        = tokenStore.edit(commit = true) { remove("${username}@${instance}") }

}
