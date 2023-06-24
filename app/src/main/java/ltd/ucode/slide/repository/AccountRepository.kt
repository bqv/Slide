package ltd.ucode.slide.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AccountRepository(
    val context: Context
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

    fun getPassword(username: String, instance: String): String?
            = passwordStore.getString("${username}@${instance}", null)

    fun setPassword(username: String, instance: String, password: String)
            = passwordStore.edit().putString("${username}@${instance}", password).apply()
}
