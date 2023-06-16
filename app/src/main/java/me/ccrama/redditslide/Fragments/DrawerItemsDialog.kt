package me.ccrama.redditslide.Fragments

import android.os.Bundle
import android.widget.CheckBox
import androidx.annotation.IdRes
import com.afollestad.materialdialogs.MaterialDialog
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.ui.settings.SettingsThemeFragment

// TODO: Replace all this $#!^ with a bitset...
private var selectedDrawerItems: Long = 0

class DrawerItemsDialog(builder: Builder) :
    MaterialDialog(builder.customView(R.layout.dialog_drawer_items, false)
        .title(R.string.settings_general_title_drawer_items)
        .positiveText(android.R.string.ok)
        .canceledOnTouchOutside(false)
        .onPositive { dialog, which ->
            if (SettingsThemeFragment.changed) {
                SettingValues.selectedDrawerItems = selectedDrawerItems
            }
        }) {

    public override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
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

    public override fun onStop() {
        super.onStop()
        SettingValues.selectedDrawerItems = selectedDrawerItems
    }

    private fun setupViews() {
        for (settingDrawerItem in SettingsDrawerEnum.values()) {
            findViewById(settingDrawerItem.layoutId).setOnClickListener {
                val checkBox = findViewById(settingDrawerItem.checkboxId) as CheckBox
                if (checkBox.isChecked) {
                    selectedDrawerItems -= settingDrawerItem.value
                } else {
                    selectedDrawerItems += settingDrawerItem.value
                }
                checkBox.isChecked = !checkBox.isChecked
            }
            SettingsThemeFragment.changed = true
            (findViewById(settingDrawerItem.checkboxId) as CheckBox).isChecked =
                selectedDrawerItems and settingDrawerItem.value != 0L
        }
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
