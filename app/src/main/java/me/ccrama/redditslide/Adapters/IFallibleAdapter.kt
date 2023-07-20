package me.ccrama.redditslide.Adapters

interface IFallibleAdapter {
    fun setError(b: Boolean)
    fun undoSetError()
}
