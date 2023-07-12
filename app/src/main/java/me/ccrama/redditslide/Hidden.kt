package me.ccrama.redditslide

import android.os.AsyncTask
import ltd.ucode.network.data.IPost

object Hidden {
    val id: MutableSet<String> = HashSet()
    @JvmStatic
    fun setHidden(s: IPost) {
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(params: Array<Void?>): Void? {
                try {
                    id.add(s.uri)
                    //AccountManager(Authentication.reddit).hide(true, s as IPost)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    @JvmStatic
    fun undoHidden(s: IPost) {
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(params: Array<Void?>): Void? {
                try {
                    id.remove(s.uri)
                    //AccountManager(Authentication.reddit).hide(false, s as IPost)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}
