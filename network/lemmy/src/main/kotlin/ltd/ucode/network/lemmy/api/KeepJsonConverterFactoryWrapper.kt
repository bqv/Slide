package ltd.ucode.network.lemmy.api

import kotlinx.serialization.json.Json
import ltd.ucode.network.lemmy.api.response.IResponse
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

internal class KeepJsonConverterFactoryWrapper(
    // This is the converter factory the deserialization will be delegated to
    private val backingConverterFactory: Converter.Factory,
) : Converter.Factory() {
    override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<ResponseBody, *> {
        require (type is Class<*>) { "$type is not a class" }
        require(IResponse::class.java.isAssignableFrom(type)) { "$type is not assignable from IResponse" }
        val responseBodyConverter = backingConverterFactory.responseBodyConverter(type, annotations, retrofit)
            as Converter<ResponseBody, IResponse>
        return WrappedResponseBodyConverter(responseBodyConverter)
    }

    private class WrappedResponseBodyConverter<Target : IResponse>(
        private val responseBodyConverter: Converter<ResponseBody, Target>,
    ) : Converter<ResponseBody, Target> {
        @Throws(IOException::class)
        override fun convert(responseBody: ResponseBody): Target? {
            val body = responseBody.source().peek().readUtf8()
            return responseBodyConverter.convert(responseBody)
                ?.also {
                    if (body.isNotBlank())
                        it.raw = Json.parseToJsonElement(body)
                }
        }
    }

    companion object {
        fun Converter.Factory.keepJson(): Converter.Factory {
            return KeepJsonConverterFactoryWrapper(this)
        }

        fun KeepJsonConverterFactoryWrapper.wrap(factory: Converter.Factory): Converter.Factory {
            return KeepJsonConverterFactoryWrapper(factory)
        }
    }
}
