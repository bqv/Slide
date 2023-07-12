package ltd.ucode

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import ltd.ucode.crash.GithubSender
import ltd.ucode.network.lemmy.api.AccountDataSource
import ltd.ucode.network.lemmy.api.InstanceDataSource
import ltd.ucode.network.lemmy.api.request.LoginRequest
import ltd.ucode.slide.repository.NetworkRepository
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

// Robolectric For SQL, activities flow, for those objects that needed context.
// JUnit4 for api's java module to make sure data are return correctly.
// Espresso For checking ui display correctly.

//@RunWith(AndroidJUnit4::class)
@RunWith(RobolectricTestRunner::class)
class Experiment {
    lateinit var context: Context

    @Before
    fun setUp() {
        this.context = ApplicationProvider.getApplicationContext()
    }

    @Test
    @ExperimentalSerializationApi
    fun testRealToken() {
        val account = try {
            val path = "/srv/code/slide/app/src/test/java/ltd/ucode/LoginRequest.json"
            val resource = File(path).inputStream()
            val request = Json.decodeFromStream<LoginRequest>(resource)
            with(request) { LoginRequest(usernameOrEmail, password) }
        } catch (e: Exception) {
            LoginRequest("", "")
        }
        val instance = "lemmy.ml"

        val source: InstanceDataSource = AccountDataSource(
            account.usernameOrEmail, account.password, account.totp2faToken,
            instance, OkHttpClient()
        )


        runBlocking { source.nodeInfo() }
            .also { println("$it") }
        runBlocking { source.getSite() }
            .also { println("${it.success.siteView}") }
        runBlocking { source.getUnreadCount() }
            .also { println("$it") }
    }

    @Test
    fun testFed() {
        val networkRepository = NetworkRepository(context, OkHttpClient(), "test")

        runBlocking { networkRepository.fetchInstanceList() }
            .also { println("${it.size}") }
    }

    @Test
    fun testCrash() {
        val sender = GithubSender(context)
    }
}
