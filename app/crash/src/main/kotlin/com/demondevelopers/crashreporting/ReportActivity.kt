package com.demondevelopers.crashreporting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Builds the report and creates a ACTION_SEND_MULTIPLE intent with attachments
 *
 * Note: Check the AndroidManifest as this is started in a new process which
 * also must be where the ContentProvider is hosted!
 *
 */
class ReportActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Make use of a ProgressDialog while generating the asynchronously

        /*
		// Note: Must be used to debug, because this activity starts
		// in a new process which is not being debugged before.
		if(!Debug.isDebuggerConnected()){
			Debug.waitForDebugger();
		}
		*/
        val ex = intent
            .getSerializableExtra(EXTRA_THROWABLE) as Throwable?
        val report = StringWriter()
        val writer = PrintWriter(report)
        writer.println("CRASH REPORT\n")
        try {
            val info = packageManager
                .getPackageInfo(packageName, 0)
            writer.println("Application:")
            writer.printf(
                "Package: %s\nVersion Name:%s\nVersion Code: %d\n\n",
                info.packageName, info.versionName, info.versionCode
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Ignored, this shouldn't happen
        }
        if (ex!!.message != null && ex.message!!.length != 0) {
            writer.printf("Reason: %s\n", ex.message)
        }
        val stack = ex.stackTrace
        if (stack.size > 0) {
            writer.println("Stack Trace:")
            for (i in stack.indices) {
                writer.println(stack[i])
            }
        }
        val target = Intent(Intent.ACTION_SEND_MULTIPLE)
        target.type = "text/plain"
        target.putExtra(
            Intent.EXTRA_EMAIL, arrayOf(
                CrashReportHandler.emailAddress
            )
        )
        target.putExtra(Intent.EXTRA_SUBJECT, "Crash Report")
        val extra_text = ArrayList<String>()
        extra_text.add(report.toString())
        target.putStringArrayListExtra(Intent.EXTRA_TEXT, extra_text)
        val attachments = ArrayList<Uri>()
        val screenshot = intent.getStringExtra(EXTRA_SCREENSHOT)
        if (screenshot != null) {
            attachments.add(
                ReportFilesProvider.Companion.setFilePath(
                    ReportFilesProvider.Companion.FILE_INDEX_SCREENSHOT, screenshot
                )
            )
        }
        val eventLogPath = CrashReportHandler.saveEventLog()
        if (eventLogPath != null) {
            attachments.add(
                ReportFilesProvider.Companion.setFilePath(
                    ReportFilesProvider.Companion.FILE_INDEX_EVENTLOG, eventLogPath
                )
            )
        }
        val systemLogPath = CrashReportHandler.saveSystemLog()
        if (systemLogPath != null) {
            attachments.add(
                ReportFilesProvider.Companion.setFilePath(
                    ReportFilesProvider.Companion.FILE_INDEX_SYSTEMLOG, systemLogPath
                )
            )
        }
        target.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments)
        startActivity(Intent.createChooser(target, "Send Crash Report Using"))
        finish()
    }

    companion object {
        private const val EXTRA_THROWABLE = "extraThrowable"
        private const val EXTRA_SCREENSHOT = "extraScreenshot"
        fun createIntent(
            context: Context?, throwable: Throwable?,
            screenshotPath: String?
        ): Intent {
            return Intent(context!!.applicationContext, ReportActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_THROWABLE, throwable)
                .putExtra(EXTRA_SCREENSHOT, screenshotPath)
        }
    }
}
