package friendly.android

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * This function has been designed to provide a serializer for a type that isn't
 * serializable.
 */
@PublishedApi
internal fun <T, S> KSerializer<S>.map(
    toSerializable: (T) -> S,
    fromSerializable: (S) -> T,
): KSerializer<T> = object : KSerializer<T> {
    override val descriptor: SerialDescriptor = this@map.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(this@map, toSerializable(value))
    }

    override fun deserialize(decoder: Decoder): T =
        fromSerializable(decoder.decodeSerializableValue(this@map))
}
