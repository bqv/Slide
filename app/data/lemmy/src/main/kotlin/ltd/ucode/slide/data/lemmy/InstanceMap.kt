package ltd.ucode.slide.data.lemmy

import android.net.Uri
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import ltd.ucode.network.lemmy.api.InstanceApi
import ltd.ucode.network.lemmy.data.type.jwt.Token
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

typealias InstanceMapCallback = (username: String, domain: String) -> Token

class InstanceMap(
    private val okHttpClient: OkHttpClient,
    private val userAgent: String,
    private val getToken: InstanceMapCallback,
) {
    private val logger: KLogger = KotlinLogging.logger {}

    private val store = ConcurrentHashMap<Domain, HashMap<Username?, InstanceApi>>()

    val count get() = store.map { it.value.count() }.sum()

    val flattened get() = store.flatMap {
        val domain = it.key
        it.value.map {
            when (val username = it.key) {
                null -> "$domain"
                else -> "$username@$domain"
            } to it.value
        }
    }

    val keys get() = store.flatMap {
        val domain = it.key
        it.value.map {
            when (val username = it.key) {
                null -> "$domain"
                else -> "$username@$domain"
            }
        }
    }

    suspend operator fun invoke(domain: String, username: String, password: String, totp: String? = null): InstanceApi {
        return Uri.Builder()
            .encodedAuthority("$username@$domain")
            .appendQueryParameter("password", password)
            .apply { totp?.let { appendQueryParameter("totp", it) } }
            .build()
            .let { invoke(it) }
    }

    suspend operator fun invoke(domain: String, username: String? = null, token: String? = null): InstanceApi {
        return Uri.Builder()
            .encodedAuthority("${username?.let { "$username@" }.orEmpty()}$domain")
            .apply { token?.let { appendQueryParameter("jwt", it) } }
            .build()
            .let { invoke(it) }
    }

    suspend operator fun invoke(uri: Uri): InstanceApi {
        logger.trace { "Keys were ${
            flattened.map { (key, value) -> "$key:${value.javaClass.simpleName}" }
        } for $uri" }

        val host = uri.host ?: throw IllegalArgumentException()
        val username = uri.userInfo ?: return getInstance(host)
        return uri.getQueryParameter("jwt")
            ?.let { jwt -> getOrCreateAccount(username, jwt, host) }
            ?: uri.getQueryParameter("password")
                ?.let { password ->
                    val totp = uri.getQueryParameter("totp")
                    getOrCreateAccount(username, password, totp, host)
                }
            ?: getAccount(username, host)
    }

    suspend fun getInstance(instance: String): InstanceApi {
        return synchronized(store) {
            assert(!instance.contains("@")) { "codepath err" }

            val instanceStore = store[instance]
                ?: HashMap<Username?, InstanceApi>()
                    .also { store[instance] = it }

            instanceStore[null]
                ?: instanceStore.values.firstOrNull() // fallback to any api on domain
                ?: InstanceApi(instance, okHttpClient)
                    .also { instanceStore[null] = it }
        }.verifyOr { remove(instance) }
    }

    suspend fun getAccount(username: String, domain: String): InstanceApi {
        return synchronized(store) {
            val key = "$username@$domain"

            val instanceStore = store[domain]
                ?: HashMap<Username?, InstanceApi>()
                    .also { store[domain] = it }

            instanceStore[username] as InstanceApi?
                ?: run {
                    val token = getToken(username, domain).token

                    InstanceApi(username, token, domain, okHttpClient)
                        .also { value -> instanceStore[username] = value }
                }
        }.verifyOr { remove(username, domain) }
    }

    suspend fun getOrCreateAccount(username: String, token: String, instance: String): InstanceApi {
        return synchronized(store) {
            val instanceStore = store[instance]
                ?: HashMap<Username?, InstanceApi>()
                    .also { store[instance] = it }

            InstanceApi(username, token, instance, okHttpClient)
                .also { value -> instanceStore[username] = value }
        }.verifyOr { remove(username, instance) }
    }

    suspend fun getOrCreateAccount(username: String, password: String, totp: String?, instance: String): InstanceApi {
        return synchronized(store) {
            val instanceStore = store[instance]
                ?: HashMap<Username?, InstanceApi>()
                    .also { store[instance] = it }

            InstanceApi(username, password, totp, instance, okHttpClient)
                .also { value -> instanceStore[username] = value }
        }.verifyOr { remove(username, instance) }
    }

    fun remove(instance: String): InstanceApi? {
        synchronized(store) {
            return store.remove(instance)
                ?.get(null)
        }
    }

    fun remove(username: String, instance: String): InstanceApi? {
        synchronized(store) {
            val key = "$username@$instance"
            val instanceStore = store[instance]
                ?: HashMap<Username?, InstanceApi>()
                    .also { store[instance] = it }
            return instanceStore.remove(key)?.also {
                if (instanceStore.isEmpty()) store.remove(instance)
            }
        }
    }

    private suspend fun <T : InstanceApi> T.verifyOr(block: (Exception) -> Unit): T {
        this.getNodeInfo().onFailure {
            block(exception.upcast())
        }

        if (this.isAuthenticated) {
            this.getUnreadCount().onFailure {
                block(exception.upcast())
            }
        } else {
            this.getSite().onFailure {
                block(exception.upcast())
            }
        }

        return this
    }
}
