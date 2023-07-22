package ltd.ucode.slide.data.source

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ltd.ucode.slide.data.auth.Credential
import ltd.ucode.slide.data.auth.CredentialDatabase
import ltd.ucode.slide.data.common.Constants.DEFAULT_PAGE_SIZE
import ltd.ucode.slide.data.common.entity.Post
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.slide.data.common.source.INetworkDataSource
import ltd.ucode.slide.data.content.ContentDatabase
import ltd.ucode.slide.data.lemmy.LemmyDataSource
import ltd.ucode.slide.data.value.Feed
import ltd.ucode.slide.data.value.Period
import ltd.ucode.slide.data.value.Sorting
import okhttp3.OkHttpClient
import java.util.SortedMap

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkDataSource(
    private val okHttpClient: OkHttpClient,
    private val userAgent: String,
    private val contentDatabase: ContentDatabase,
    private val credentialDatabase: CredentialDatabase,
) : IDataSource {
    private val sources: SortedMap<String, out INetworkDataSource> by lazy {
        sortedMapOf(
            LemmyDataSource.name to LemmyDataSource.create(okHttpClient, userAgent, contentDatabase, credentialDatabase)
        )
    }

    private fun domainSource(domain: String): Flow<INetworkDataSource> = flow {
        coroutineScope {
            sources.forEach {
                launch {
                    it.value.updateSite(domain)
                }
            }
        }

        contentDatabase.sites.flow(domain)
            .mapNotNull { it.software }
            .distinctUntilChanged()
            .mapNotNull { sources[it] }
            .let { emitAll(it) }
    }

    override suspend fun login(username: String, domain: String) {
        domainSource(domain).first().login(username, domain)
    }

    override suspend fun login(username: String, domain: String, credential: Credential) {
        domainSource(domain).first().login(username, domain, credential)
    }

    override fun getSite(rowId: Long): Flow<Site> {
        return contentDatabase.sites.flow(rowId).distinctUntilChanged()
            .onEach { site ->
                val source = site.software?.let { sources[it] }
                source?.updateSite(site.name)
            }
    }

    override fun getSite(domain: String): Flow<Site> {
        return domainSource(domain)
            .flatMapLatest { source ->
                coroutineScope {
                    launch {
                        source.updateSite(domain)
                    }
                }

                contentDatabase.sites.flow(name = domain).distinctUntilChanged()
                    .onEach { site ->
                        val source = site.software?.let { sources[it] }
                        source?.updateSite(site.name)
                    }
            }
    }

    override fun getSites(): Flow<List<Site>> = flow {
        coroutineScope {
            sources.forEach {
                launch {
                    it.value.updateSites()
                }
            }
        }

        contentDatabase.sites.flowAll().distinctUntilChanged()
            .let { emitAll(it) }
    }

    override fun getSites(software: String): Flow<List<Site>> = flow {
        coroutineScope {
            val source = sources[software]
            launch {
                source?.updateSites()
            }
        }

        contentDatabase.sites.flowAllBySoftware(software).distinctUntilChanged()
            .let { emitAll(it) }
    }

    override fun getPost(rowId: Long): Flow<Post> {
        return contentDatabase.posts.flow(rowId).distinctUntilChanged()
            .onEach { post ->
                val source = post.site.software?.let { sources[it] }
                source?.updatePost(post.site.name, post.postId)
            }
    }

    override fun getPost(domain: String, key: Int): Flow<Post> {
        return domainSource(domain)
            .flatMapLatest { source ->
                source.updatePost(domain, key)

                contentDatabase.posts.flow(postId = key, siteName = domain).distinctUntilChanged()
                    .onEach { post ->
                        post.site.software?.let {
                            assert(source == sources[it])
                        }
                        coroutineScope {
                            launch {
                                source.updatePost(post.site.name, post.postId)
                            }
                        }
                    }
            }
    }

    override fun getPosts(domain: String, feed: Feed, period: Period, order: Sorting): Flow<PagingData<Post>> {
        val pager = Pager(config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE),
            // TODO: remoteMediator = ??
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

        return domainSource(domain)
            .flatMapLatest { source ->
                coroutineScope {
                    launch {
                        source.updatePosts(domain, feed, period, order)
                    }
                }

                pager.flow.distinctUntilChanged()
            }
    }

    override fun getPosts(domain: String, feed: Feed, pageSize: Int, period: Period, order: Sorting): Flow<List<Post>> {
        val dbFlow = when (order) {
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

        return domainSource(domain)
            .flatMapLatest { source ->
                coroutineScope {
                    launch {
                        source.updatePosts(domain, feed, period, order)
                    }
                }

                dbFlow.distinctUntilChanged()
            }
    }
}
