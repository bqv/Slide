package ltd.ucode.slide.data.value

sealed class Feed private constructor (val authenticated: Boolean) {
    object Subscribed : Feed(true)
    object Local : Feed(false)
    object All : Feed(false)
    class Group(val name: String) : Feed(false)
    class User(val name: String) : Feed(false)
}
