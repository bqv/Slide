package ltd.ucode.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object CoroutineScopeExtensions {
    fun <R> CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit,
        doInBackground: suspend () -> R,
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
