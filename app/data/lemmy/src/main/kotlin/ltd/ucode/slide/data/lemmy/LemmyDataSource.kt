package ltd.ucode.slide.data.lemmy

import android.net.Uri
import info.the_federation.FediverseStats
import ltd.ucode.network.lemmy.api.request.GetFederatedInstancesRequest
import ltd.ucode.network.lemmy.api.request.GetPostRequest
import ltd.ucode.network.lemmy.api.request.GetPostsRequest
import ltd.ucode.network.lemmy.api.request.GetSiteRequest
import ltd.ucode.network.lemmy.api.response.GetFederatedInstancesResponse
import ltd.ucode.network.lemmy.api.response.GetPostResponse
import ltd.ucode.network.lemmy.api.response.GetPostsResponse
import ltd.ucode.network.lemmy.api.response.GetSiteResponse
import ltd.ucode.network.lemmy.data.id.PostId
import ltd.ucode.network.lemmy.data.type.PostListingType
import ltd.ucode.network.lemmy.data.type.PostSortType
import ltd.ucode.network.lemmy.data.type.jwt.Token
import ltd.ucode.slide.data.auth.Credential
import ltd.ucode.slide.data.common.auth.ICredentialDatabase
import ltd.ucode.slide.data.common.content.IContentDatabase
import ltd.ucode.slide.data.common.source.INetworkDataSource
import ltd.ucode.slide.data.lemmy.post.GetPostsResponseMarshaller.toPost
import ltd.ucode.slide.data.lemmy.post.PostListingTypeMarshaller.from
import ltd.ucode.slide.data.lemmy.post.PostSortTypeMarshaller.from
import ltd.ucode.slide.data.lemmy.site.GetFederatedInstancesResponseExtensions.toSites
import ltd.ucode.slide.data.lemmy.site.GetSiteResponseExtensions.toSite
import ltd.ucode.slide.data.lemmy.site.GetSiteResponseExtensions.toSiteImage
import ltd.ucode.slide.data.lemmy.site.TheFederationNodeExtensions.toSiteMetadataPartial
import ltd.ucode.slide.data.value.Feed
import ltd.ucode.slide.data.value.Period
import ltd.ucode.slide.data.value.Sorting
import ltd.ucode.util.ksp.locator.Locator
import okhttp3.OkHttpClient

