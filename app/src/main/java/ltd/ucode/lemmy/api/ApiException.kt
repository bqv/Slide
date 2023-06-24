package ltd.ucode.lemmy.api

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ApiException(val path: String,
                        val statusCode: Int,
                        @Transient val errorBody: String,
                        val content: JsonElement? =
                            if (statusCode >= 500) throw ServerException(path, statusCode)
                            else readBody(errorBody)
) : Exception(content?.let { readError(path, statusCode, it) } ?: errorBody) {
    companion object {
        private fun readError(path: String, statusCode: Int, error: JsonElement): String? {
            val errName = error.jsonObject["error"]?.jsonPrimitive?.contentOrNull

            when (errName) {
                "not_logged_in" -> throw AuthenticationException(path, statusCode)
            }

            return errName?.let {
                it.replace('_', ' ')
                    .replaceFirstChar(Char::titlecase)
            }
        }

        private fun readBody(body: String): JsonElement? {
            return try {
                Json.parseToJsonElement(body)
            } catch (e: SerializationException) {
                null
            }
        }
    }

    data class AuthenticationException(val path: String,
                                       val statusCode: Int
    ) : Exception("HTTP $statusCode") {
    }

    data class ServerException(val path: String,
                               val statusCode: Int
    ) : Exception("HTTP $statusCode") {
    }
}
