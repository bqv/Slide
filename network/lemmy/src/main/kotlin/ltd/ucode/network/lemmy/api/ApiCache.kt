package ltd.ucode.network.lemmy.api

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.reactivecircus.cache4k.Cache
import io.github.reactivecircus.cache4k.CacheEvent
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object ApiCache {
    private val logger: KLogger = KotlinLogging.logger {}

    abstract class CachedProperty<TRequest : Any, TResponse : Any, TCachedApi>(
        timeToLive: Duration? = 30.seconds,
        timeToIdle: Duration? = null,
        maxEntries: Long? = null,
    ) : ReadOnlyProperty<InstanceApi, TCachedApi> {
        val cache = Cache.Builder<TRequest, TResponse>()
            .apply { timeToIdle?.let(::expireAfterAccess) }
            .apply { timeToLive?.let(::expireAfterWrite) }
            .apply { maxEntries?.let(::maximumCacheSize) }
            .eventListener { event: CacheEvent<TRequest, TResponse> ->
                logger.trace { "$event" }
            }
            .build()
    }

    fun <TResponse : Any> cachedUnit(
        timeToLive: Duration? = 30.seconds,
        timeToIdle: Duration? = null,
        maxEntries: Long? = null,
        block: suspend () -> ApiResult<TResponse>,
    ) = cachedWithDefault(
        timeToLive = timeToLive,
        timeToIdle = timeToIdle,
        maxEntries = maxEntries,
        default = Unit,
        block = { block() },
    )

    fun <TRequest : Any, TResponse : Any> cachedWithDefault(
        default: TRequest,
        timeToLive: Duration? = 30.seconds,
        timeToIdle: Duration? = null,
        maxEntries: Long? = null,
        block: suspend (request: TRequest) -> ApiResult<TResponse>,
    ) = object : CachedProperty<TRequest, TResponse, CachedApiRequestWithDefault<TRequest, TResponse>>(
        timeToLive = timeToLive,
        timeToIdle = timeToIdle,
        maxEntries = maxEntries,
    ) {
        override fun getValue(thisRef: InstanceApi, property: KProperty<*>) =
            CachedApiRequestWithDefault(thisRef.instance, property.name, cache, default, block)
    }

    fun <TRequest : Any, TResponse : Any> cached(
        timeToLive: Duration? = 30.seconds,
        timeToIdle: Duration? = null,
        maxEntries: Long? = null,
        block: suspend (request: TRequest) -> ApiResult<TResponse>,
    ) = object : CachedProperty<TRequest, TResponse, CachedApiRequest<TRequest, TResponse>>(
        timeToLive = timeToLive,
        timeToIdle = timeToIdle,
        maxEntries = maxEntries,
    ) {
        override fun getValue(thisRef: InstanceApi, property: KProperty<*>)
            = CachedApiRequest(thisRef.instance, property.name, cache, block)
    }

    class CachedApiRequestWithDefault<TRequest : Any, TResponse : Any> internal constructor(
        instance: String,
        name: String,
        cache: Cache<TRequest, TResponse>,
        val default: TRequest,
        action: suspend (request: TRequest) -> ApiResult<TResponse>,
    ) : CachedApiRequest<TRequest, TResponse>(instance, name, cache, action) {
        suspend operator fun invoke(): ApiResult<TResponse> {
            return super.invoke(default)
        }

        override suspend operator fun invoke(request: TRequest): ApiResult<TResponse> {
            return super.invoke(request)
        }
    }

    open class CachedApiRequest<TRequest : Any, TResponse : Any> internal constructor(
        private val instance: String,
        private val name: String,
        val cache: Cache<TRequest, TResponse>,
        private val action: suspend (request: TRequest) -> ApiResult<TResponse>,
    ) {
        open suspend operator fun invoke(request: TRequest): ApiResult<TResponse> {
            return try {
                ApiResult.Success(instance, cache.get(request) {
                    action(request).also {
                        it.onFailure {
                            throw ApiCacheLoaderException(this)
                        }
                    }.success
                })
            } catch (e: ApiCacheLoaderException) {
                @Suppress("UNCHECKED_CAST")
                e.failure as ApiResult<TResponse>
            }
        }
    }

    private data class ApiCacheLoaderException(val failure: ApiResult<*>) : Exception() {}
}
