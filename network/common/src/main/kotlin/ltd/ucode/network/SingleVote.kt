package ltd.ucode.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.sign

@Serializable(with = SingleVote.Serializer::class)
/**
 * A single vote value.
 *
 * Distinct values are UPVOTE, NOVOTE, and DOWNVOTE.
 *
 * @param score a signed integer (only sign matters)
 */
@JvmInline value class SingleVote(val score: Int) {
    companion object { // pseudo enum
        val UPVOTE get() = SingleVote(+1)
        val NOVOTE get() = SingleVote(0)
        val DOWNVOTE get() = SingleVote(-1)

        fun of(maybeScore: Int?) = when (maybeScore?.let { it > 0 }) {
            null -> { SingleVote.NOVOTE }
            true -> { SingleVote.UPVOTE }
            false -> { SingleVote.DOWNVOTE }
        }
    }

    class Serializer : KSerializer<SingleVote> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor(Serializer::class.qualifiedName!!,
                PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): SingleVote {
            return SingleVote(decoder.decodeInt())
        }

        override fun serialize(encoder: Encoder, value: SingleVote) {
            encoder.encodeInt(value.score.sign)
        }
    }
}
