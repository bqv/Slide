package ltd.ucode.network.lemmy.api

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ltd.ucode.util.extensions.StringExtensions.isHTML
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Selector.SelectorParseException

data class ApiException(override val path: String,
                        override val statusCode: Int,
                        @Transient override val errorBody: String,
                        val jsonContent: JsonElement? = readBody(errorBody),
                        val htmlContent: Document? = readPage(errorBody, path)
) : Exception(
    jsonContent?.let { readError(path, statusCode, it) }
        ?: htmlContent?.let { readError(path, statusCode, it) }
        ?: errorBody
), NetworkException {
    companion object {
        private fun readError(path: String, statusCode: Int, error: JsonElement): String? {
            return error.jsonObject["error"]?.jsonPrimitive?.contentOrNull?.let {
                it.replace('_', ' ')
                    .replaceFirstChar(Char::titlecase)
            }
        }

        private fun readError(path: String, statusCode: Int, error: Document): String {
            return error.wholeText()
                .trim()
                .replace(Regex(" +"), " ")
                .let { "[$statusCode] $it" }
        }

        private fun readBody(body: String): JsonElement? {
            return try {
                Json.parseToJsonElement(body)
            } catch (e: SerializationException) {
                null
            }
        }

        private fun readPage(body: String, url: String): Document? {
            return try {
                if (body.isHTML) {
                    Jsoup.parse(body, url)
                } else {
                    null
                }
            } catch (e: SelectorParseException) {
                null
            }
        }
    }

    sealed interface Reason {
        object Unauthenticated : Reason {}
        object Null : Reason {}
        class Other(val s: String) : Reason {}
    }

    val reason: Reason get() =
        when (val err = jsonContent?.jsonObject?.get("error")?.jsonPrimitive?.contentOrNull) {
            "not_logged_in" -> Reason.Unauthenticated
            null -> Reason.Null
            else -> Reason.Other(
                err.replace('_', ' ').replaceFirstChar(Char::titlecase)
            )
        }

    override fun rethrow(): Nothing {
        throw this
    }

    override fun upcast(): Exception {
        return this
    }
}
