package ltd.ucode.slide

import android.app.Service
import android.content.Intent
import android.os.IBinder
import zerobranch.androidremotedebugger.AndroidRemoteDebugger

class DebugService : Service() {
    private var startMode: Int = START_STICKY
    private var binder: IBinder? = null
    private var allowRebind: Boolean = true

    override fun onCreate() {
        // Created
        AndroidRemoteDebugger.init(
            AndroidRemoteDebugger.Builder(applicationContext)
                .enabled(true)
                .excludeUncaughtException()
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // starting, from startService()
        return startMode
    }

    override fun onBind(intent: Intent): IBinder? {
        // starting, from bindService()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // All clients unbound, with unbindService()
        return allowRebind
    }

    override fun onRebind(intent: Intent?) {
        // rebinding, with bindService()
    }

    override fun onDestroy() {
        // stopping
    }
}
