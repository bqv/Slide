package ltd.ucode.slide

class PagedData<out T>(private var page: Int = 1, // one-based
                       private val fetcher: (page: Int) -> suspend () -> List<T>)
{
    var hasNext = true
        private set

    suspend fun next(): List<T> {
        val page = fetcher(page++)()

        if (page.isEmpty()) hasNext = false

        return page
    }
}
