package ltd.ucode.lemmy.api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject

class ApiException(val path: String,
                   val statusCode: Int,
                   val errorBody: String,
                   val content: JSONObject = Json.decodeFromString(errorBody)
) : Exception(content.optString("error", null) ?: errorBody) {
}
