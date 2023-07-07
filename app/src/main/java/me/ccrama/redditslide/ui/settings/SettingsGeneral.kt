package me.ccrama.redditslide.ui.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.ui.settings.SettingsGeneralFragment.Companion.doNotifText
import java.io.File

class SettingsGeneral : BaseActivityAnim() {
    private val fragment: SettingsGeneralFragment = SettingsGeneralFragment(this)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_settings_general)
        setupAppBar(R.id.toolbar, R.string.settings_title_general, true, true)
        (findViewById<View>(R.id.settings_general) as ViewGroup).addView(
            layoutInflater.inflate(R.layout.activity_settings_general_child, null)
        )
        fragment.Bind()
    }

    fun onFolderSelection(folder: File) {
        fragment.onFolderSelection(folder)
    }

    override fun onResume() {
        super.onResume()
        doNotifText(this)
    }
}
