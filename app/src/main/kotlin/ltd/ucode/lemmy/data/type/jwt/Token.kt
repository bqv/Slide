package ltd.ucode.lemmy.data.type.jwt

import android.util.Base64
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class Token(val token: String) {

    private val parts by lazy { token.split(".") }

    private val header by lazy { parts[0].let(::decode)
        .let(Json.Default::parseToJsonElement).jsonObject }

    val algorithm by lazy { header["alg"]!!.jsonPrimitive.content }

    val type by lazy { header["typ"]!!.jsonPrimitive.content }

    private val payload by lazy { parts[1].let(::decode)
        .let(Json.Default::parseToJsonElement).jsonObject }

    val claims by lazy { payload.toMutableMap()
        .mapValues { it.value } }

    val subject by lazy { claims["sub"]!!.jsonPrimitive.toString() }
    val issuer by lazy { claims["iss"]!!.jsonPrimitive.content }
    val issuedAtTime by lazy { claims["iat"]!!.jsonPrimitive.long
        .let { Instant.fromEpochSeconds(it) } }

    internal val signature by lazy { parts[2] }

    companion object {
        const val EXAMPLE = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOjMzMDM2LCJpc3MiOiJsZW1teS5tbCIsImlhdCI6MTY4NzU1NDYxNH0.jrEYVLgCBKehA_wIZE0Hw7BJEaRCpQUm_MEVuLSpnZw"
    }
}

private fun decode(s: String): String {
    val bytes = s.toByteArray(Charsets.UTF_8)
    val decoded = Base64.decode(bytes, Base64.DEFAULT)
    return String(decoded, Charsets.UTF_8)
}
