package ltd.ucode.crash

import android.content.Context
import android.util.Base64
import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.kohsuke.github.GHAppInstallation
import org.kohsuke.github.GHAppInstallationToken
import org.kohsuke.github.GHIssue
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHLabel
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date

class GithubIssueTracker(context: Context? = null) {
    private enum class Label(name: String) {
        CRASH("crash"),
        BUG("bug"),
        DOCUMENTATION("documentation"),
        DUPLICATE("documentation"),
        ENHANCEMENT("enhancement"),
        GOOD_FIRST_ISSUE("good first issue"),
        HELP_WANTED("help wanted"),
        INVALID("invalid"),
        QUESTION("question"),
        WONT_FIX("wontfix");
    }

    operator fun get(key: String): Issue {
        val label: GHLabel = repository.getLabel(Label.CRASH.name)

        fun canary(hash: String = "xxxxxxxx") = "<!--CRASH:$hash-->"
        Log.d(BuildConfig.LIBRARY_PACKAGE_NAME, "Wanting \"${canary(key)}\"")
        val issues = repository.getIssues(GHIssueState.ALL)
            .filter { it.labels.contains(label) }
            .also {
                Log.d(BuildConfig.LIBRARY_PACKAGE_NAME, "Scanning ${it.size} issues")
            }
            .sortedByDescending { it.createdAt }
            .associateBy {
                it.body.substring(0, minOf(it.body.length, canary().length))
                    .also {
                        Log.d(BuildConfig.LIBRARY_PACKAGE_NAME, "Candidate: \"$it\"")
                    }
            }

        return issues.firstNotNullOfOrNull {
            if (it.key == canary(key)) it.value
                .also {
                    Log.d(BuildConfig.LIBRARY_PACKAGE_NAME, "Found preexisting")
                }
            else null
        }.let(::Issue)
    }

    inner class Issue(private var issue: GHIssue? = null) {
        val isNew = issue == null

        fun post(title: String, body: String) {
            if (issue == null)
                issue = repository.createIssue(title)
                    .label(Label.CRASH.name)
                    .body(body)
                    .create()
            else
                issue!!.comment(body)
        }
    }

    companion object {
        private const val aid = 355146
        private val pem: String = """
            MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDNfWTfIC5cGRURJNNfaTNIHlhKBdwQ
            xcA9ZQo0Z73zuJlL+lZZ6sooINi4FgtGZUH2IDyZiDB9k3sU/fV1Vv4BhJvWCyGODPpkVil4hHVwPdmJ
            ax4FQffw5jL72uvGGr+0/uT++lBm/vYd14oOmCO1y1R6vbkogGgX/f+yLkmrvV02JqWBBgSrL/KMya31
            91MvAXAEWcwU8jXaa5gXkOJ7iZeDgPZWQygGEIRnRms324jCK3p4RS3/mZw8ZUBpbG4QStNaoWqVK7IO
            hLCvaNu3cF6Q5/PtAHjZa89fkca8jao3Eke4ptBCI49Q8L3SPGhr3iQ0+tTH4qxgP2PtA7q5AgMBAAEC
            ggEACgCOhmXUm9sEsX7vYveF7X68oW3DlnVptBNU+dQK5PAZVh9rUJkjGezvX5aDGfwXx9kt2fNNGO+y
            0ATEi9+eIYjI5RRJn9Z14ahoAFvCM6JvEkdKgMsDPJCX7rZrP4tRxYeaseaRP1ItvKofKDKzmke5ZvgR
            KS2G45QTfCLQpzFMIBPhr3gV8O109VtGZpz/0EE3epcDrLydvOOUoJCFIaE2fL6WwgjKjvin3U3BPNgP
            GJzO9LtxP3RbgWxkUzf4KNLDNeMYxBH0q+Xs1p+dwlHWm8TxIv5zIr8kOzjair+0kDPpcA3yyUXd6cHA
            6raVBgfMlLbkIGr4KO2QJxoGmQKBgQDroWSAVtckK7uNdaCufejVBcq1n49TgNsTYK2LBu1zmCtW7DbI
            8M7nAf+hVpkE6ZBwkkeajlmxvE3tWBtAWRvJVnR1yviMnLaG2WwV8CqbEE7T0l9B2hzIoqfuFrnv1Ae2
            xEeYvHiScoh+5JFofYPN1GRrO4hvt2vIwkv0wV96owKBgQDfQPnbtQkLwIGQrn6QkkQ6Aff+TShX2ytR
            3Pyvex6P8IBuBC8XrbdLyd3w3xznnz4ZQbal2VsyMx3c+SmZX71aWZ94okDPRXDGlauQl2b9P1GeMkqr
            Vn5mxMlrSWXm/YdzYCKq0JLO94SoxP76cxia3REv3WbFa7OsQI+tEhuG8wKBgQCnZMqfGhJREfh1sGDs
            VWp5G1o82RPbQKliBMaFA/Dgs/PmFn19FwYFure+CGVAxiTktCbGN+aki5/Yw0To2+UPjanCnOUiD5rk
            BcXxd+LsshMuDD+76pWUO2mNjPue00R/pMUwToRhlZg+fWaHktN2ADMusuZkZyvdPZr0UAPifQKBgQCC
            DfYAGYw1fmV8BUrRqYN5T2BKkmQoGhM4U0YYa96392C5tlJAtwAKdISIJ1FNVST1zaQ7JU3NBp4k9jlX
            kcBa+868lbivhkJWTSZuyuRCLzq6r410FqT39Tdo+o8UaykW+y/21h5P1z0+m9P1zkrNHG9AtPeDlmRK
            uil4pw3GJQKBgQCHfX0LBXwJjer7emyYkQyeU+rZElh1CJQSsxSDM56wEGNjWCQSX3G0ZXVXb0GPs/Sy
            sTxgY1CsrNkoNV26huNykkueNKsSg5IsufDt1IurujzkRls835CCQARiu8qzz45HviOqEbQ5vou9Pn/q
            n77E24CYm600phWkuqZg5wLPGA==
        """.trimIndent().replace("\n", "")

        private const val user = "bqv"
        private const val repo = "slide"
    }

    private val bot: GitHub by lazy {
        val privateKey: RSAPrivateKey = KeyFactory.getInstance("RSA").run {
            val bytes: ByteArray = Base64.decode(pem, Base64.DEFAULT)
            val spec = PKCS8EncodedKeySpec(bytes)
            generatePrivate(spec) as RSAPrivateKey
        }

        val auth: String = System.currentTimeMillis().let { now ->
            JWT.create()
                .withIssuedAt(Date(now))
                .withExpiresAt(Date(now + (10 * 60)))
                .withIssuer(aid.toString())
                .sign(Algorithm.RSA256(null, privateKey))
        }

        GitHubBuilder().withJwtToken(auth).build()
    }

    private val client: GitHub by lazy {
        val installation: GHAppInstallation = bot.app.getInstallationByUser(user)

        val token: GHAppInstallationToken = installation.createToken().create()!!

        GitHubBuilder().withAppInstallationToken(token.token).build()
    }

    private val repository: GHRepository = client.getRepository("$user/$repo")
}
