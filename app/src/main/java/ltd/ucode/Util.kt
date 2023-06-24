package ltd.ucode

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import ltd.ucode.slide.BuildConfig
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object Util {

    @OptIn(ExperimentalSerializationApi::class)
    val SnakeCaseSerializer = Json {
        encodeDefaults = true
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> {
        return mapNotNull { it.value?.let { value -> it.key to value } }
            .toMap()
    }

    fun <R> CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit,
        doInBackground: () -> R,
        onPostExecute: (R) -> Unit,
    ) = launch {
        onPreExecute()
        val result = withContext(Dispatchers.IO) {
            // runs in background thread without blocking the Main Thread
            doInBackground()
        }
        onPostExecute(result)
    }

    val Instant.ago: Duration
        get() = DurationUnit.SECONDS.let { unit ->
            minus(Clock.System.now()).absoluteValue.toLong(unit).toDuration(unit)
        }

    val userAgent: String
        get() = "android:" + BuildConfig.APPLICATION_ID +  ":v" + BuildConfig.VERSION_NAME
}
