package me.ccrama.redditslide.ui.settings

import android.app.Activity
import android.os.Build
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import ltd.ucode.slide.App
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.util.LinkUtil

class SettingsHandlingFragment(private val context: Activity) :
    CompoundButton.OnCheckedChangeListener {
    var domainListLayout: LinearLayout? = null
    fun Bind() {
        //todo web stuff
        val shortlink = context.findViewById<SwitchCompat>(R.id.settings_handling_shortlink)
        val gif = context.findViewById<SwitchCompat>(R.id.settings_handling_gif)
        val hqgif = context.findViewById<SwitchCompat>(R.id.settings_handling_hqgif)
        val image = context.findViewById<SwitchCompat>(R.id.settings_handling_image)
        val album = context.findViewById<SwitchCompat>(R.id.settings_handling_album)
        val peek = context.findViewById<SwitchCompat>(R.id.settings_handling_peek)
        shortlink.isChecked = !SettingValues.shareLongLink
        gif.isChecked = SettingValues.gif
        hqgif.isChecked = SettingValues.hqgif
        image.isChecked = SettingValues.image
        album.isChecked = SettingValues.album
        peek.isChecked = SettingValues.peek
        shortlink.setOnCheckedChangeListener(this)
        gif.setOnCheckedChangeListener(this)
        hqgif.setOnCheckedChangeListener(this)
        image.setOnCheckedChangeListener(this)
        album.setOnCheckedChangeListener(this)
        peek.setOnCheckedChangeListener(this)
        val readerMode = context.findViewById<SwitchCompat>(R.id.settings_handling_reader_mode)
        val readernight = context.findViewById<SwitchCompat>(R.id.settings_handling_readernight)
        val handlingVideoLayout = context.findViewById<RelativeLayout>(R.id.settings_handling_video)
        domainListLayout = context.findViewById(R.id.settings_handling_domainlist)
        val domainListEditText = context.findViewById<EditText>(R.id.settings_handling_domain_edit)

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//* Browser */
        setUpBrowserLinkHandling()
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        readerMode.isChecked = SettingValues.readerMode
        readerMode.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            SettingValues.readerMode = isChecked
            SettingValues.editBoolean(SettingValues.PREF_READER_MODE, SettingValues.readerMode)
            readernight.isEnabled =
                SettingValues.NightModeState.isEnabled && SettingValues.readerMode
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        readernight.isEnabled = SettingValues.NightModeState.isEnabled && SettingValues.readerMode
        readernight.isChecked = SettingValues.readerNight
        readernight.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            SettingValues.readerNight = isChecked
            SettingValues.editBoolean(SettingValues.PREF_READER_NIGHT, isChecked)
        }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        if (!App.videoPlugin) {
            handlingVideoLayout.setOnClickListener { v: View? ->
                LinkUtil.launchMarketUri(
                    context, R.string.youtube_plugin_package
                )
            }
        } else {
            handlingVideoLayout.visibility = View.GONE
        }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        /* activity_settings_handling_child.xml does not load these elements so we need to null check */if ((domainListEditText != null) and (domainListLayout != null)) {
            domainListEditText!!.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    SettingValues.alwaysExternal +=
                        domainListEditText.text.toString().lowercase().trim { it <= ' ' }
                    domainListEditText.setText("")
                    updateFilters()
                }
                false
            }
            updateFilters()
        }
    }

    private fun setUpBrowserLinkHandling() {
        val browserTypeRadioGroup =
            context.findViewById<RadioGroup>(R.id.settings_handling_select_browser_type)
        val selectBrowserLayout =
            context.findViewById<RelativeLayout>(R.id.settings_handling_select_browser_layout)
        val webBrowserView = context.findViewById<TextView>(R.id.settings_handling_browser)
        browserTypeRadioGroup.check(LinkHandlingMode.idResFromValue(SettingValues.linkHandlingMode))
        browserTypeRadioGroup.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            SettingValues.linkHandlingMode = LinkHandlingMode.valueFromIdRes(checkedId)
        }
        val installedBrowsers: HashMap<String, String> = App.Companion.installedBrowsers
        if (!installedBrowsers.containsKey(SettingValues.selectedBrowser)) {
            SettingValues.selectedBrowser = ""
        }
        webBrowserView.text = installedBrowsers[SettingValues.selectedBrowser]
        if (installedBrowsers.size <= 1) {
            selectBrowserLayout.visibility = View.GONE
        } else {
            selectBrowserLayout.visibility = View.VISIBLE
            selectBrowserLayout.setOnClickListener { v: View? ->
                val popupMenu = PopupMenu(
                    context, v!!
                )
                val packageNames = HashMap<MenuItem, String?>()
                for ((key, value) in installedBrowsers) {
                    val menuItem = popupMenu.menu.add(value)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        menuItem.tooltipText = key
                    }
                    packageNames[menuItem] = key
                }
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    SettingValues.selectedBrowser = packageNames[item]!!
                    webBrowserView.text = item.title
                    true
                }
                popupMenu.show()
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView.id) {
            R.id.settings_handling_shortlink -> {
                SettingValues.shareLongLink = !isChecked
                SettingValues.editBoolean(SettingValues.PREF_LONG_LINK, !isChecked)
            }

            R.id.settings_handling_gif -> {
                SettingValues.gif = isChecked
                SettingValues.editBoolean(SettingValues.PREF_GIF, isChecked)
            }

            R.id.settings_handling_hqgif -> {
                SettingValues.hqgif = isChecked
                SettingValues.editBoolean(SettingValues.PREF_HQGIF, isChecked)
            }

            R.id.settings_handling_image -> {
                SettingValues.image = isChecked
                SettingValues.editBoolean(SettingValues.PREF_IMAGE, isChecked)
            }

            R.id.settings_handling_album -> {
                SettingValues.album = isChecked
                SettingValues.editBoolean(SettingValues.PREF_ALBUM, isChecked)
            }

            R.id.settings_handling_peek -> {
                SettingValues.peek = isChecked
                SettingValues.editBoolean(SettingValues.PREF_PEEK, isChecked)
            }
        }
    }

    private fun updateFilters() {
        domainListLayout!!.removeAllViews()
        for (s in SettingValues.alwaysExternal) {
            if (!s.isEmpty() && (!App.videoPlugin || !s.contains("youtube.co") && !s.contains("youtu.be"))) {
                val t = context.layoutInflater.inflate(
                    R.layout.account_textview,
                    domainListLayout, false
                )
                val accountTextViewName = t.findViewById<TextView>(R.id.name)
                val accountTextViewRemove = t.findViewById<ImageView>(R.id.remove)
                accountTextViewName.text = s
                accountTextViewRemove.setOnClickListener { v: View? ->
                    SettingValues.alwaysExternal -= s
                    updateFilters()
                }
                domainListLayout!!.addView(t)
            }
        }
    }

    enum class LinkHandlingMode(val value: Int, @field:IdRes @param:IdRes val idRes: Int) {
        EXTERNAL(0, R.id.settings_handling_browser_type_external_browser), INTERNAL(
            1,
            R.id.settings_handling_browser_type_internal_browser
        ),
        CUSTOM_TABS(2, R.id.settings_handling_browser_type_custom_tabs);

        companion object {
            private val sBiMap: BiMap<Int, Int> =
                HashBiMap.create<Int, Int>(object : HashMap<Int, Int>() {
                    init {
                        put(EXTERNAL.value, EXTERNAL.idRes)
                        put(INTERNAL.value, INTERNAL.idRes)
                        put(CUSTOM_TABS.value, CUSTOM_TABS.idRes)
                    }
                })

            fun idResFromValue(value: Int): Int {
                return sBiMap[value]!!
            }

            fun valueFromIdRes(@IdRes idRes: Int): Int {
                return sBiMap.inverse()[idRes]!!
            }
        }
    }
}
