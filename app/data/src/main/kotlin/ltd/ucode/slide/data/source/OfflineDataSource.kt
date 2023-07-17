package ltd.ucode.slide.data.source

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import ltd.ucode.slide.data.Constants.DEFAULT_PAGE_SIZE
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.auth.Credential
import ltd.ucode.slide.data.entity.Post
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.value.Feed
import ltd.ucode.slide.data.value.Period
import ltd.ucode.slide.data.value.Sorting

class OfflineDataSource(private val contentDatabase: ContentDatabase) : IDataSource {
    override suspend fun login(username: String, domain: String)
        = throw UnsupportedOperationException("offline")

    override suspend fun login(username: String, domain: String, credential: Credential)
        = throw UnsupportedOperationException("offline")

    override fun getSite(rowId: Int): Flow<Site> {
        return contentDatabase.sites.flow(rowId).distinctUntilChanged()
    }

    override fun getSite(domain: String): Flow<Site> {
        return contentDatabase.sites.flow(name = domain).distinctUntilChanged()
    }

    override fun getSites(): Flow<List<Site>> {
        return contentDatabase.sites.flowAll().distinctUntilChanged()
    }

    override fun getSites(software: String): Flow<List<Site>> {
        return contentDatabase.sites.flowAllBySoftware(software).distinctUntilChanged()
    }

    override fun getPost(rowId: Int): Flow<Post> {
        return contentDatabase.posts.flow(rowId).distinctUntilChanged()
    }

    override fun getPost(domain: String, key: Int): Flow<Post> {
        return contentDatabase.posts.flow(postId = key, siteName = domain).distinctUntilChanged()
    }

    override fun getPosts(domain: String, feed: Feed, period: Period, order: Sorting): Flow<PagingData<Post>> {
        val pager = Pager(config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE),
        ) {
            when (order) {
                is Sorting.New -> if (order.comments)
                    contentDatabase.posts.pagingSourceNewComments(siteName = domain, before = period.before, after = period.after)
                else
                    contentDatabase.posts.pagingSourceNew(siteName = domain, before = period.before, after = period.after)
                is Sorting.Old -> if (order.controversial)
                    contentDatabase.posts.pagingSourceControversial(siteName = domain, before = period.before, after = period.after)
                else
                    contentDatabase.posts.pagingSourceOld(siteName = domain, before = period.before, after = period.after)
                is Sorting.Top -> if (order.comments)
                    contentDatabase.posts.pagingSourceMostComments(siteName = domain, before = period.before, after = period.after)
                else
                    contentDatabase.posts.pagingSourceTop(siteName = domain, before = period.before, after = period.after)
                is Sorting.Hot -> if (order.active)
                    contentDatabase.posts.pagingSourceActive(siteName = domain, before = period.before, after = period.after)
                else
                    contentDatabase.posts.pagingSourceHot(siteName = domain, before = period.before, after = period.after)
            }
        }

        return pager.flow.distinctUntilChanged()
    }

    override fun getPosts(domain: String, feed: Feed, pageSize: Int, period: Period, order: Sorting): Flow<List<Post>> {
        return when (order) {
            is Sorting.New -> if (order.comments)
                contentDatabase.posts.flowNewComments(limit = pageSize, siteName = domain, before = period.before, after = period.after)
            else
                contentDatabase.posts.flowNew(limit = pageSize, siteName = domain, before = period.before, after = period.after)

            is Sorting.Old -> if (order.controversial)
                contentDatabase.posts.flowControversial(limit = pageSize, siteName = domain, before = period.before, after = period.after)
            else
                contentDatabase.posts.flowOld(limit = pageSize, siteName = domain, before = period.before, after = period.after)

            is Sorting.Top -> if (order.comments)
                contentDatabase.posts.flowMostComments(limit = pageSize, siteName = domain, before = period.before, after = period.after)
            else
                contentDatabase.posts.flowTop(limit = pageSize, siteName = domain, before = period.before, after = period.after)

            is Sorting.Hot -> if (order.active)
                contentDatabase.posts.flowActive(limit = pageSize, siteName = domain, before = period.before, after = period.after)
            else
                contentDatabase.posts.flowHot(limit = pageSize, siteName = domain, before = period.before, after = period.after)
        }
    }
}
