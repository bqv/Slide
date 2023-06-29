package com.demondevelopers.crashreporting

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.os.Debug
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object CrashReportHandler {
    private val TAG = CrashReportHandler::class.java.simpleName
    private val EVENT_LOG_CMD = arrayOf("logcat", "-d", "-b", "events", "-v", "time")
    private val SYSTEM_LOG_CMD = arrayOf("logcat", "-d", "-v", "time")
    private var mAndroidHandler: Thread.UncaughtExceptionHandler? = null
    private var mCustomHandler: Thread.UncaughtExceptionHandler? = null
    private var mAppContext: Context? = null
    var emailAddress: String? = null
        private set

    @Volatile
    private var bCrashing = false
    private var bInstalled = false
    val isDebug: Boolean
        get() = Debug.waitingForDebugger() || Debug.isDebuggerConnected()

    fun install(context: Context, emailAddress: String?) {
        if (isDebug) {
            // NOTE: It does not generate crash reports when you are debugging your app.
            return
        }
        check(!bInstalled) { "Already installed" }
        mAndroidHandler = Thread.getDefaultUncaughtExceptionHandler()
        mCustomHandler = UncaughtHandler()
        mAppContext = context.applicationContext
        CrashReportHandler.emailAddress = emailAddress
        Thread.setDefaultUncaughtExceptionHandler(mCustomHandler)
        bInstalled = true
    }

    fun uninstall() {
        check(bInstalled) { "Not installed" }
        Thread.setDefaultUncaughtExceptionHandler(mAndroidHandler)
        bInstalled = false
    }

    fun reinstall() {
        check(!bInstalled) { "Still installed" }
        Thread.setDefaultUncaughtExceptionHandler(mCustomHandler)
        bInstalled = true
    }

    val screenshot: Bitmap?
        get() {
            val activity = ReportingActivityLifecycleCallbacks.currentActivity
                ?: return null
            val window = activity.window ?: return null
            val view = window.decorView ?: return null
            view.buildDrawingCache()
            var cache = view.drawingCache
            val screenshot = cache!!.copy(cache.config, false)
            cache = null
            if (!view.isDrawingCacheEnabled) {
                view.destroyDrawingCache()
            }
            return screenshot
        }

    fun saveScreenShot(bitmap: Bitmap): String? {
        var stream: FileOutputStream? = null
        try {
            val temp = File.createTempFile("crash-report", ".jpg")
            stream = FileOutputStream(temp)
            bitmap.compress(CompressFormat.JPEG, 80, stream)
            return temp.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.message, e)
                }
            }
        }
        return null
    }

    fun saveEventLog(): String? {
        return captureCommand("event-log", EVENT_LOG_CMD)
    }

    fun saveSystemLog(): String? {
        return captureCommand("system-log", SYSTEM_LOG_CMD)
    }

    private fun captureCommand(filePrefix: String, command: Array<String>): String? {
        var process: Process? = null
        var `is`: InputStream? = null
        var fos: FileOutputStream? = null
        try {
            val temp = File.createTempFile(filePrefix, ".txt")
            fos = FileOutputStream(temp)
            process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            `is` = process.inputStream
            val reader = BufferedReader(InputStreamReader(`is`))
            val writer = BufferedWriter(OutputStreamWriter(fos))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                writer.append(line).append('\n')
            }
            writer.flush()
            return temp.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        } finally {
            process?.destroy()
            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.message, e)
                }
            }
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.message, e)
                }
            }
        }
        return null
    }

    private class UncaughtHandler : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, ex: Throwable) {
            try {
                // Don't re-enter -- avoid infinite loops if crash-reporting crashes.
                if (bCrashing) {
                    return
                }
                bCrashing = true
                Log.e(TAG, "FATAL EXCEPTION: " + thread.name, ex)
                // Attempt to save a screenshot (no permissions required!)
                var screenshot: String? = null
                val bm = CrashReportHandler.screenshot
                if (bm != null) {
                    screenshot = saveScreenShot(bm)
                    bm.recycle()
                }
                // Bring up crash dialog
                val context = mAppContext
                context!!.startActivity(
                    ReportActivity.Companion.createIntent(
                        context,
                        ex,
                        screenshot
                    )
                )
            } catch (t: Throwable) {
                try {
                    Log.e(TAG, "Error reporting crash", t)
                } catch (ignored: Throwable) {
                    // Even Log.e() fails! Oh well.
                }
            } finally {
                // Try everything to make sure this process goes away.
                Log.d(TAG, "Quitting")
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10) // magic numbers!
            }
        }
    }
}
