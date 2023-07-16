package ltd.ucode.slide.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.slide.data.ContentDatabase
import javax.inject.Inject

class CredentialDatabase @Inject constructor(
    @ApplicationContext val context: Context,
    private val contentDatabase: ContentDatabase,
) {
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

    val accounts: Set<String>
        get() = tokenStore.all.keys

    fun get(username: String, instance: String): Credential
        = tokenStore.getString("${username}@${instance}", null)?.let(::Credential)
        ?: throw NoSuchElementException("${username}@${instance}")

    fun set(username: String, instance: String, credential: Credential)
        = tokenStore.edit { putString("${username}@${instance}", credential.string) }

    fun delete(username: String, instance: String)
        = tokenStore.edit(commit = true) { remove("${username}@${instance}") }

}
