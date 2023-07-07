package me.ccrama.redditslide.ui.settings

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.afollestad.materialdialogs.MaterialDialog
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Synccit.MySynccitReadTask
import me.ccrama.redditslide.Synccit.MySynccitUpdateTask
import me.ccrama.redditslide.Synccit.SynccitRead

class SettingsSynccit : BaseActivityAnim() {
    var name: EditText? = null
    var auth: EditText? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_settings_synccit)
        setupAppBar(R.id.toolbar, R.string.settings_synccit, true, true)
        name = findViewById<View>(R.id.name) as EditText
        auth = findViewById<View>(R.id.auth) as EditText
        name!!.setText(SettingValues.synccitName)
        auth!!.setText(SettingValues.synccitAuth)
        if (SettingValues.synccitAuth!!.isEmpty()) {
            findViewById<View>(R.id.remove).isEnabled = false
        }
        findViewById<View>(R.id.remove).setOnClickListener {
            if (!SettingValues.synccitAuth!!.isEmpty()) {
                AlertDialog.Builder(this@SettingsSynccit)
                    .setTitle(R.string.settings_synccit_delete)
                    .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                        SettingValues.synccitName = ""
                        SettingValues.synccitAuth = ""
                        /* dontcare
                                    SharedPreferences.Editor e = SettingValues.prefs.edit();

                                    e.putString(SettingValues.SYNCCIT_NAME, SettingValues.synccitName);
                                    e.putString(SettingValues.SYNCCIT_AUTH, SettingValues.synccitAuth);
                                    e.apply();
                                     */name!!.setText(SettingValues.synccitName)
                        auth!!.setText(SettingValues.synccitAuth)
                        SynccitRead.visitedIds.removeAll(setOf("16noez"))
                    }
                    .setNegativeButton(R.string.btn_no, null)
                    .show()
            }
        }
        findViewById<View>(R.id.save).setOnClickListener {
            val d: Dialog = MaterialDialog(this@SettingsSynccit)
                .title(R.string.settings_synccit_authenticate)
                //.progress(true, 100)
                .message(R.string.misc_please_wait)
                .cancelable(false)
                .also { it.show() }
            MySynccitUpdateTask().execute("16noez")
            SettingValues.synccitName = name!!.text.toString()
            SettingValues.synccitAuth = auth!!.text.toString()
            try {
                MySynccitReadTask().execute("16noez").get()
                if (SynccitRead.visitedIds.contains("16noez")) {
                    //success
                    d.dismiss()
                    /* dontcare
                                    SharedPreferences.Editor e = SettingValues.prefs.edit();

                                    e.putString(SettingValues.SYNCCIT_NAME, SettingValues.synccitName);
                                    e.putString(SettingValues.SYNCCIT_AUTH, SettingValues.synccitAuth);
                                    e.apply();
                                     */findViewById<View>(R.id.remove).isEnabled = true
                    AlertDialog.Builder(this@SettingsSynccit)
                        .setTitle(R.string.settings_synccit_connected)
                        .setMessage(R.string.settings_synccit_active)
                        .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int -> finish() }
                        .setOnDismissListener { dialog: DialogInterface? -> finish() }
                        .show()
                } else {
                    d.dismiss()
                    AlertDialog.Builder(this@SettingsSynccit)
                        .setTitle(R.string.settings_synccit_failed)
                        .setMessage(R.string.settings_synccit_failed_msg)
                        .setPositiveButton(R.string.btn_ok, null)
                        .show()
                }
            } catch (e: Exception) {
                d.dismiss()
                AlertDialog.Builder(this@SettingsSynccit)
                    .setTitle(R.string.settings_synccit_failed)
                    .setMessage(R.string.settings_synccit_failed_msg)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show()
            }
        }
    }
}
