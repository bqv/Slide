package ltd.ucode.slide.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.lemmy.data.type.jwt.Token
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.data.ContentDatabase
import javax.inject.Inject

class AccountRepository @Inject constructor(
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

    companion object {
        @JvmStatic
        val currentAccount: String?
            get() {
                return Authentication.name?.let {
                    if (it.lowercase() == "loggedout") null else it
                }
            }
    }

    val accounts: Set<String>
        get() = tokenStore.all.keys

    fun getToken(username: String, instance: String): String?
            = tokenStore.getString("${username}@${instance}", null)

    fun setToken(username: String, instance: String, token: Token)
            = tokenStore.edit().putString("${username}@${instance}", token.token).apply()

    fun deleteToken(username: String, instance: String)
            = tokenStore.edit().remove("${username}@${instance}").apply()
}
