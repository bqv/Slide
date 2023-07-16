package ltd.ucode.slide.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.auth.Credential
import ltd.ucode.slide.data.auth.CredentialDatabase
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.source.NetworkDataSource
import ltd.ucode.slide.data.source.OfflineDataSource
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

private typealias Domain = String
private typealias Username = String

class NetworkRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val okHttpClient: OkHttpClient,
    @Named("userAgent") val userAgent: String,
    val contentDatabase: ContentDatabase,
    val credentialDatabase: CredentialDatabase,
) {
    var defaultInstance: String = "lemmy.ml"

    private val logger: KLogger = KotlinLogging.logger {}

    init {
        logger.info { "Creating ${javaClass.simpleName}"}
    }

    private val offlineDataSource = OfflineDataSource(contentDatabase)
    private val networkDataSource = NetworkDataSource(
        okHttpClient = okHttpClient,
        userAgent = userAgent,
        contentDatabase = contentDatabase,
        credentialDatabase = credentialDatabase,
    )

    var isOnline: Boolean = true
        private set

    val dataSource get() = if (isOnline) networkDataSource else offlineDataSource

    fun fetchInstanceList(): Flow<List<Site>> {
        return dataSource.getSites()
    }

    suspend fun connect(account: String) {
        val (username, instance) = account.split("@")
            .also { if (it.size != 2) throw IllegalArgumentException("Must be user@instance.tld") }
            .let { Pair(it[0], it[1]) }

        dataSource.login(username, instance)
    }

    suspend fun create(username: String, password: String, totp: String?, instance: String) {
        dataSource.login(username, instance,
            Uri.Builder()
                .encodedAuthority("$username@$instance")
                .appendQueryParameter("password", password)
                .appendQueryParameter("totp", totp)
                .build()
                .toString()
                .let(::Credential)
        )
    }
}
