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
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.auth.Credential
import ltd.ucode.slide.data.auth.CredentialDatabase
import ltd.ucode.slide.data.lemmy.post.GetPostResponseMarshaller.toPost
import ltd.ucode.slide.data.lemmy.post.GetPostsResponseMarshaller.toPosts
import ltd.ucode.slide.data.lemmy.post.PostListingTypeMarshaller.from
import ltd.ucode.slide.data.lemmy.post.PostSortTypeMarshaller.from
import ltd.ucode.slide.data.lemmy.site.GetFederatedInstancesResponseExtensions.toSites
import ltd.ucode.slide.data.lemmy.site.GetSiteResponseExtensions.toSite
import ltd.ucode.slide.data.lemmy.site.TheFederationNodeExtensions.toSiteMetadataPartial
import ltd.ucode.slide.data.source.INetworkDataSource
import ltd.ucode.slide.data.value.Feed
import ltd.ucode.slide.data.value.Period
import ltd.ucode.slide.data.value.Sorting
import okhttp3.OkHttpClient

class LemmyDataSource(
    private val okHttpClient: OkHttpClient,
    private val userAgent: String,
    private val contentDatabase: ContentDatabase,
    credentialDatabase: CredentialDatabase,
) : INetworkDataSource("lemmy", credentialDatabase) {
    companion object {
        init {
            INetworkDataSource += object : INetworkDataSourceFactory {
                override fun create(
                    okHttpClient: OkHttpClient,
                    userAgent: String,
                    contentDatabase: ContentDatabase,
                    credentialDatabase: CredentialDatabase,
                ): INetworkDataSource {
                    return LemmyDataSource(okHttpClient, userAgent, contentDatabase, credentialDatabase)
                }
            }
        }
    }

    private val getApi: InstanceMap = InstanceMap(okHttpClient, userAgent) { username, domain ->
        val credential = getCredential(username, domain)
        val uri = Uri.parse(credential.string)

        assert(uri.host == domain) { "${uri.host} != $domain " }
        assert(uri.userInfo == username) { "${uri.userInfo} != $username "}
        val jwt = uri.getQueryParameter("jwt")
            ?: throw IllegalArgumentException(
                "Names were: {${uri.queryParameterNames.joinToString(", ")}}")

        Token(jwt)
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
        return api.token()
    }

    private suspend fun login(username: String, domain: String, token: Token): Token {
        val api = getApi.getOrCreateAccount(username, token.token, domain)
        return api.token()
    }

    override suspend fun updateSite(domain: String) {
        getApi(domain).getSite(GetSiteRequest())
            .onSuccess { data: GetSiteResponse ->
                contentDatabase.sites.upsert(data.toSite())
                data.myUser // TODO: handle user
                data.admins // TODO: handle users
                contentDatabase.languages.ensureAll(emptyList()) //data.allLanguages // TODO: handle languages
                data.taglines // TODO: handle taglines
                data.federatedInstances // TODO: handle sites
            }
    }

    override suspend fun updateSites(domain: String) {
        getApi(domain).getFederatedInstances(GetFederatedInstancesRequest())
            .onSuccess { data: GetFederatedInstancesResponse ->
                data.toSites(this.name).let {
                    contentDatabase.sites.ensureAll(it)
                }
            }
    }

    override suspend fun updateSites() {
        val limit: Int? = null
        val nodeList = FediverseStats.getLemmyServers(userAgent, limit)
            ?.thefederation_node.orEmpty()

        nodeList.forEach { node ->
            contentDatabase.sites.upsert(node.toSiteMetadataPartial())
        }
    }

    override suspend fun updatePost(domain: String, postId: Int) {
        getApi(domain).getPost(GetPostRequest(
            id = PostId(postId),
        ))
            .onSuccess { data: GetPostResponse ->
                contentDatabase.posts.upsert(data.toPost(contentDatabase, domain))
                // TODO: also, a PostVote
            }
    }

    override suspend fun updatePosts(domain: String, feed: Feed, period: Period, sorting: Sorting) {
        getApi(domain).getPosts(GetPostsRequest(
            sort = PostSortType.from(sorting, period),
            type = PostListingType.from(feed),
        ))
            .onSuccess { data: GetPostsResponse ->
                data.toPosts(contentDatabase, domain).forEach {
                    contentDatabase.posts.upsert(it)
                    // TODO: also, a PostVote
                }
            }
    }
}

