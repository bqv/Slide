package ltd.ucode.slide.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.slide.Authentication
import javax.inject.Inject

class AccountRepository @Inject constructor(
    @ApplicationContext val context: Context,
) {
    private val passwordStore: SharedPreferences by lazy {
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
        get() = passwordStore.all.keys

    fun getPassword(username: String, instance: String): String?
            = passwordStore.getString("${username}@${instance}", null)

    fun setPassword(username: String, instance: String, password: String)
            = passwordStore.edit().putString("${username}@${instance}", password).apply()

    fun deletePassword(username: String, instance: String)
            = passwordStore.edit().remove("${username}@${instance}").apply()
}
