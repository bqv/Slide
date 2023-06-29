package me.ccrama.redditslide.ui.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.SubmissionCache
import me.ccrama.redditslide.views.CreateCardView
import me.ccrama.redditslide.views.CreateCardView.CreateView
import me.ccrama.redditslide.views.CreateCardView.isCard
import me.ccrama.redditslide.views.CreateCardView.isDesktop
import me.ccrama.redditslide.views.CreateCardView.isMiddle
import me.ccrama.redditslide.views.CreateCardView.setActionbarVisible
import me.ccrama.redditslide.views.CreateCardView.setBigPicCropped
import me.ccrama.redditslide.views.CreateCardView.setBigPicEnabled
import me.ccrama.redditslide.views.CreateCardView.setCardViewType
import me.ccrama.redditslide.views.CreateCardView.setMiddleCard
import me.ccrama.redditslide.views.CreateCardView.setNoThumbnails
import me.ccrama.redditslide.views.CreateCardView.setSmallTag
import me.ccrama.redditslide.views.CreateCardView.setSwitchThumb

class EditCardsLayout : BaseActivityAnim() {
    public override fun onCreate(savedInstance: Bundle?) {
        overrideRedditSwipeAnywhere()
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstance)
        applyColorTheme()
        setContentView(R.layout.activity_settings_theme_card)
        setupAppBar(R.id.toolbar, R.string.settings_layout_default, true, true)
        val layout = findViewById<View>(R.id.card) as LinearLayout
        layout.removeAllViews()
        layout.addView(CreateView(layout))

