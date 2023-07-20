package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.data.content.ContentDatabase
import ltd.ucode.slide.data.auth.CredentialDatabase
import javax.inject.Inject

class AccountRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val contentDatabase: ContentDatabase,
    private val credentialDatabase: CredentialDatabase,
) {
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
        get() = credentialDatabase.accounts
}
