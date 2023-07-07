package me.ccrama.redditslide.Fragments

import android.view.View
import android.widget.CheckBox
import androidx.annotation.IdRes
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.ui.settings.SettingsThemeFragment

// TODO: Replace all this $#!^ with a bitset...
private var selectedDrawerItems: Long = 0

class DrawerItemsDialog(val dialog: MaterialDialog) {
    init {
        dialog.apply {
            customView(R.layout.dialog_drawer_items, scrollable = false)
            title(R.string.settings_general_title_drawer_items)
            positiveButton(android.R.string.ok) { dialog ->
                if (SettingsThemeFragment.changed) {
                    SettingValues.selectedDrawerItems = selectedDrawerItems
                }
            }
            cancelOnTouchOutside(false)
            setOnShowListener {
                selectedDrawerItems = SettingValues.selectedDrawerItems
                if (selectedDrawerItems == -1L) {
                    selectedDrawerItems = 0
                    for (settingDrawerItem in SettingsDrawerEnum.values()) {
                        selectedDrawerItems += settingDrawerItem.value
                    }
                    SettingValues.selectedDrawerItems = selectedDrawerItems
                }
                setupViews()
            }
            setOnDismissListener {
                SettingValues.selectedDrawerItems = selectedDrawerItems
            }
            setOnCancelListener {
                SettingValues.selectedDrawerItems = selectedDrawerItems
            }
        }
    }

    private fun setupViews() {
        for (settingDrawerItem in SettingsDrawerEnum.values()) {
            dialog.findViewById<View>(settingDrawerItem.layoutId).setOnClickListener {
                val checkBox = dialog.findViewById<CheckBox>(settingDrawerItem.checkboxId)
                if (checkBox.isChecked) {
                    selectedDrawerItems -= settingDrawerItem.value
                } else {
                    selectedDrawerItems += settingDrawerItem.value
                }
                checkBox.isChecked = !checkBox.isChecked
            }
            SettingsThemeFragment.changed = true
            (dialog.findViewById<CheckBox>(settingDrawerItem.checkboxId)).isChecked =
                selectedDrawerItems and settingDrawerItem.value != 0L
        }
    }

    fun show() {
        dialog.show()
    }

    inline fun show(crossinline func: MaterialDialog.() -> Unit) {
        dialog.show(func)
    }

    enum class SettingsDrawerEnum(
        var value: Long,
        @field:IdRes @param:IdRes var layoutId: Int,
        @field:IdRes @param:IdRes var checkboxId: Int,
        @field:IdRes @param:IdRes var drawerId: Int
    ) {
        PROFILE(
            1 shl 0,
            R.id.settings_drawer_profile,
            R.id.settings_drawer_profile_checkbox,
            R.id.prof_click
        ),
        INBOX(
            1 shl 1,
            R.id.settings_drawer_inbox,
            R.id.settings_drawer_inbox_checkbox,
            R.id.inbox
        ),
        MULTIREDDITS(
            1 shl 2,
            R.id.settings_drawer_multireddits,
            R.id.settings_drawer_multireddits_checkbox,
            R.id.multi
        ),
        GOTO_PROFILE(
            1 shl 3,
            R.id.settings_drawer_goto_profile,
            R.id.settings_drawer_goto_profile_checkbox,
            R.id.prof
        ),
        DISCOVER(
            1 shl 4,
            R.id.settings_drawer_discover,
            R.id.settings_drawer_discover_checkbox,
            R.id.discover
        );
    }
}
