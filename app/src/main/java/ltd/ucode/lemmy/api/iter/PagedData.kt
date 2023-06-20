package ltd.ucode.lemmy.api.iter

class PagedData<out T>(private var page: Int = 0,
                       private val fetcher: (page: Int) -> suspend () -> List<T>) {
    var hasNext = true
        private set

    suspend fun next(): List<T> {
        val page = fetcher(page++)()

        if (page.isEmpty()) hasNext = false

        return page
    }
}
