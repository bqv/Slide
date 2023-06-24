package ltd.ucode

import android.content.Context
import android.content.SharedPreferences
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import ltd.ucode.Util.ago
import ltd.ucode.lemmy.api.AccountDataSource
import ltd.ucode.lemmy.api.InstanceDataSource
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.lemmy.data.type.jwt.Token
import ltd.ucode.lemmy.repository.AccountRepository
import org.junit.Before
import org.mockito.Mockito
import java.io.File
import kotlin.test.Test


class Experiment {
    lateinit var context: Context
    lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setUp() {
        this.sharedPreferences = SPMockBuilder().createSharedPreferences()
        this.context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.getSharedPreferences("accounts", 0))
            .thenReturn(sharedPreferences)
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
        val source: InstanceDataSource = AccountDataSource(
            context, AccountRepository(context),
            account.usernameOrEmail,"lemmy.ml")
        runBlocking { source.nodeInfo() }
            .also { println("$it") }
        runBlocking { source.getSite() }
            .also { println("${it.siteView}") }
        runBlocking { source.getUnreadCount() }
            .also { println("$it") }
    }
}
