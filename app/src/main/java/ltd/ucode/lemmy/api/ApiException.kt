package ltd.ucode.lemmy.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ApiException(val path: String,
                        val statusCode: Int,
                        @Transient val errorBody: String,
                        val content: JsonElement = Json.parseToJsonElement(errorBody)
) : Exception(readError(content) ?: "") {
}

private fun readError(error: JsonElement): String? {
    return error.jsonObject["error"]?.jsonPrimitive?.contentOrNull ?.let {
        it.replace('_', ' ')
            .replaceFirstChar(Char::titlecase)
    }
}
