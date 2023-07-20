package ltd.ucode.slide.data.common.source

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import ltd.ucode.slide.data.auth.Credential
import ltd.ucode.slide.data.common.auth.ICredentialDatabase
import ltd.ucode.slide.data.common.content.IContentDatabase
import ltd.ucode.slide.data.value.Feed
import ltd.ucode.slide.data.value.Period
import ltd.ucode.slide.data.value.Sorting
import okhttp3.OkHttpClient
import java.util.SortedMap

typealias SourcesMap = SortedMap<String, out INetworkDataSource>

abstract class INetworkDataSource(
    val name: String,
    private val credentialDatabase: ICredentialDatabase,
) {
    private val logger: KLogger = KotlinLogging.logger {}

    init {
        logger.debug { "Initializing networkDataSource: $name" }
    }

    interface INetworkDataSourceFactory {
        fun create(
            okHttpClient: OkHttpClient,
            userAgent: String,
            contentDatabase: IContentDatabase,
            credentialDatabase: ICredentialDatabase,
        ): INetworkDataSource
    }

    protected fun getCredential(username: String, domain: String): Credential {
        return credentialDatabase.get(username, domain)
    }

    protected suspend fun withCredential(username: String, domain: String, block: suspend (Credential) -> Credential) {
        var credential = credentialDatabase.get(username, domain)
        credential = block(credential)
        credentialDatabase.set(username, domain, credential)
    }

    suspend fun login(username: String, domain: String) {
        withCredential(username, domain) {
            this.login(username, domain, it)
        }
    }

    abstract suspend fun login(username: String, domain: String, credential: Credential): Credential
    abstract suspend fun updateSite(domain: String)
    abstract suspend fun updateSites(domain: String)
    abstract suspend fun updateSites()
    abstract suspend fun updatePost(domain: String, key: Int)
    abstract suspend fun updatePosts(domain: String, feed: Feed, period: Period, order: Sorting)

    @OptIn(DelicateCoroutinesApi::class)
    companion object {
        private val sources_: MutableList<INetworkDataSourceFactory> = mutableListOf()

        internal fun sources(
            okHttpClient: OkHttpClient,
            userAgent: String,
            contentDatabase: IContentDatabase,
            credentialDatabase: ICredentialDatabase,
        ): SourcesMap {
            return sources_
                .map { it.create(okHttpClient, userAgent, contentDatabase, credentialDatabase) }
                .associateBy(INetworkDataSource::name)
                .toSortedMap()
        }

        @JvmStatic
        protected operator fun plusAssign(value: INetworkDataSourceFactory) {
            sources_.add(value)
        }

        suspend fun SourcesMap.each(
            scope: CoroutineScope = GlobalScope,
            block: suspend INetworkDataSource.() -> Unit,
        ): Map<String, Exception?> {
            return mapValuesAsync(scope) {
                try {
                    block(it)
                    null
                } catch (e: Exception) {
                    e
                }
            }
        }

        private suspend fun <K, V, R> Map<K, V>.mapValuesAsync(
            scope: CoroutineScope = GlobalScope,
            block: suspend (V) -> R,
        ) : Map<K, R> {
            return map {
                scope.async(start = CoroutineStart.LAZY) {
                    Pair(it.key, block(it.value))
                }
            }.awaitAll().toMap()
        }
    }
}
