package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import info.`the-federation`.FediverseStats
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import ltd.ucode.lemmy.api.AccountDataSource
import ltd.ucode.lemmy.api.ApiResult
import ltd.ucode.lemmy.api.InstanceDataSource
import ltd.ucode.lemmy.data.type.NodeInfoResult
import ltd.ucode.lemmy.data.value.Addressable
import ltd.ucode.slide.table.Instance
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

        fun get(instance: String): InstanceDataSource {
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

        fun get(username: String, domain: String): AccountDataSource {
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

        fun login(username: String, password: String, instance: String): AccountDataSource {
            synchronized(store) {
                val instanceStore = store[instance]
                    ?: HashMap<Username?, InstanceDataSource>()
                        .also { store[instance] = it }
                return AccountDataSource(username, password, instance, okHttpClient)
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

    private fun get(key: Addressable): InstanceDataSource {
        return if (key.hasUsername) instances.get(key.username!!, key.domain)!!
        else instances.get(key.domain)!!
    }

    operator fun get(key: String?): InstanceDataSource {
        logger.trace { "Keys were ${
            instances.keys
                .map { "$it:${get(Addressable(it)).javaClass.simpleName}" }
        } for $key" }
        return get(Addressable(key ?: defaultInstance))
    }

    private fun deleteLogin(username: String, instance: String) {
        instances.remove(username, instance)
    }

    suspend fun fetchInstanceList(limit: Int? = null): List<Instance> {
        val nodeList = FediverseStats.getLemmyServers(userAgent, limit)
            ?.thefederation_node.orEmpty()

        return nodeList.map {
            val stat = it.thefederation_stats.firstOrNull()
            Instance(it.name, it.version, it.country, stat?.local_posts, stat?.local_comments,
                stat?.users_total, stat?.users_half_year, stat?.users_monthly, stat?.users_weekly)
        }
    }

    fun connect(account: String): AccountDataSource {
        val (username, instance) = account.split("@")
            .let { Pair(it[0], it[1]) }

        val password = accountRepository.getPassword(username, instance)!!

        return instances.login(username, password, instance)
    }

    private suspend fun login(username: String, password: String, instance: String): ApiResult<AccountDataSource> {
        val dataSource = instances.login(username, password, instance)
        return dataSource.getUnreadCount()
            .mapSuccess { dataSource }
            .onFailure {
                deleteLogin(username, instance)
                exception.rethrow()
            }
    }

    suspend fun create(username: String, password: String, instance: String) {
        accountRepository.setPassword(username, instance, password)
        login(username, password, instance).onFailure {
            accountRepository.deletePassword(username, instance)
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
