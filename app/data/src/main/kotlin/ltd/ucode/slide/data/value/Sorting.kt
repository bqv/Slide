package ltd.ucode.slide.data.value

sealed class Sorting {
    class New(val comments: Boolean = false) : Sorting() // Remote Mediated
    class Old(val controversial: Boolean = false) : Sorting() // Remote Mediated (Reverse)
    class Top(val comments: Boolean = false) : Sorting() // Oneshot
    class Hot(val active: Boolean = false) : Sorting()   // Oneshot
}