class LemmyDataSource(
    private val okHttpClient: OkHttpClient,
    private val userAgent: String,
    private val contentDatabase: IContentDatabase,
    credentialDatabase: ICredentialDatabase,
) : INetworkDataSource(name, credentialDatabase) {
    @Locator(INetworkDataSourceFactory::class)
    companion object : INetworkDataSourceFactory {
        const val name = "lemmy"

        override fun create(
            okHttpClient: OkHttpClient,
            userAgent: String,
            contentDatabase: IContentDatabase,
            credentialDatabase: ICredentialDatabase,
        ): INetworkDataSource {
            return LemmyDataSource(okHttpClient, userAgent, contentDatabase, credentialDatabase)
        }
    }

    private val getApi: InstanceMap = InstanceMap(okHttpClient, userAgent) { username, domain ->
        val credential = getCredential(username, domain)
        try {
            val uri = Uri.parse(credential.string)
            assert(uri.host == domain) { "${uri.host} != $domain " }
            assert(uri.userInfo == username) { "${uri.userInfo} != $username " }
            val jwt = uri.getQueryParameter("jwt")
                ?: throw IllegalArgumentException(
                    "Names were: {${uri.queryParameterNames.joinToString(", ")}}")

            Token(jwt)
        } catch (e: Exception) {
            credentialDatabase.delete(username, domain)
            throw e
        }
    }

    override suspend fun login(username: String, domain: String, credential: Credential): Credential {
        val uri = Uri.parse(credential.string)

        assert(uri.host == domain) { "${uri.host} != $domain " }
        assert(uri.userInfo == username) { "${uri.userInfo} != $username "}
        val password = uri.getQueryParameter("password")
        val totp = uri.getQueryParameter("totp")
        val jwt = uri.getQueryParameter("jwt")

        val token = when {
            jwt != null -> login(username, domain, Token(jwt))
            password != null -> login(username, domain, password, totp)
            else -> throw IllegalArgumentException(
                "Names were: {${uri.queryParameterNames.joinToString(", ")}}")
        }

        return uri.buildUpon()
            .clearQuery()
            .appendQueryParameter("jwt", token.token)
            .build()
            .toString()
            .let(::Credential)
    }

    private suspend fun login(username: String, domain: String, password: String, totp: String?): Token {
        val api = getApi.getOrCreateAccount(username, password, totp, domain)
        return api.token
    }

    private suspend fun login(username: String, domain: String, token: Token): Token {
        val api = getApi.getOrCreateAccount(username, token.token, domain)
        return api.token
    }

    override suspend fun updateSite(domain: String) {
        with (getApi(domain)) {
            getNodeInfo().bindSuccess {
                val nodeInfo = data.nodeInfo
                require(nodeInfo.software.name == name) { "expected $name, found ${nodeInfo.software.name}" }

                getSite(GetSiteRequest())
                    .onSuccess { data: GetSiteResponse ->
                        val site = data.toSite(nodeInfo).run {
                            copy(rowId = contentDatabase.sites.upsert(this))
                        }
                        val siteImage = data.toSiteImage(site).run {
                            copy(rowId = contentDatabase.sites.upsert(this))
                        }
                        data.myUser // TODO: handle user
                        data.admins // TODO: handle users
                        contentDatabase.languages.ensureAll(emptyList()) //data.allLanguages // TODO: handle languages
                        data.taglines // TODO: handle taglines
                        data.federatedInstances // TODO: handle sites
                    }
            }
        }
    }

    override suspend fun updateSite(domain: String, siteId: Int) {
        with (getApi(domain)) {
            getNodeInfo().bindSuccess {
                val nodeInfo = data.nodeInfo
                require(nodeInfo.software.name == name) { "expected $name, found ${nodeInfo.software.name}" }

                getSite(GetSiteRequest())
                    .onSuccess { data: GetSiteResponse ->
                        val site = data.toSite(nodeInfo).run {
                            copy(rowId = contentDatabase.sites.upsert(this))
                        }
                        val siteImage = data.toSiteImage(site).run {
                            copy(rowId = contentDatabase.sites.upsert(this))
                        }
                        data.myUser // TODO: handle user
                        data.admins // TODO: handle users
                        contentDatabase.languages.ensureAll(emptyList()) //data.allLanguages // TODO: handle languages
                        data.taglines // TODO: handle taglines
                        data.federatedInstances // TODO: handle sites
                    }
            }
        }
    }

    override suspend fun updateSites(domain: String) {
        getApi(domain).getFederatedInstances(GetFederatedInstancesRequest())
            .onSuccess { data: GetFederatedInstancesResponse ->
                val sites = data.toSites(this.name).let {
                    contentDatabase.sites.ensureAll(it)
                }
            }
    }

    override suspend fun updateSites() {
        val limit: Int? = null
        val nodeList = FediverseStats.getLemmyServers(userAgent, limit)
            ?.thefederation_node

        nodeList?.forEach { node ->
            val site = node.toSiteMetadataPartial().run {
                copy(rowId = contentDatabase.sites.upsert(this))
            }
        }
    }

    override suspend fun updatePost(domain: String, postId: Int) {
        getApi(domain).getPost(GetPostRequest(
            id = PostId(postId),
        ))
            .onSuccess { data: GetPostResponse ->
                val site = contentDatabase.sites.get(data.postView.community.instanceId.id, domain)!!
                val group = contentDatabase.groups.get(data.postView.community.id.id, domain)!!
                val user = contentDatabase.users.get(data.postView.creator.id.id, domain)!!
                val language = contentDatabase.languages.get(data.postView.post.languageId.id, domain)!!

                val post = data.postView.toPost(site, group, user, language).run {
                    copy(rowId = contentDatabase.posts.upsert(this))
                }
                // TODO: also, a PostVote
            }
    }

    override suspend fun updatePosts(domain: String, feed: Feed, period: Period, sorting: Sorting) {
        getApi(domain).getPosts(GetPostsRequest(
            sort = PostSortType.from(sorting, period),
            type = PostListingType.from(feed),
        ))
            .onSuccess { data: GetPostsResponse ->
                data.posts.forEach {
                    val site = contentDatabase.sites.get(it.community.instanceId.id, domain)!! // no! update.
                    val group = contentDatabase.groups.get(it.community.id.id, domain)!!
                    val user = contentDatabase.users.get(it.creator.id.id, domain)!!
                    val language = contentDatabase.languages.get(it.post.languageId.id, domain)!!

                    val post = it.toPost(site, group, user, language).run {
                        copy(rowId = contentDatabase.posts.upsert(this))
                    }
                    // TODO: also, a PostVote
                }
            }
    }
}

