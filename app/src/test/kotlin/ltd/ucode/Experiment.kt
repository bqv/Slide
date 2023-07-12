package ltd.ucode

import ltd.ucode.network.lemmy.data.type.jwt.Token
import ltd.ucode.util.extensions.InstantExtensions.ago
import org.junit.Before
import kotlin.test.Test

class Experiment {
    @Before
    fun setUp() {
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
        //println("Signature: ${token.signature}")
    }
}
