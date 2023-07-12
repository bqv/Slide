package info.the_federation

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import info.the_federation.graphql.generated.GetLemmyServersQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import java.net.URL
import java.util.concurrent.TimeUnit

object FediverseStats {
    private val httpClient: HttpClient by lazy {
        HttpClient(engineFactory = OkHttp) {
            engine {
                config {
                    connectTimeout(10, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(60, TimeUnit.SECONDS)
                }
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }
        }
    }

    suspend fun getLemmyServers(userAgent: String, limit: Int? = null): GetLemmyServersQuery.Result? {
        val client = GraphQLKtorClient(
            url = URL("https://the-federation.info/v1/graphql"),
            httpClient = httpClient
        )
        val getLemmyServersQuery = GetLemmyServersQuery(GetLemmyServersQuery.Variables(
            plat = "lemmy",
            limit = limit
        ))
        val result = client.execute(getLemmyServersQuery) {
            header("User-Agent", userAgent)
        }

        return result.data
    }
}
