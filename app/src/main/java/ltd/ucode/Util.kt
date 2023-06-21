package ltd.ucode

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Util {

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

}
