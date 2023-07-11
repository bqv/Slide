package ltd.ucode.slide

//import java.security.spec.PKCS8EncodedKeySpec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializer(forClass = PublicKey::class)
object PublicKeySerializer : KSerializer<PublicKey> {
    override val descriptor: SerialDescriptor
        get() = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: PublicKey) {
        TODO("Not yet implemented")
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun deserialize(decoder: Decoder): PublicKey {
        val pem = decoder.decodeString().trim()
        val bytes = Base64.decode(
            pem.split("\n")
                .drop(1)
                .dropLast(1)
                .joinToString("")
        )
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }
}
