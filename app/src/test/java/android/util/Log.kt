package android.util

class Log {
    fun v(vararg params: Any) {
    }

    companion object {
        @JvmStatic fun v(a: String, b: String): Int {
            println("$a: $b")
            return 0
        }
    }
}
