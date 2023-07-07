package me.ccrama.redditslide.ui.settings

import android.app.Activity
import android.content.DialogInterface
import android.os.AsyncTask
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onShow
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.RemovalReasonType
import ltd.ucode.slide.SettingValues.ToolboxRemovalMessageType
import ltd.ucode.slide.SettingValues.editBoolean
import ltd.ucode.slide.SettingValues.editInt
import me.ccrama.redditslide.Toolbox.Toolbox
import me.ccrama.redditslide.UserSubscriptions
import net.dean.jraw.http.NetworkException

class SettingsModerationFragment(private val context: Activity) {
    fun Bind() {
        val removalReasonsLayout =
            context.findViewById<RelativeLayout>(R.id.settings_moderation_removal_reasons)
        val removalReasonsCurrentView =
            context.findViewById<TextView>(R.id.settings_moderation_removal_reasons_current)
        val enableToolboxSwitch =
            context.findViewById<SwitchCompat>(R.id.settings_moderation_toolbox_enabled)
        val removalMessageLayout =
            context.findViewById<RelativeLayout>(R.id.settings_moderation_toolbox_message)
        val removalMessageCurrentView =
            context.findViewById<TextView>(R.id.settings_moderation_toolbox_message_current)
        val sendMsgAsSubredditSwitch =
            context.findViewById<SwitchCompat>(R.id.settings_moderation_toolbox_sendMsgAsSubreddit)
        val stickyMessageSwitch =
            context.findViewById<SwitchCompat>(R.id.settings_moderation_toolbox_sticky)
        val lockAfterRemovalSwitch =
            context.findViewById<SwitchCompat>(R.id.settings_moderation_toolbox_lock)
        val refreshLayout =
            context.findViewById<RelativeLayout>(R.id.settings_moderation_toolbox_refresh)

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//* General (Moderation) */
        // Set up removal reason setting
        if (SettingValues.removalReasonType == RemovalReasonType.SLIDE.ordinal) {
            removalReasonsCurrentView.text = context.getString(R.string.settings_mod_removal_slide)
        } else if (SettingValues.removalReasonType == RemovalReasonType.TOOLBOX.ordinal) {
            removalReasonsCurrentView.text =
                context.getString(R.string.settings_mod_removal_toolbox)
        } else {
            removalReasonsCurrentView.text = context.getString(R.string.settings_mod_removal_reddit)
        }
        removalReasonsLayout.setOnClickListener { v: View? ->
            val popupMenu = PopupMenu(
                context, v
            )
            popupMenu.menuInflater.inflate(R.menu.removal_reason_setings, popupMenu.menu)
            popupMenu.menu.findItem(R.id.toolbox).isEnabled = SettingValues.toolboxEnabled
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.slide -> setModRemovalReasonType(
                        removalReasonsCurrentView,
                        RemovalReasonType.SLIDE.ordinal, R.string.settings_mod_removal_slide
                    )

                    R.id.toolbox -> setModRemovalReasonType(
                        removalReasonsCurrentView,
                        RemovalReasonType.TOOLBOX.ordinal, R.string.settings_mod_removal_toolbox
                    )
                }
                true
            }
            popupMenu.show()
        }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//* Toolbox */
        // Set up toolbox enabled switch
        enableToolboxSwitch.isChecked = SettingValues.toolboxEnabled
        enableToolboxSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            SettingValues.toolboxEnabled = isChecked
            editBoolean(SettingValues.PREF_MOD_TOOLBOX_ENABLED, isChecked)
            removalMessageLayout.isEnabled = isChecked
            sendMsgAsSubredditSwitch.isEnabled = isChecked
            stickyMessageSwitch.isEnabled = isChecked
            lockAfterRemovalSwitch.isEnabled = isChecked
            refreshLayout.isEnabled = isChecked
            if (!isChecked) {
                setModRemovalReasonType(
                    removalReasonsCurrentView,
                    RemovalReasonType.SLIDE.ordinal, R.string.settings_mod_removal_slide
                )
            }

            // download and cache toolbox stuff in the background unless it's already loaded
            for (sub in UserSubscriptions.modOf!!) {
                Toolbox.ensureConfigCachedLoaded(sub, false)
                Toolbox.ensureUsernotesCachedLoaded(sub, false)
            }
        }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Set up toolbox default removal type
        removalMessageLayout.isEnabled = SettingValues.toolboxEnabled
        if (SettingValues.toolboxMessageType == ToolboxRemovalMessageType.COMMENT.ordinal) {
            removalMessageCurrentView.text = context.getString(R.string.toolbox_removal_comment)
        } else if (SettingValues.toolboxMessageType == ToolboxRemovalMessageType.PM.ordinal) {
            removalMessageCurrentView.text = context.getString(R.string.toolbox_removal_pm)
        } else if (SettingValues.toolboxMessageType == ToolboxRemovalMessageType.BOTH.ordinal) {
            removalMessageCurrentView.text = context.getString(R.string.toolbox_removal_both)
        } else {
            removalMessageCurrentView.text = context.getString(R.string.toolbox_removal_none)
        }
        removalMessageLayout.setOnClickListener { v: View? ->
            val popupMenu = PopupMenu(
                context, v
            )
            popupMenu.menuInflater.inflate(R.menu.settings_toolbox_message, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.comment -> setToolboxRemovalMessageType(
                        removalMessageCurrentView,
                        ToolboxRemovalMessageType.COMMENT.ordinal, R.string.toolbox_removal_comment
                    )

                    R.id.pm -> setToolboxRemovalMessageType(
                        removalMessageCurrentView,
                        ToolboxRemovalMessageType.PM.ordinal, R.string.toolbox_removal_pm
                    )

                    R.id.both -> setToolboxRemovalMessageType(
                        removalMessageCurrentView,
                        ToolboxRemovalMessageType.BOTH.ordinal, R.string.toolbox_removal_both
                    )

                    R.id.none -> setToolboxRemovalMessageType(
                        removalMessageCurrentView,
                        ToolboxRemovalMessageType.NONE.ordinal, R.string.toolbox_removal_none
                    )
                }
                true
            }
            popupMenu.show()
        }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Set up modmail switch
        sendMsgAsSubredditSwitch.isEnabled = SettingValues.toolboxEnabled
        sendMsgAsSubredditSwitch.isChecked = SettingValues.toolboxModmail
        sendMsgAsSubredditSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            SettingValues.toolboxModmail = isChecked
            editBoolean(SettingValues.PREF_MOD_TOOLBOX_MODMAIL, isChecked)
        }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Set up sticky switch
        stickyMessageSwitch.isEnabled = SettingValues.toolboxEnabled
        stickyMessageSwitch.isChecked = SettingValues.toolboxSticky
        stickyMessageSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            SettingValues.toolboxSticky = isChecked
            editBoolean(SettingValues.PREF_MOD_TOOLBOX_STICKY, isChecked)
        }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Set up lock switch
        lockAfterRemovalSwitch.isEnabled = SettingValues.toolboxEnabled
        lockAfterRemovalSwitch.isChecked = SettingValues.toolboxLock
        lockAfterRemovalSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            SettingValues.toolboxLock = isChecked
            editBoolean(SettingValues.PREF_MOD_TOOLBOX_LOCK, isChecked)
        }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Set up force refresh button
        refreshLayout.isEnabled = SettingValues.toolboxEnabled
        refreshLayout.setOnClickListener { v: View? ->
            MaterialDialog(context)
                .message(R.string.settings_mod_toolbox_refreshing)
                //.progress(false, UserSubscriptions.modOf!!.size * 2)
                .onShow { dialog ->
                    AsyncRefreshToolboxTask(dialog).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR
                    )
                }
                .cancelable(false)
                .show()
        }
    }

    private fun setToolboxRemovalMessageType(textView: TextView, enumOrdinal: Int, string: Int) {
        SettingValues.toolboxMessageType = enumOrdinal
        setBaseModerationType(textView, SettingValues.toolboxMessageType, string)
    }

    private fun setModRemovalReasonType(textView: TextView, enumOrdinal: Int, string: Int) {
        SettingValues.removalReasonType = enumOrdinal
        setBaseModerationType(textView, SettingValues.removalReasonType, string)
    }

    private fun setBaseModerationType(textView: TextView, moderationType: Int, string: Int) {
        editInt(SettingValues.PREF_MOD_REMOVAL_TYPE, moderationType)
        textView.text = context.getString(string)
    }

    private class AsyncRefreshToolboxTask constructor(dialog: DialogInterface) :
        AsyncTask<Void?, Void?, Void?>() {
        val dialog: MaterialDialog

        init {
            this.dialog = dialog as MaterialDialog
        }

        override fun doInBackground(vararg voids: Void?): Void? {
            for (sub in UserSubscriptions.modOf!!) {
                try {
                    Toolbox.downloadToolboxConfig(sub)
                } catch (ignored: NetworkException) {
                }
                publishProgress()
                try {
                    Toolbox.downloadUsernotes(sub)
                } catch (ignored: NetworkException) {
                }
                publishProgress()
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            dialog.dismiss()
        }

        override fun onProgressUpdate(vararg voids: Void?) {
            //dialog.incrementProgress(1)
        }
    }
}
