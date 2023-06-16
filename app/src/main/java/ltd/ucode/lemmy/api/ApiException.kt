package ltd.ucode.lemmy.api

import org.json.JSONObject

class ApiException(val path: String,
                   val statusCode: Int,
                   val errorBody: String,
                   val content: JSONObject = JSONObject(errorBody)
) : Exception(content.optString("error", null) ?: errorBody) {
}
