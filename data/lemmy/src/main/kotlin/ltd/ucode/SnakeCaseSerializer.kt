package ltd.ucode

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@OptIn(ExperimentalSerializationApi::class)
val SnakeCaseSerializer = Json {
    encodeDefaults = true
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}
