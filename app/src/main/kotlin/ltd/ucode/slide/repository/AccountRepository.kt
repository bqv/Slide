package ltd.ucode.slide.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.lemmy.api.ApiException
import javax.inject.Inject

class AccountRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val instanceRepository: InstanceRepository,
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

    val accounts: Set<String>
        get() = passwordStore.all.keys

    fun getPassword(username: String, instance: String): String?
            = passwordStore.getString("${username}@${instance}", null)

    fun setPassword(username: String, instance: String, password: String)
            = passwordStore.edit().putString("${username}@${instance}", password).apply()

    suspend fun create(username: String, password: String, instance: String) {
        passwordStore.edit().putString("${username}@${instance}", password).apply()
        try {
            val dataSource = instanceRepository.createLogin(username, password, instance)
            dataSource.getUnreadCount()
            dataSource
        } catch (e: ApiException) {
            instanceRepository.deleteLogin(username, instance)
            passwordStore.edit().remove("${username}@${instance}").apply()
            throw e
        }
    }
}
