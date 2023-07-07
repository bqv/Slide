package me.ccrama.redditslide.ui.settings

import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.getLayoutSettings
import ltd.ucode.slide.SettingValues.hasPicsEnabled
import ltd.ucode.slide.SettingValues.resetPicsEnabled
import ltd.ucode.slide.SettingValues.setLayoutSettings
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.UserSubscriptions.getAllUserSubreddits
import me.ccrama.redditslide.UserSubscriptions.getSubscriptions
import me.ccrama.redditslide.UserSubscriptions.sort
import me.ccrama.redditslide.UserSubscriptions.syncSubredditsGetObject
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.GetClosestColor
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.LayoutUtils

class SettingsSubreddit : BaseActivityAnim() {
    var mSettingsSubAdapter: SettingsSubAdapter? = null
    var changedSubs = ArrayList<String>()
    private var recycler: RecyclerView? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            val i = Intent(this@SettingsSubreddit, SettingsSubreddit::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(i)
            overridePendingTransition(0, 0)
            finish()
            overridePendingTransition(0, 0)
        }
    }

    var done = 0
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_settings_subreddit)
        SettingsThemeFragment.changed = true
        setupAppBar(R.id.toolbar, R.string.title_subreddit_settings, true, true)
        recycler = findViewById<View>(R.id.subslist) as RecyclerView
        recycler!!.layoutManager = LinearLayoutManager(this)
        reloadSubList()
        findViewById<View>(R.id.reset).setOnClickListener {
            AlertDialog.Builder(this@SettingsSubreddit)
                .setTitle(R.string.clear_all_sub_themes)
                .setMessage(R.string.clear_all_sub_themes_msg)
                .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                    for (s in changedSubs) {
                        Palette.removeColor(s)
                        setLayoutSettings(s, false)
                        ColorPreferences(this@SettingsSubreddit).removeFontStyle(s)
                        resetPicsEnabled(s)
                    }
                    reloadSubList()
                }
                .setNegativeButton(R.string.btn_no, null)
                .show()
        }
        findViewById<View>(R.id.post_floating_action_button).setOnClickListener {
            val subs: ArrayList<String> = sort(getSubscriptions(this@SettingsSubreddit))
            val subsAsChar = subs.toTypedArray<CharSequence>()
            val builder = MaterialDialog(this@SettingsSubreddit)
            builder.title(R.string.dialog_choose_subreddits_to_edit)
                .listItemsMultiChoice(items = subsAsChar.toList()) { dialog: MaterialDialog, which: IntArray, text: List<CharSequence> ->
                    val selectedSubs = ArrayList<String>()
                    for (i in which) {
                        selectedSubs.add(subsAsChar[i].toString())
                    }
                    if (mSettingsSubAdapter != null) mSettingsSubAdapter!!.prepareAndShowSubEditor(
                        selectedSubs
                    )
                    return@listItemsMultiChoice
                }
                .positiveButton(R.string.btn_select)
                .negativeButton(R.string.btn_cancel)
                .show()
        }
        findViewById<View>(R.id.color).setOnClickListener {
            if (Authentication.isLoggedIn) {
                AlertDialog.Builder(this@SettingsSubreddit)
                    .setTitle(R.string.dialog_color_sync_title)
                    .setMessage(R.string.dialog_color_sync_message)
                    .setPositiveButton(R.string.misc_continue) { dialog: DialogInterface?, which: Int ->
                        val d: MaterialDialog = MaterialDialog(this@SettingsSubreddit)
                            .title(R.string.general_sub_sync)
                            .message(R.string.misc_please_wait)
                            //.progress(false, 100)
                            .cancelable(false)
                            .also { it.show() }
                        object : AsyncTask<Void?, Void?, Void>() {
                            override fun doInBackground(vararg params: Void?): Void? {
                                val subColors = syncSubredditsGetObject()
                                //d.setMaxProgress(subColors.size)
                                var i = 0
                                done = 0
                                for (s in subColors) {
                                    if (s.dataNode.has("key_color") && s.dataNode["key_color"].asText().isNotEmpty()
                                        && Palette.getColor(s.displayName.lowercase()) == Palette.getDefaultColor()
                                    ) {
                                        Palette.setColor(
                                            s.displayName.lowercase(),
                                            GetClosestColor.getClosestColor(
                                                s.dataNode["key_color"].asText(),
                                                this@SettingsSubreddit
                                            )
                                        )
                                        done++
                                    }
                                    //d.setProgress(i)
                                    i++
                                    /*
                                    if (i == d.getMaxProgress()) {
                                        d.dismiss()
                                    }*/
                                }
                                return null
                            }

                            override fun onPostExecute(aVoid: Void) {
                                reloadSubList()
                                val res = resources
                                AlertDialog.Builder(this@SettingsSubreddit)
                                    .setTitle(R.string.color_sync_complete)
                                    .setMessage(
                                        res.getQuantityString(
                                            R.plurals.color_sync_colored,
                                            done,
                                            done
                                        )
                                    )
                                    .setPositiveButton(R.string.btn_ok, null)
                                    .show()
                            }
                        }.execute()
                        d.show()
                    }.setNegativeButton(R.string.btn_cancel, null).show()
            } else {
                val s =
                    Snackbar.make(mToolbar!!, R.string.err_color_sync_login, Snackbar.LENGTH_SHORT)
                LayoutUtils.showSnackbar(s)
            }
        }
    }

    fun reloadSubList() {
        changedSubs.clear()
        val allSubs: List<String> = sort(getAllUserSubreddits(this))

        // Check which subreddits are different
        val colorPrefs = ColorPreferences(this@SettingsSubreddit)
        val defaultFont = colorPrefs.fontStyle.color
        for (s in allSubs) {
            if (Palette.getColor(s) != Palette.getDefaultColor() || getLayoutSettings(s)!! || colorPrefs.getFontStyleSubreddit(
                    s
                ).color != defaultFont || hasPicsEnabled(s)
            ) {
                changedSubs.add(s)
            }
        }
        mSettingsSubAdapter = SettingsSubAdapter(this, changedSubs)
        recycler!!.adapter = mSettingsSubAdapter
        val fab = findViewById<View>(R.id.post_floating_action_button) as FloatingActionButton
        recycler!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 && fab.id != 0) {
                    fab.show()
                } else {
                    fab.hide()
                }
            }
        })
        fab.show()
    }
}
