package me.ccrama.redditslide.ui.settings

import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.processphoenix.ProcessPhoenix
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.util.FileUtil
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.ProUtil
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Calendar

class SettingsBackup : BaseActivityAnim() {
    var progress: MaterialDialog? = null
    var title: String? = null
    var file: File? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42) {
            progress = MaterialDialog(this@SettingsBackup)
                .title(R.string.backup_restoring)
                .message(R.string.misc_please_wait)
                .cancelable(false)
                //.progress(true, 1)
            progress!!.show()
            if (data != null) {
                val fileUri = data.data
                Log.v(LogUtil.getTag(), "WORKED! " + fileUri.toString())
                val fw = StringWriter()
                try {
                    val `is` = contentResolver.openInputStream((fileUri)!!)
                    val reader = BufferedReader(InputStreamReader(`is`))
                    var c = reader.read()
                    while (c != -1) {
                        fw.write(c)
                        c = reader.read()
                    }
                    val read = fw.toString()
                    if (read.contains("Slide_backupEND>")) {
                        val files = read.split("END>\\s*".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        progress!!.dismiss()
                        progress = MaterialDialog(this@SettingsBackup)
                            .title(R.string.backup_restoring) //.progress(false, files.length - 1)
                        progress!!.show()
                        for (i in 1 until files.size) {
                            var innerFile = files[i]
                            val t = innerFile.substring(6, innerFile.indexOf(">"))
                            innerFile = innerFile.substring(
                                innerFile.indexOf(">") + 1
                            )
                            val newF = File(
                                (applicationInfo.dataDir
                                        + File.separator
                                        + "shared_prefs"
                                        + File.separator
                                        + t)
                            )
                            Log.v(LogUtil.getTag(), "WRITING TO " + newF.absolutePath)
                            try {
                                val newfw = FileWriter(newF)
                                val bw = BufferedWriter(newfw)
                                bw.write(innerFile)
                                bw.close()
                                //progress.setProgress(progress.getCurrentProgress() + 1)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                        AlertDialog.Builder(this@SettingsBackup)
                            .setTitle(R.string.backup_restore_settings)
                            .setMessage(R.string.backup_restarting)
                            .setOnDismissListener { dialog: DialogInterface? ->
                                ProcessPhoenix.triggerRebirth(
                                    this@SettingsBackup
                                )
                            }
                            .setPositiveButton(
                                R.string.btn_ok
                            ) { dialog: DialogInterface?, which: Int ->
                                ProcessPhoenix.triggerRebirth(this@SettingsBackup)
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        progress!!.hide()
                        AlertDialog.Builder(this@SettingsBackup)
                            .setTitle(R.string.err_not_valid_backup)
                            .setMessage(R.string.err_not_valid_backup_msg)
                            .setPositiveButton(R.string.btn_ok, null)
                            .setCancelable(false)
                            .show()
                    }
                } catch (e: Exception) {
                    progress!!.hide()
                    e.printStackTrace()
                    AlertDialog.Builder(this@SettingsBackup)
                        .setTitle(R.string.err_file_not_found)
                        .setMessage(R.string.err_file_not_found_msg)
                        .setPositiveButton(R.string.btn_ok, null)
                        .show()
                }
            } else {
                progress!!.dismiss()
                AlertDialog.Builder(this@SettingsBackup)
                    .setTitle(R.string.err_file_not_found)
                    .setMessage(R.string.err_file_not_found_msg)
                    .setPositiveButton(R.string.btn_ok, null)
                    .setCancelable(false)
                    .show()
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_settings_sync)
        setupAppBar(R.id.toolbar, R.string.settings_title_backup, true, true)
        if (SettingValues.isPro) {
            findViewById<View>(R.id.backfile).setOnClickListener(
                View.OnClickListener {
                    AlertDialog.Builder(this@SettingsBackup)
                        .setTitle(R.string.settings_backup_include_personal_title)
                        .setMessage(R.string.settings_backup_include_personal_text)
                        .setPositiveButton(
                            R.string.btn_yes,
                            { dialog: DialogInterface?, which: Int -> backupToDir(false) })
                        .setNegativeButton(
                            R.string.btn_no,
                            { dialog: DialogInterface?, which: Int -> backupToDir(true) })
                        .setNeutralButton(R.string.btn_cancel, null)
                        .setCancelable(false)
                        .show()
                })
            findViewById<View>(R.id.restorefile).setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "file/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                val mimeTypes = arrayOf("text/plain")
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                startActivityForResult(intent, 42)
            }
        } else {
            ProUtil.proUpgradeMsg(this, R.string.general_backup_ispro)
                .setNegativeButton(
                    R.string.btn_no_thanks
                ) { dialog: DialogInterface?, whichButton: Int -> finish() }
                .setCancelable(false)
                .show()
        }
    }

    fun backupToDir(personal: Boolean) {
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun onPreExecute() {
                progress = MaterialDialog(this@SettingsBackup)
                    .cancelable(false)
                    .title(R.string.backup_backing_up)
                    //.progress(false, 40)
                    .cancelable(false)
                progress!!.show()
            }

            override fun doInBackground(vararg params: Void?): Void? {
                val prefsdir = File(applicationInfo.dataDir, "shared_prefs")
                if (prefsdir.exists() && prefsdir.isDirectory) {
                    val list = prefsdir.list()
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .mkdirs()
                    val backedup = File(
                        (Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        )
                            .toString() + File.separator
                                + "Slide"
                                + SimpleDateFormat("-yyyy-MM-dd-HH-mm-ss").format(
                            Calendar.getInstance().time
                        )
                                + (if (!personal) "-personal" else "")
                                + ".txt")
                    )
                    file = backedup
                    var fw: FileWriter? = null
                    try {
                        backedup.createNewFile()
                        fw = FileWriter(backedup)
                        fw.write("Slide_backupEND>")
                        for (s: String in list) {
                            if (!s.contains("cache") && !s.contains("ion-cookies") && !s.contains(
                                    "albums"
                                ) && !s.contains("com.google") && (!personal || ((!s.contains("SUBSNEW")
                                        && !s.contains("appRestart")
                                        && !s.contains("STACKTRACE")
                                        && !s.contains("AUTH")
                                        && !s.contains("TAGS")
                                        && !s.contains("SEEN")
                                        && !s.contains("HIDDEN")
                                        && !s.contains("HIDDEN_POSTS"))))
                            ) {
                                var fr: FileReader? = null
                                try {
                                    fr = FileReader(prefsdir.toString() + File.separator + s)
                                    var c = fr.read()
                                    fw.write("<START" + File(s).name + ">")
                                    while (c != -1) {
                                        fw.write(c)
                                        c = fr.read()
                                    }
                                    fw.write("END>")
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                } finally {
                                    close(fr)
                                }
                            }
                        }
                        return null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        //todo error
                    } finally {
                        close(fw)
                    }
                }
                return null
            }

            override fun onPostExecute(aVoid: Void?) {
                progress!!.dismiss()
                AlertDialog.Builder(this@SettingsBackup)
                    .setTitle(R.string.backup_complete)
                    .setMessage(R.string.backup_saved_downloads)
                    .setPositiveButton(R.string.btn_view) { dialog: DialogInterface?, which: Int ->
                        val intent: Intent = FileUtil.getFileIntent(
                            file,
                            Intent(Intent.ACTION_VIEW),
                            this@SettingsBackup
                        )
                        if (intent.resolveActivityInfo(packageManager, 0) != null) {
                            startActivity(
                                Intent.createChooser(
                                    intent, getString(R.string.settings_backup_view)
                                )
                            )
                        } else {
                            val s: Snackbar = Snackbar.make(
                                findViewById(R.id.restorefile),
                                getString(
                                    R.string.settings_backup_err_no_explorer,
                                    file!!.absolutePath + file
                                ),
                                Snackbar.LENGTH_INDEFINITE
                            )
                            LayoutUtils.showSnackbar(s)
                        }
                    }
                    .setNegativeButton(R.string.btn_close, null)
                    .setCancelable(false)
                    .show()
            }
        }.execute()
    }

    companion object {
        fun close(stream: Closeable?) {
            try {
                stream?.close()
            } catch (ignored: IOException) {
            }
        }
    }
}
