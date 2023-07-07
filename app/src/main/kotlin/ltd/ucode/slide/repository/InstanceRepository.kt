package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import info.the_federation.FediverseStats
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import ltd.ucode.lemmy.api.AccountDataSource
import ltd.ucode.lemmy.api.ApiResult
import ltd.ucode.lemmy.api.InstanceDataSource
import ltd.ucode.lemmy.data.type.NodeInfoResult
import ltd.ucode.lemmy.data.type.jwt.Token
import ltd.ucode.lemmy.data.value.Addressable
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.entity.Site
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named

private typealias Domain = String
private typealias Username = String

class InstanceRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val okHttpClient: OkHttpClient,
    @Named("userAgent") val userAgent: String,
    val contentDatabase: ContentDatabase,
    val accountRepository: AccountRepository,
) {
    var defaultInstance: String = "lemmy.ml"

    private val logger: KLogger = KotlinLogging.logger {}

    init {
        logger.info { "Creating ${javaClass.simpleName}"}
    }

    private val instances = InstanceMap()

    inner class InstanceMap {
        private val store = ConcurrentHashMap<Domain, HashMap<Username?, InstanceDataSource>>()

        val count get() = store.map { it.value.count() }.sum()

        val keys get() = store.flatMap {
            val domain = it.key
            it.value.map {
                when (val username = it.key) {
                    null -> "$domain"
                    else -> "$username@$domain"
                }
            }
        }

        fun getInstance(instance: String): InstanceDataSource {
            synchronized(store) {
                if (instance.contains("@")) throw IllegalArgumentException("codepath error")
                val instanceStore = store[instance]
                    ?: HashMap<Username?, InstanceDataSource>()
                        .also { store[instance] = it }
                return instanceStore[null] ?: instanceStore.values.firstOrNull()
                    ?: InstanceDataSource(instance, okHttpClient)
                        .also { instanceStore[null] = it }
            }
        }

        fun getAccount(username: String, domain: String): AccountDataSource {
            synchronized(store) {
                val key = "$username@$domain"
                val instanceStore = store[domain]
                    ?: HashMap<Username?, InstanceDataSource>()
                        .also { store[domain] = it }
                return instanceStore[username] as AccountDataSource?
                    ?: this@InstanceRepository.connect(key)
                        .also { instanceStore[username] = it }
            }
        }

        fun getOrCreateAccount(username: String, token: String, instance: String): AccountDataSource {
            synchronized(store) {
                val instanceStore = store[instance]
                    ?: HashMap<Username?, InstanceDataSource>()
                        .also { store[instance] = it }
                return AccountDataSource(username, token, instance, okHttpClient)
                    .also { value -> instanceStore[username] = value }
            }
        }

        fun getOrCreateAccount(username: String, password: String, totp: String?, instance: String): AccountDataSource {
            synchronized(store) {
                val instanceStore = store[instance]
                    ?: HashMap<Username?, InstanceDataSource>()
                        .also { store[instance] = it }
                return AccountDataSource(username, password, totp, instance, okHttpClient)
                    .also { value -> instanceStore[username] = value }
            }
        }

        fun remove(username: String, instance: String): InstanceDataSource? {
            synchronized(store) {
                val key = "$username@$instance"
                val instanceStore = store[instance]
                    ?: HashMap<Username?, InstanceDataSource>()
                        .also { store[instance] = it }
                return instanceStore.remove(key)?.also {
                    if (instanceStore.isEmpty()) store.remove(instance)
                }
            }
        }
    }

    private fun getAccountOrInstance(key: Addressable): InstanceDataSource {
        return if (key.hasUsername) instances.getAccount(key.username!!, key.domain)!!
        else instances.getInstance(key.domain)!!
    }

    operator fun get(key: String?): InstanceDataSource {
        logger.trace { "Keys were ${
            instances.keys
                .map { "$it:${getAccountOrInstance(Addressable(it)).javaClass.simpleName}" }
        } for $key" }
        return getAccountOrInstance(Addressable(key ?: defaultInstance))
    }

    private fun deleteLogin(username: String, instance: String) {
        instances.remove(username, instance)
    }

    suspend fun fetchInstanceList(limit: Int? = null): List<Site> {
        val nodeList = FediverseStats.getLemmyServers(userAgent, limit)
            ?.thefederation_node.orEmpty()

        return nodeList.map(Site::from)
    }

    fun connect(account: String): AccountDataSource {
        val (username, instance) = account.split("@")
            .also { if (it.size != 2) throw IllegalArgumentException("Must be user@instance.tld") }
            .let { Pair(it[0], it[1]) }

        val token = accountRepository.getToken(username, instance)!!

        return instances.getOrCreateAccount(username, token, instance)
    }

    private suspend fun login(username: String, password: String, totp: String?, instance: String): ApiResult<Token> {
        val dataSource = instances.getOrCreateAccount(username, password, totp, instance)
        return dataSource.getUnreadCount()
            .mapSuccess { dataSource.token() }
            .onFailure {
                deleteLogin(username, instance)
                exception.rethrow()
            }
    }

    suspend fun create(username: String, password: String, totp: String?, instance: String) {
        login(username, password, totp, instance)
            .onSuccess { token -> accountRepository.setToken(username, instance, token) }
            .onFailure {
                accountRepository.deleteToken(username, instance)
                exception.rethrow()
            }
    }

    suspend fun getNodeInfo(instance: String): ApiResult<NodeInfoResult> {
        return with(this[instance]) {
            getNodeInfo()
        }
    }

    suspend fun getFederatedInstances(fromInstance: String = "lemmy.ml"): ApiResult<Set<String>> {
        return this[fromInstance].getFederatedInstances().mapSuccess {
            data.federatedInstances?.run {
                linked.toSortedSet().also {
                    it.addAll(allowed.orEmpty())
                    it.addAll(blocked.orEmpty())
                }
            }.orEmpty()
        }
    }
}
