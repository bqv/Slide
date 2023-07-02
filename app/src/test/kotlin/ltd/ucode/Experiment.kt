package ltd.ucode

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import io.matthewnelson.fake_keystore.FakeAndroidKeyStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import ltd.ucode.util.Util.ago
import ltd.ucode.lemmy.api.AccountDataSource
import ltd.ucode.lemmy.api.InstanceDataSource
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.lemmy.data.type.jwt.Token
import ltd.ucode.slide.data.repository.AccountRepository
import ltd.ucode.slide.data.repository.InstanceRepository
import okhttp3.OkHttpClient
import org.acra.config.MailSenderConfiguration
import org.acra.sender.EmailIntentSender
import org.acra.sender.HttpSender
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.Test

// Robolectric For SQL, activities flow, for those objects that needed context.
// JUnit4 for api's java module to make sure data are return correctly.
// Espresso For checking ui display correctly.

//@RunWith(AndroidJUnit4::class)
@RunWith(RobolectricTestRunner::class)
class Experiment {
    lateinit var context: Context
    lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setUp() {
        this.sharedPreferences = SPMockBuilder().createSharedPreferences()
        this.context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.getSharedPreferences("accounts", 0))
            .thenReturn(sharedPreferences)
        this.context = ApplicationProvider.getApplicationContext()
        FakeAndroidKeyStore.setup
    }

    @Test
    fun testToken() {
        val token = Token(Token.EXAMPLE)
        println("Token: ${token.token}")
        println("Type: ${token.type}")
        println("Algorithm: ${token.algorithm}")
        for ((key, value) in token.claims) {
            println("Claim['$key']: $value")
        }
        println("Issued: ${token.issuedAtTime} (${token.issuedAtTime.ago} ago)")
        println("Signature: ${token.signature}")
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
        val accounts = AccountRepository(context)
        val instance = "lemmy.ml"
        accounts.setPassword(account.usernameOrEmail, instance, account.password)


        val source: InstanceDataSource = AccountDataSource(
            context, accounts,
            account.usernameOrEmail,"lemmy.ml", OkHttpClient()
        )


        runBlocking { source.nodeInfo() }
            .also { println("$it") }
        runBlocking { source.getSite() }
            .also { println("${it.siteView}") }
        runBlocking { source.getUnreadCount() }
            .also { println("$it") }
    }

    @Test
    fun testFed() {
        val instanceRepository = InstanceRepository(context, OkHttpClient())

        runBlocking { instanceRepository.getInstanceList() }
            .also { println("${it.size}") }
    }

    @Test
    fun testCrash() {
        val sender = EmailIntentSender()
    }
}
