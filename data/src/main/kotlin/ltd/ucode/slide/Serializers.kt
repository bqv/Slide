package ltd.ucode.slide

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.net.URI
import java.net.URL

object Serializers {
    val module = SerializersModule {
        polymorphic(URL::class, URLSerializer)
        polymorphic(URI::class, URISerializer)
    }

    @OptIn(ExperimentalSerializationApi::class)
    val snakeCase = Json {
        encodeDefaults = true
        isLenient = true
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
        //decodeEnumsCaseInsensitive = true
        serializersModule = module
    }
}
