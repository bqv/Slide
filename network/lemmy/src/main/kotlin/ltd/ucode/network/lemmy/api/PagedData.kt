package ltd.ucode.network.lemmy.api

class PagedData<out T>(private var page: Int = 1, // one-based
                       private val fetcher: (page: Int) -> suspend () -> ApiResult<List<T>>)
{
    var hasNext = true
        private set

    suspend fun next(): ApiResult<List<@UnsafeVariance T>> {
        return fetcher(page++)()
            .mapSuccess {
                if (data.isEmpty()) hasNext = false
                data
            }
    }
}