        //View type//
        //Cards or List//
        (findViewById<View>(R.id.view_current) as TextView).text =
            if (isCard) (if (isMiddle) getString(R.string.mode_centered) else getString(R.string.mode_card)) else if (isDesktop) getString(
                R.string.mode_desktop_compact
            ) else getString(R.string.mode_list)
        findViewById<View>(R.id.view).setOnClickListener { v ->
            val popup = PopupMenu(this@EditCardsLayout, v)
            popup.menuInflater.inflate(R.menu.card_mode_settings, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.center -> {
                        layout.removeAllViews()
                        layout.addView(setMiddleCard(true, layout))
                    }

                    R.id.card -> {
                        layout.removeAllViews()
                        layout.addView(setCardViewType(CreateCardView.CardEnum.LARGE, layout))
                    }

                    R.id.list -> {
                        layout.removeAllViews()
                        layout.addView(setCardViewType(CreateCardView.CardEnum.LIST, layout))
                    }

                    R.id.desktop -> {
                        layout.removeAllViews()
                        layout.addView(setCardViewType(CreateCardView.CardEnum.DESKTOP, layout))
                    }
                }
                (findViewById<View>(R.id.view_current) as TextView).text =
                    if (isCard) (if (isMiddle) getString(R.string.mode_centered) else getString(R.string.mode_card)) else if (isDesktop) getString(
                        R.string.mode_desktop_compact
                    ) else getString(R.string.mode_list)
                true
            }
            popup.show()
        }
        run {
            val single = findViewById<View>(R.id.commentlast) as SwitchCompat
            single.isChecked = SettingValues.commentLastVisit
            single.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.commentLastVisit = isChecked
            }
        }
        run {
            val single = findViewById<View>(R.id.domain) as SwitchCompat
            single.isChecked = SettingValues.showDomain
            single.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.showDomain = isChecked
            }
        }
        run {
            val single2 = findViewById<View>(R.id.selftextcomment) as SwitchCompat
            single2.isChecked = SettingValues.hideSelftextLeadImage
            single2.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.hideSelftextLeadImage = isChecked
            }
        }
        run {
            val single2 = findViewById<View>(R.id.abbreviateScores) as SwitchCompat
            single2.isChecked = SettingValues.abbreviateScores
            single2.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.abbreviateScores = isChecked
            }
        }
        run {
            val single2 = findViewById<View>(R.id.hidePostAwards) as SwitchCompat
            single2.isChecked = SettingValues.hidePostAwards
            single2.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.hidePostAwards = isChecked
            }
        }
        run {
            val single2 = findViewById<View>(R.id.titleTop) as SwitchCompat
            single2.isChecked = SettingValues.titleTop
            single2.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.titleTop = isChecked
            }
        }
        run {
            val single = findViewById<View>(R.id.votes) as SwitchCompat
            single.isChecked = SettingValues.votesInfoLine
            single.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.votesInfoLine = isChecked
                SubmissionCache.evictAll()
            }
        }
        run {
            val single = findViewById<View>(R.id.contenttype) as SwitchCompat
            single.isChecked = SettingValues.typeInfoLine
            single.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.typeInfoLine = isChecked
                SubmissionCache.evictAll()
            }
        }
        run {
            val single = findViewById<View>(R.id.selftext) as SwitchCompat
            single.isChecked = SettingValues.cardText
            single.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.cardText = isChecked
            }
        }
        //Pic modes//
        //it won't be
        val CURRENT_PICTURE = (findViewById<View>(R.id.picture_current) as TextView)
        if (SettingValues.bigPicCropped) {
            CURRENT_PICTURE.setText(R.string.mode_cropped)
        } else if (SettingValues.bigPicEnabled) {
            CURRENT_PICTURE.setText(R.string.mode_bigpic)
        } else if (SettingValues.noThumbnails) {
            CURRENT_PICTURE.setText(R.string.mode_no_thumbnails)
        } else {
            CURRENT_PICTURE.setText(R.string.mode_thumbnail)
        }
        findViewById<View>(R.id.picture).setOnClickListener { v ->
            val popup = PopupMenu(this@EditCardsLayout, v)
            popup.menuInflater.inflate(R.menu.pic_mode_settings, popup.menu)
            popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    when (item.itemId) {
                        R.id.bigpic -> {
                            layout.removeAllViews()
                            layout.addView(setBigPicEnabled(true, layout))
                            run {
                                SettingValues.resetPicsEnabledAll()
                            }
                        }

                        R.id.cropped -> {
                            layout.removeAllViews()
                            layout.addView(setBigPicCropped(true, layout))
                        }

                        R.id.thumbnail -> {
                            layout.removeAllViews()
                            layout.addView(setBigPicEnabled(false, layout))
                            run {
                                SettingValues.resetPicsEnabledAll()
                            }
                        }

                        R.id.noThumbnails -> {
                            layout.removeAllViews()
                            layout.addView(setNoThumbnails(true, layout))
                            run {
                                SettingValues.resetPicsEnabledAll()
                            }
                        }
                    }
                    if (SettingValues.bigPicCropped) {
                        CURRENT_PICTURE.setText(R.string.mode_cropped)
                    } else if (SettingValues.bigPicEnabled) {
                        CURRENT_PICTURE.setText(R.string.mode_bigpic)
                    } else if (SettingValues.noThumbnails) {
                        CURRENT_PICTURE.setText(R.string.mode_no_thumbnails)
                    } else {
                        CURRENT_PICTURE.setText(R.string.mode_thumbnail)
                    }
                    return true
                }
            })
            popup.show()
        }
        if (!SettingValues.noThumbnails) {
            //def won't be null
            val bigThumbnails = (findViewById<View>(R.id.bigThumbnails) as SwitchCompat)
            bigThumbnails.isChecked = SettingValues.bigThumbnails
            bigThumbnails.setOnCheckedChangeListener(object :
                CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                    SettingValues.bigThumbnails = isChecked
                    if (!SettingValues.bigPicCropped) {
                        layout.removeAllViews()
                        layout.addView(setBigPicEnabled(false, layout))
                        run {
                            SettingValues.resetPicsEnabledAll()
                        }
                    }
                }
            })
        }

        //Actionbar//
        (findViewById<View>(R.id.actionbar_current) as TextView).text =
            if (!SettingValues.actionbarVisible) (if (SettingValues.actionbarTap) getString(R.string.tap_actionbar) else getString(
                R.string.press_actionbar
            )) else getString(R.string.always_actionbar)
        findViewById<View>(R.id.actionbar).setOnClickListener { v ->
            val popup = PopupMenu(this@EditCardsLayout, v)
            popup.menuInflater.inflate(R.menu.actionbar_mode, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.always -> {
                        SettingValues.actionbarTap = false
                        layout.removeAllViews()
                        layout.addView(setActionbarVisible(true, layout))
                    }

                    R.id.tap -> {
                        SettingValues.actionbarTap = true
                        layout.removeAllViews()
                        layout.addView(setActionbarVisible(false, layout))
                    }

                    R.id.button -> {
                        SettingValues.actionbarTap = false
                        layout.removeAllViews()
                        layout.addView(setActionbarVisible(false, layout))
                    }
                }
                (findViewById<View>(R.id.actionbar_current) as TextView).text =
                    if (!SettingValues.actionbarVisible) (if (SettingValues.actionbarTap) getString(
                        R.string.tap_actionbar
                    ) else getString(R.string.press_actionbar)) else getString(R.string.always_actionbar)
                true
            }
            popup.show()
        }


        //Other buttons//
        val hidebutton = findViewById<View>(R.id.hidebutton) as AppCompatCheckBox
        layout.findViewById<View>(R.id.hide).visibility =
            if (SettingValues.hideButton && SettingValues.actionbarVisible) View.VISIBLE else View.GONE
        layout.findViewById<View>(R.id.save).visibility =
            if (SettingValues.saveButton && SettingValues.actionbarVisible) View.VISIBLE else View.GONE
        hidebutton.isChecked = SettingValues.hideButton
        hidebutton.setOnCheckedChangeListener { buttonView, isChecked ->
            SettingValues.hideButton = isChecked
            layout.findViewById<View>(R.id.hide).visibility =
                if (SettingValues.hideButton && SettingValues.actionbarVisible) View.VISIBLE else View.GONE
            layout.findViewById<View>(R.id.save).visibility =
                if (SettingValues.saveButton && SettingValues.actionbarVisible) View.VISIBLE else View.GONE
        }
        val savebutton = findViewById<View>(R.id.savebutton) as AppCompatCheckBox
        layout.findViewById<View>(R.id.save).visibility =
            if (SettingValues.saveButton && SettingValues.actionbarVisible) View.VISIBLE else View.GONE
        savebutton.isChecked = SettingValues.saveButton
        savebutton.setOnCheckedChangeListener { buttonView, isChecked ->
            SettingValues.saveButton = isChecked
            layout.findViewById<View>(R.id.hide).visibility =
                if (SettingValues.hideButton && SettingValues.actionbarVisible) View.VISIBLE else View.GONE
            layout.findViewById<View>(R.id.save).visibility =
                if (SettingValues.saveButton && SettingValues.actionbarVisible) View.VISIBLE else View.GONE
        }

        //Smaller tags//
        val smallTag = findViewById<View>(R.id.tagsetting) as SwitchCompat
        smallTag.isChecked = SettingValues.smallTag
        smallTag.setOnCheckedChangeListener { buttonView, isChecked ->
            layout.removeAllViews()
            layout.addView(setSmallTag(isChecked, layout))
        }


        //Actionbar//
        //Enable, collapse//
        val switchThumb = findViewById<View>(R.id.action) as SwitchCompat
        switchThumb.isChecked = SettingValues.switchThumb
        switchThumb.setOnCheckedChangeListener { buttonView, isChecked ->
            layout.removeAllViews()
            layout.addView(setSwitchThumb(isChecked, layout))
        }
    }
}
