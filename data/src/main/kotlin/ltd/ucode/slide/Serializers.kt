package ltd.ucode.slide

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

object Serializers {
    @OptIn(ExperimentalSerializationApi::class)
    val snakeCase = Json {
        encodeDefaults = true
        isLenient = true
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
        //decodeEnumsCaseInsensitive = true
    }
}
