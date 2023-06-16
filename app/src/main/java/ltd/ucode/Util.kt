package ltd.ucode

object Util {

    fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> {
        return mapNotNull { it.value?.let { value -> it.key to value } }
            .toMap()
    }

}
