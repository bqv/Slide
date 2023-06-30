package ltd.ucode.lemmy.data.value

import kotlinx.serialization.Serializable

@Serializable
@JvmInline value class Addressable(val address: String) {
    companion object {
        const val delimiter = "@"
    }

    val hasUsername: Boolean get() = address.contains(delimiter)

    val username: String? get() = if (address.contains(delimiter)) {
        address.substringBeforeLast(delimiter)
    } else null

    val domain: String get() = address.substringAfterLast(delimiter)
}
