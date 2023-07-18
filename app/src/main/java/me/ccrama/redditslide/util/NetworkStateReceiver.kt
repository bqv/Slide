package me.ccrama.redditslide.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetworkStateReceiver : BroadcastReceiver() {
    protected var listeners: MutableList<NetworkStateReceiverListener> = ArrayList()
    protected var connected: Boolean? = null

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.extras == null) return
        connected = NetworkUtil.isConnected(context)
        notifyStateToAll()
    }

    private fun notifyStateToAll() {
        for (listener in listeners) notifyState(listener)
    }

    private fun notifyState(listener: NetworkStateReceiverListener?) {
        if (connected == null || listener == null) return
        if (connected!!) listener.networkAvailable() else listener.networkUnavailable()
    }

    fun addListener(l: NetworkStateReceiverListener) {
        listeners.add(l)
        notifyState(l)
    }

    fun removeListener(l: NetworkStateReceiverListener) {
        listeners.remove(l)
    }

    interface NetworkStateReceiverListener {
        fun networkAvailable()
        fun networkUnavailable()
    }
}
