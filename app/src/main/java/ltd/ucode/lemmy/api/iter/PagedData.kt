package ltd.ucode.lemmy.api.iter

import kotlinx.coroutines.runBlocking

class PagedData<out T>(private var page: Int = 1, // one-based
                       private val fetcher: (page: Int) -> suspend () -> List<T>)
    : Iterable<List<T>> {
    var hasNext = true
        private set

    suspend fun next(): List<T> {
        val page = fetcher(page++)()

        if (page.isEmpty()) hasNext = false

        return page
    }

    override fun iterator(): Iterator<List<T>> {
        return object : Iterator<List<T>> {
            override fun hasNext(): Boolean {
                return this@PagedData.hasNext
            }

            override fun next(): List<T> {
                return runBlocking { this@PagedData.next() }
            }

        }
    }
}
