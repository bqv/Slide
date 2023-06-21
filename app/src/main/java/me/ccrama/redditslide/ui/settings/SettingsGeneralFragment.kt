package me.ccrama.redditslide.ui.settings

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.os.AsyncTask
import android.os.Build
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.InputCallback
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback
import com.google.android.material.snackbar.Snackbar
import com.rey.material.widget.Slider
import ltd.ucode.slide.App
import ltd.ucode.slide.App.Companion.isPackageInstalled
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Constants.BackButtonBehaviorOptions
import me.ccrama.redditslide.Fragments.DrawerItemsDialog
import me.ccrama.redditslide.Fragments.FolderChooserDialogCreate
import me.ccrama.redditslide.Notifications.CheckForMail
import me.ccrama.redditslide.Notifications.NotificationJobScheduler
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.DialogUtil
import me.ccrama.redditslide.util.ImageLoaderUtils
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.util.SortingUtil
import me.ccrama.redditslide.util.StringUtil
import me.ccrama.redditslide.util.TimeUtils
import net.dean.jraw.models.CommentSort
import net.dean.jraw.models.Subreddit
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod
import java.io.File
import java.util.Arrays
import java.util.Locale

class SettingsGeneralFragment<ActivityType>(private val context: ActivityType) :
    FolderChooserDialogCreate.FolderCallback where ActivityType : AppCompatActivity?, ActivityType : FolderChooserDialogCreate.FolderCallback? {
    private var input: String? = null

    /* Allow SettingsGeneral and Settings Activity classes to use the same XML functionality */
    fun Bind() {
        val notifLayout =
            context!!.findViewById<RelativeLayout>(R.id.settings_general_notifications)
        val notifCurrentView =
            context.findViewById<TextView>(R.id.settings_general_notifications_current)
        val subNotifLayout =
            context.findViewById<RelativeLayout>(R.id.settings_general_sub_notifications)
        val defaultSortingCurrentView =
            context.findViewById<TextView>(R.id.settings_general_sorting_current)
        context.findViewById<View>(R.id.settings_general_drawer_items)
            .setOnClickListener { v: View? ->
                DrawerItemsDialog(
                    MaterialDialog.Builder(
                        context
                    )
                ).show()
            }
        run {
            val immersiveModeSwitch =
                context!!.findViewById<SwitchCompat>(R.id.settings_general_immersivemode)
            if (immersiveModeSwitch != null) {
                immersiveModeSwitch.isChecked = SettingValues.immersiveMode
                immersiveModeSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                    SettingsThemeFragment.changed = true
                    SettingValues.immersiveMode = isChecked
                })
            }
        }
        run {
            val highClrSpaceSwitch =
                context!!.findViewById<SwitchCompat>(R.id.settings_general_high_colorspace)
            if (highClrSpaceSwitch != null) {
                highClrSpaceSwitch.isChecked = SettingValues.highColorspaceImages
                highClrSpaceSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                    SettingsThemeFragment.changed = true
                    SettingValues.highColorspaceImages = isChecked
                    val application = context.application as App
                    ImageLoaderUtils.initImageLoader(application.applicationContext)
                    application.defaultImageLoader = ImageLoaderUtils.imageLoader
                })
            }
        }
        run {
            val forceLangSwitch =
                context!!.findViewById<SwitchCompat>(R.id.settings_general_forcelanguage)
            if (forceLangSwitch != null) {
                forceLangSwitch.isChecked = SettingValues.overrideLanguage
                forceLangSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                    SettingsThemeFragment.changed = true
                    SettingValues.overrideLanguage = isChecked
                })
            }
        }

        //hide fab while scrolling
        run {
            val alwaysShowFabSwitch =
                context!!.findViewById<SwitchCompat>(R.id.settings_general_always_show_fab)
            if (alwaysShowFabSwitch != null) {
                alwaysShowFabSwitch.isChecked = SettingValues.alwaysShowFAB
                alwaysShowFabSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    SettingsThemeFragment.changed = true
                    SettingValues.alwaysShowFAB = isChecked
                }
            }
        }

        // Show image download button
        run {
            val showDownloadBtnSwitch =
                context!!.findViewById<SwitchCompat>(R.id.settings_general_show_download_button)
            if (showDownloadBtnSwitch != null) {
                showDownloadBtnSwitch.isChecked = SettingValues.imageDownloadButton
                showDownloadBtnSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    SettingValues.imageDownloadButton = isChecked
                }
            }
        }
        run {
            val subfolderSwitch =
                context!!.findViewById<SwitchCompat>(R.id.settings_general_subfolder)
            if (subfolderSwitch != null) {
                subfolderSwitch.isChecked = SettingValues.imageSubfolders
                subfolderSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    SettingValues.imageSubfolders = isChecked
                }
            }
        }
        val setSaveLocationLayout =
            context.findViewById<RelativeLayout>(R.id.settings_general_set_save_location)
        setSaveLocationLayout?.setOnClickListener { v: View? ->
            DialogUtil.showFolderChooserDialog(
                context
            )
        }
        val setSaveLocationView =
            context.findViewById<TextView>(R.id.settings_general_set_save_location_view)
        if (setSaveLocationView != null) {
            val loc = SettingValues.appRestart.getString(
                "imagelocation",
                context.getString(R.string.settings_image_location_unset)
            )
            setSaveLocationView.text = loc
        }
        val expandedMenuSwitch =
            context.findViewById<SwitchCompat>(R.id.settings_general_expandedmenu)
        if (expandedMenuSwitch != null) {
            expandedMenuSwitch.isChecked = SettingValues.expandedToolbar
            expandedMenuSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                SettingValues.expandedToolbar = isChecked
            }
        }
        val viewTypeLayout = context.findViewById<RelativeLayout>(R.id.settings_general_viewtype)
        viewTypeLayout?.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(context, SettingsViewType::class.java)
                context.startActivity(i)
            }
        })

        //FAB multi choice//
        val fabLayout = context.findViewById<RelativeLayout>(R.id.settings_general_fab)
        val currentFabView = context.findViewById<TextView>(R.id.settings_general_fab_current)
        if (currentFabView != null && fabLayout != null) {
            currentFabView.text =
                if (SettingValues.fab) if (SettingValues.fabType == Constants.FAB_DISMISS) context.getString(
                    R.string.fab_hide
                ) else context.getString(R.string.fab_create) else context.getString(R.string.fab_disabled)
            fabLayout.setOnClickListener { v: View? ->
                val popup: PopupMenu = PopupMenu(
                    context, (v)!!
                )
                popup.menuInflater.inflate(R.menu.fab_settings, popup.menu)
                popup.setOnMenuItemClickListener { item: MenuItem ->
                    when (item.itemId) {
                        R.id.disabled -> {
                            SettingValues.fab = false
                        }

                        R.id.hide -> {
                            SettingValues.fab = true
                            SettingValues.fabType = Constants.FAB_DISMISS
                        }

                        R.id.create -> {
                            SettingValues.fab = true
                            SettingValues.fabType = Constants.FAB_POST
                        }

                        R.id.search -> {
                            SettingValues.fab = true
                            SettingValues.fabType = Constants.FAB_SEARCH
                        }
                    }
                    if (SettingValues.fab) {
                        if (SettingValues.fabType == Constants.FAB_DISMISS) {
                            currentFabView.setText(R.string.fab_hide)
                        } else if (SettingValues.fabType == Constants.FAB_POST) {
                            currentFabView.setText(R.string.fab_create)
                        } else {
                            currentFabView.setText(R.string.fab_search)
                        }
                    } else {
                        currentFabView.setText(R.string.fab_disabled)
                    }
                    true
                }
                popup.show()
            }
        }

        //SettingValues.subredditSearchMethod == 1 for drawer, 2 for toolbar, 3 for both
        val currentMethodTitle =
            context.findViewById<TextView>(R.id.settings_general_subreddit_search_method_current)
        if (currentMethodTitle != null) {
            when (SettingValues.subredditSearchMethod) {
                Constants.SUBREDDIT_SEARCH_METHOD_DRAWER -> currentMethodTitle.text =
                    context.getString(R.string.subreddit_search_method_drawer)

                Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR -> currentMethodTitle.text =
                    context.getString(R.string.subreddit_search_method_toolbar)

                Constants.SUBREDDIT_SEARCH_METHOD_BOTH -> currentMethodTitle.text =
                    context.getString(R.string.subreddit_search_method_both)
            }
        }
        val currentMethodLayout =
            context.findViewById<RelativeLayout>(R.id.settings_general_subreddit_search_method)
        currentMethodLayout?.setOnClickListener { v: View? ->
            val popup: PopupMenu = PopupMenu(
                context, (v)!!
            )
            popup.menuInflater.inflate(R.menu.subreddit_search_settings, popup.menu)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.subreddit_search_drawer -> {
                        SettingValues.subredditSearchMethod =
                            Constants.SUBREDDIT_SEARCH_METHOD_DRAWER
                        searchChanged = true
                    }

                    R.id.subreddit_search_toolbar -> {
                        SettingValues.subredditSearchMethod =
                            Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                        searchChanged = true
                    }

                    R.id.subreddit_search_both -> {
                        SettingValues.subredditSearchMethod = Constants.SUBREDDIT_SEARCH_METHOD_BOTH
                        searchChanged = true
                    }
                }
                when (SettingValues.subredditSearchMethod) {
                    Constants.SUBREDDIT_SEARCH_METHOD_DRAWER -> currentMethodTitle!!.text =
                        context.getString(R.string.subreddit_search_method_drawer)

                    Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR -> currentMethodTitle!!.text =
                        context.getString(R.string.subreddit_search_method_toolbar)

                    Constants.SUBREDDIT_SEARCH_METHOD_BOTH -> currentMethodTitle!!.text =
                        context.getString(R.string.subreddit_search_method_both)
                }
                true
            }
            popup.show()
        }
        val currentBackButtonTitle =
            context.findViewById<TextView>(R.id.settings_general_back_button_behavior_current)
        if ((SettingValues.backButtonBehavior
                    == BackButtonBehaviorOptions.ConfirmExit.value)
        ) {
            currentBackButtonTitle.text =
                context.getString(R.string.back_button_behavior_confirm_exit)
        } else if ((SettingValues.backButtonBehavior
                    == BackButtonBehaviorOptions.OpenDrawer.value)
        ) {
            currentBackButtonTitle.text = context.getString(R.string.back_button_behavior_drawer)
        } else if ((SettingValues.backButtonBehavior
                    == BackButtonBehaviorOptions.GotoFirst.value)
        ) {
            currentBackButtonTitle.text =
                context.getString(R.string.back_button_behavior_goto_first)
        } else {
            currentBackButtonTitle.text = context.getString(R.string.back_button_behavior_default)
        }
        val currentBackButtonLayout =
            context.findViewById<RelativeLayout>(R.id.settings_general_back_button_behavior)
        currentBackButtonLayout.setOnClickListener { v: View? ->
            val popup: PopupMenu = PopupMenu(
                context, (v)!!
            )
            popup.getMenuInflater().inflate(R.menu.back_button_behavior_settings, popup.getMenu())
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem ->
                when (item.getItemId()) {
                    R.id.back_button_behavior_default -> {
                        SettingValues.backButtonBehavior =
                            BackButtonBehaviorOptions.Default.getValue()
                    }

                    R.id.back_button_behavior_confirm_exit -> {
                        SettingValues.backButtonBehavior =
                            BackButtonBehaviorOptions.ConfirmExit.getValue()
                    }

                    R.id.back_button_behavior_open_drawer -> {
                        SettingValues.backButtonBehavior =
                            BackButtonBehaviorOptions.OpenDrawer.getValue()
                    }

                    R.id.back_button_behavior_goto_first -> {
                        SettingValues.backButtonBehavior =
                            BackButtonBehaviorOptions.GotoFirst.getValue()
                    }
                }
                if ((SettingValues.backButtonBehavior
                            == BackButtonBehaviorOptions.ConfirmExit.getValue())
                ) {
                    currentBackButtonTitle.setText(
                        context.getString(R.string.back_button_behavior_confirm_exit)
                    )
                } else if ((SettingValues.backButtonBehavior
                            == BackButtonBehaviorOptions.OpenDrawer.getValue())
                ) {
                    currentBackButtonTitle.setText(
                        context.getString(R.string.back_button_behavior_drawer)
                    )
                } else if ((SettingValues.backButtonBehavior
                            == BackButtonBehaviorOptions.GotoFirst.getValue())
                ) {
                    currentBackButtonTitle.setText(
                        context.getString(R.string.back_button_behavior_goto_first)
                    )
                } else {
                    currentBackButtonTitle.setText(
                        context.getString(R.string.back_button_behavior_default)
                    )
                }
                true
            })
            popup.show()
        }
        if (notifCurrentView != null &&
            context.findViewById<View?>(R.id.settings_general_sub_notifs_current) != null
        ) {
            if (App.notificationTime > 0) {
                notifCurrentView.text = context.getString(
                    R.string.settings_notification_short,
                    TimeUtils.getTimeInHoursAndMins(
                        App.notificationTime,
                        context.baseContext
                    )
                )
                setSubText()
            } else {
                notifCurrentView.setText(R.string.settings_notifdisabled)
                (context.findViewById<View>(R.id.settings_general_sub_notifs_current) as TextView).setText(
                    R.string.settings_enable_notifs
                )
            }
        }
        if (Authentication.isLoggedIn) {
            notifLayout?.setOnClickListener { v: View? ->
                val inflater: LayoutInflater = context.layoutInflater
                val dialoglayout: View = inflater.inflate(R.layout.inboxfrequency, null)
                setupNotificationSettings(dialoglayout, context)
            }
            subNotifLayout?.setOnClickListener { v: View? -> showSelectDialog() }
        } else {
            if (notifLayout != null) {
                notifLayout.isEnabled = false
                notifLayout.alpha = 0.25f
            }
            if (subNotifLayout != null) {
                subNotifLayout.isEnabled = false
                subNotifLayout.alpha = 0.25f
            }
        }
        if (defaultSortingCurrentView != null) {
            defaultSortingCurrentView.text =
                SortingUtil.getSortingStrings()[SortingUtil.getSortingId("")]
        }
        run {
            if (context!!.findViewById<View?>(R.id.settings_general_sorting) != null) {
                context.findViewById<View>(R.id.settings_general_sorting).setOnClickListener(
                    View.OnClickListener { v: View? ->
                        val l2 =
                            DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                                when (i) {
                                    0 -> SortingUtil.defaultSorting = Sorting.HOT
                                    1 -> SortingUtil.defaultSorting = Sorting.NEW
                                    2 -> SortingUtil.defaultSorting = Sorting.RISING
                                    3 -> {
                                        SortingUtil.defaultSorting = Sorting.TOP
                                        askTimePeriod()
                                        return@OnClickListener
                                    }

                                    4 -> {
                                        SortingUtil.defaultSorting = Sorting.CONTROVERSIAL
                                        askTimePeriod()
                                        return@OnClickListener
                                    }
                                }
                                SettingValues.defaultSorting = SortingUtil.defaultSorting
                                if (defaultSortingCurrentView != null) {
                                    defaultSortingCurrentView.text = SortingUtil.getSortingStrings()
                                        .get(SortingUtil.getSortingId(""))
                                }
                            }

                        // Remove the "Best" sorting option from settings because it is only supported on the frontpage.
                        var skip = -1
                        val sortingStrings: MutableList<String> =
                            ArrayList(Arrays.asList(*SortingUtil.getSortingStrings()))
                        for (i in sortingStrings.indices) {
                            if ((sortingStrings[i] == context.getString(R.string.sorting_best))) {
                                skip = i
                                break
                            }
                        }
                        if (skip != -1) {
                            sortingStrings.removeAt(skip)
                        }
                        AlertDialog.Builder(this@SettingsGeneralFragment.context)
                            .setTitle(R.string.sorting_choose)
                            .setSingleChoiceItems(
                                sortingStrings.toTypedArray(),
                                SortingUtil.getSortingId(""),
                                l2
                            )
                            .show()
                    })
            }
        }
        doNotifText(context)
        run {
            val i2 =
                if (SettingValues.defaultCommentSorting == CommentSort.CONFIDENCE) 0 else if (SettingValues.defaultCommentSorting == CommentSort.TOP) 1 else if (SettingValues.defaultCommentSorting == CommentSort.NEW) 2 else if ((SettingValues.defaultCommentSorting
                            == CommentSort.CONTROVERSIAL)
                ) 3 else if (SettingValues.defaultCommentSorting == CommentSort.OLD) 4 else if ((SettingValues.defaultCommentSorting
                            == CommentSort.QA)
                ) 5 else 0
            val sortingCurrentCommentView =
                context!!.findViewById<TextView>(R.id.settings_general_sorting_current_comment)
            if (sortingCurrentCommentView != null) {
                sortingCurrentCommentView.text = SortingUtil.getSortingCommentsStrings()[i2]
            }
            if (context.findViewById<View?>(R.id.settings_general_sorting_comment) != null) {
                context.findViewById<View>(R.id.settings_general_sorting_comment)
                    .setOnClickListener { v: View? ->
                        val l2: DialogInterface.OnClickListener =
                            DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                                var commentSorting: CommentSort =
                                    SettingValues.defaultCommentSorting
                                when (i) {
                                    0 -> commentSorting = CommentSort.CONFIDENCE
                                    1 -> commentSorting = CommentSort.TOP
                                    2 -> commentSorting = CommentSort.NEW
                                    3 -> commentSorting = CommentSort.CONTROVERSIAL
                                    4 -> commentSorting = CommentSort.OLD
                                    5 -> commentSorting = CommentSort.QA
                                }
                                SettingValues.defaultCommentSorting = commentSorting
                                if (sortingCurrentCommentView != null) {
                                    sortingCurrentCommentView.text =
                                        SortingUtil.getSortingCommentsStrings().get(i)
                                }
                            }
                        val res: Resources = context.baseContext.resources
                        AlertDialog.Builder(this@SettingsGeneralFragment.context)
                            .setTitle(R.string.sorting_choose)
                            .setSingleChoiceItems(
                                arrayOf(
                                    res.getString(R.string.sorting_best),
                                    res.getString(R.string.sorting_top),
                                    res.getString(R.string.sorting_new),
                                    res.getString(R.string.sorting_controversial),
                                    res.getString(R.string.sorting_old),
                                    res.getString(R.string.sorting_ama)
                                ), i2, l2
                            )
                            .show()
                    }
            }
        }
    }

    private fun askTimePeriod() {
        val defaultSortingCurrentView =
            context!!.findViewById<TextView>(R.id.settings_general_sorting_current)
        val l2 = DialogInterface.OnClickListener { dialogInterface, i ->
            when (i) {
                0 -> SortingUtil.timePeriod = TimePeriod.HOUR
                1 -> SortingUtil.timePeriod = TimePeriod.DAY
                2 -> SortingUtil.timePeriod = TimePeriod.WEEK
                3 -> SortingUtil.timePeriod = TimePeriod.MONTH
                4 -> SortingUtil.timePeriod = TimePeriod.YEAR
                5 -> SortingUtil.timePeriod = TimePeriod.ALL
            }
            SettingValues.defaultSorting = SortingUtil.defaultSorting
            SettingValues.timePeriod = SortingUtil.timePeriod
            defaultSortingCurrentView.text =
                (SortingUtil.getSortingStrings()[SortingUtil.getSortingId("")]
                        + " > "
                        + SortingUtil.getSortingTimesStrings()[SortingUtil.getSortingTimeId(
                    ""
                )])
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.sorting_choose)
            .setSingleChoiceItems(
                SortingUtil.getSortingTimesStrings(),
                SortingUtil.getSortingTimeId(""),
                l2
            )
            .show()
    }

    private fun setSubText() {
        val rawSubs = StringUtil.stringToArray(SettingValues.appRestart.getString(CheckForMail.SUBS_TO_GET, ""))
        var subText = context!!.getString(R.string.sub_post_notifs_settings_none)
        val subs = StringBuilder()
        for (s: String in rawSubs) {
            if (!s.isEmpty()) {
                try {
                    val split = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    subs.append(split[0])
                    subs.append("(+").append(split[1]).append(")")
                    subs.append(", ")
                } catch (ignored: Exception) {
                }
            }
        }
        if (!subs.toString().isEmpty()) {
            subText = subs.substring(0, subs.toString().length - 2)
        }
        (context.findViewById<View>(R.id.settings_general_sub_notifs_current) as TextView).text =
            subText
    }

    private fun showSelectDialog() {
        val rawSubs = StringUtil.stringToArray(SettingValues.appRestart.getString(CheckForMail.SUBS_TO_GET, ""))
        val subThresholds = HashMap<String, Int>()
        for (s: String in rawSubs) {
            try {
                val split = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                subThresholds[split[0].lowercase()] = Integer.valueOf(split[1])
            } catch (ignored: Exception) {
            }
        }

        // Get list of user's subscriptions
        val subs = UserSubscriptions.getSubscriptions(context)
        // Add any subs that the user has notifications for but isn't subscribed to
        for (s: String in subThresholds.keys) {
            if (subs == null) break // TODO: stopgap
            if (!subs.contains(s)) {
                subs.add(s)
            }
        }
        val sorted: List<String> = UserSubscriptions.sort(subs)

        //Array of all subs
        var all = arrayOfNulls<String>(sorted.size)
        //Contains which subreddits are checked
        val checked = BooleanArray(all.size)


        //Remove special subreddits from list and store it in "all"
        var i = 0
        for (s: String in sorted) {
            if ((s != "all"
                        && s != "frontpage"
                        && !s.contains("+")
                        && !s.contains(".")
                        && !s.contains("/m/"))
            ) {
                all[i] = s.lowercase()
                i++
            }
        }

        //Remove empty entries & store which subreddits are checked
        val list: MutableList<String?> = ArrayList()
        i = 0
        for (s: String? in all) {
            if (s != null && !s.isEmpty()) {
                list.add(s)
                if (subThresholds.containsKey(s)) {
                    checked[i] = true
                }
                i++
            }
        }

        //Convert List back to Array
        all = list.toTypedArray()
        val toCheck = ArrayList(subThresholds.keys)
        val finalAll = all
        AlertDialog.Builder(context!!)
            .setMultiChoiceItems(
                finalAll,
                checked
            ) { dialog: DialogInterface?, which: Int, isChecked: Boolean ->
                if (!isChecked) {
                    toCheck.remove(finalAll.get(which))
                } else {
                    toCheck.add(finalAll.get(which))
                }
            }
            .setTitle(R.string.sub_post_notifs_title_settings)
            .setPositiveButton(
                context!!.getString(R.string.btn_add).uppercase(Locale.getDefault())
            ) { dialog: DialogInterface?, which: Int -> showThresholdDialog(toCheck, false) }
            .setNegativeButton(R.string.sub_post_notifs_settings_search) { dialog: DialogInterface?, which: Int ->
                MaterialDialog.Builder(
                    context
                )
                    .title(R.string.reorder_add_subreddit)
                    .inputRangeRes(2, 21, R.color.md_red_500)
                    .alwaysCallInputCallback()
                    .input(
                        context.getString(R.string.reorder_subreddit_name), null,
                        false
                    ) { dialog, raw ->
                        input = raw.toString()
                            .replace(
                                "\\s".toRegex(),
                                ""
                            ) //remove whitespace from input
                    }
                    .positiveText(R.string.btn_add)
                    .onPositive { dialog, which -> AsyncGetSubreddit().execute(input) }
                    .negativeText(R.string.btn_cancel)
                    .show()
            }
            .show()
    }

    private fun showThresholdDialog(strings: ArrayList<String?>, search: Boolean) {
        val subsRaw = StringUtil.stringToArray(SettingValues.appRestart.getString(CheckForMail.SUBS_TO_GET, ""))
        if (!search) {
            //NOT a sub searched for, was instead a list of all subs
            for (raw: String in ArrayList(subsRaw)) {
                if (!strings.contains(raw.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0])) {
                    subsRaw.remove(raw)
                }
            }
        }
        val subs = ArrayList<String>()
        for (s: String in subsRaw) {
            try {
                subs.add(s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].lowercase())
            } catch (e: Exception) {
            }
        }
        val toAdd = ArrayList<String>()
        for (s: String? in strings) {
            if (!subs.contains(s!!.lowercase())) {
                toAdd.add(s.lowercase())
            }
        }
        if (!toAdd.isEmpty()) {
            MaterialDialog.Builder(context!!).title(
                R.string.sub_post_notifs_threshold
            )
                .items(*arrayOf<String>("1", "5", "10", "20", "40", "50"))
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(0
                ) { dialog, itemView, which, text ->
                    for (s: String in toAdd) {
                        subsRaw.add("$s:$text")
                    }
                    saveAndUpdateSubs(subsRaw)
                    true
                }
                .cancelable(false)
                .show()
        } else {
            saveAndUpdateSubs(subsRaw)
        }
    }

    private fun saveAndUpdateSubs(subs: ArrayList<String>) {
        SettingValues.appRestart.edit()
            .putString(CheckForMail.SUBS_TO_GET, StringUtil.arrayToString(subs))
            .commit()
        setSubText()
    }

    override fun onFolderSelection(
        dialog: FolderChooserDialogCreate,
        folder: File, isSaveToLocation: Boolean
    ) {
        SettingValues.appRestart.edit().putString("imagelocation", folder.absolutePath).apply()
        Toast.makeText(
            context,
            context!!.getString(R.string.settings_set_image_location, folder.absolutePath),
            Toast.LENGTH_LONG
        ).show()
        (context.findViewById<View>(R.id.settings_general_set_save_location_view) as TextView).text =
            folder.absolutePath
    }

    override fun onFolderChooserDismissed(dialog: FolderChooserDialogCreate) {}
    private inner class AsyncGetSubreddit : AsyncTask<String?, Void?, Subreddit?>() {
        public override fun onPostExecute(subreddit: Subreddit?) {
            if (subreddit != null || input.equals("friends", ignoreCase = true) || input.equals(
                    "mod", ignoreCase = true
                )
            ) {
                val singleSub = ArrayList<String?>()
                singleSub.add(subreddit!!.displayName.lowercase())
                showThresholdDialog(singleSub, true)
            }
        }

        override fun doInBackground(vararg params: String?): Subreddit? {
            return try {
                Authentication.reddit!!.getSubreddit(params[0])
            } catch (e: Exception) {
                context!!.runOnUiThread {
                    try {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.subreddit_err)
                            .setMessage(
                                context.getString(
                                    R.string.subreddit_err_msg,
                                    params.get(0)
                                )
                            )
                            .setPositiveButton(
                                R.string.btn_ok,
                                DialogInterface.OnClickListener { dialog: DialogInterface, which: Int -> dialog.dismiss() })
                            .setOnDismissListener(null)
                            .show()
                    } catch (ignored: Exception) {
                    }
                }
                null
            }
        }
    }

    companion object {
        var searchChanged //whether or not the subreddit search method changed
                = false

        @JvmStatic
        fun setupNotificationSettings(dialoglayout: View, context: Activity) {
            val landscape = dialoglayout.findViewById<Slider>(R.id.landscape)
            val checkBox = dialoglayout.findViewById<CheckBox>(R.id.load)
            val sound = dialoglayout.findViewById<CheckBox>(R.id.sound)
            val notifCurrentView =
                context.findViewById<TextView>(R.id.settings_general_notifications_current)
            sound.isChecked = SettingValues.notifSound
            sound.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.notifSound = isChecked
            }
            if (App.notificationTime == -1) {
                checkBox.isChecked = false
                checkBox.text = context.getString(R.string.settings_mail_check)
            } else {
                checkBox.isChecked = true
                landscape.setValue(App.notificationTime / 15.0f, false)
                checkBox.text = context.getString(
                    R.string.settings_notification_newline,
                    TimeUtils.getTimeInHoursAndMins(
                        App.notificationTime,
                        context.baseContext
                    )
                )
            }
            landscape.setOnPositionChangeListener { slider, b, v, v1, i, i1 ->
                if (checkBox.isChecked) {
                    checkBox.text = context.getString(
                        R.string.settings_notification,
                        TimeUtils.getTimeInHoursAndMins(i1 * 15, context.baseContext)
                    )
                }
            }
            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                if (!isChecked) {
                    App.notificationTime = -1
                    SettingValues.colours.edit().putInt("notificationOverride", -1).apply()
                    checkBox.text = context.getString(R.string.settings_mail_check)
                    landscape.setValue(0f, true)
                    if (App.notifications != null) {
                        App.notifications!!.cancel()
                    }
                } else {
                    App.notificationTime = 60
                    landscape.setValue(4f, true)
                    checkBox.text = context.getString(
                        R.string.settings_notification,
                        TimeUtils.getTimeInHoursAndMins(
                            App.notificationTime,
                            context.baseContext
                        )
                    )
                }
            }
            dialoglayout.findViewById<View>(R.id.title)
                .setBackgroundColor(Palette.getDefaultColor())
            //todo final Slider portrait = (Slider) dialoglayout.findViewById(R.id.portrait);

            //todo  portrait.setBackgroundColor(Palette.getDefaultColor());
            val builder = AlertDialog.Builder(context)
                .setView(dialoglayout)
            val dialog: Dialog = builder.create()
            dialog.show()
            dialog.setOnDismissListener {
                if (checkBox.isChecked) {
                    App.notificationTime = landscape.value * 15
                    SettingValues.colours.edit()
                        .putInt("notificationOverride", landscape.value * 15)
                        .apply()
                    if (App.notifications == null) {
                        App.notifications = NotificationJobScheduler(context.application)
                    }
                    App.notifications!!.cancel()
                    App.notifications!!.start()
                }
            }
            dialoglayout.findViewById<View>(R.id.save)
                .setOnClickListener {
                    if (checkBox.isChecked) {
                        App.notificationTime = landscape.value * 15
                        SettingValues.colours.edit()
                            .putInt("notificationOverride", landscape.value * 15)
                            .apply()
                        if (App.notifications == null) {
                            App.notifications = NotificationJobScheduler(context.application)
                        }
                        App.notifications!!.cancel()
                        App.notifications!!.start()
                        dialog.dismiss()
                        if (context is SettingsGeneral) {
                            notifCurrentView.text = context.getString(
                                R.string.settings_notification_short,
                                TimeUtils.getTimeInHoursAndMins(
                                    App.notificationTime,
                                    context.getBaseContext()
                                )
                            )
                        }
                    } else {
                        App.notificationTime = -1
                        SettingValues.colours.edit().putInt("notificationOverride", -1).apply()
                        if (App.notifications == null) {
                            App.notifications = NotificationJobScheduler(context.application)
                        }
                        App.notifications!!.cancel()
                        dialog.dismiss()
                        if (context is SettingsGeneral) {
                            notifCurrentView.setText(R.string.settings_notifdisabled)
                        }
                    }
                }
        }

        @JvmStatic
        fun doNotifText(context: Activity) {
            run {
                val notifs = context.findViewById<View>(R.id.settings_general_redditnotifs)
                if (notifs != null) {
                    if (!isPackageInstalled("com.reddit.frontpage") ||
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                    ) {
                        notifs.visibility = View.GONE
                        if (context.findViewById<View?>(R.id.settings_general_installreddit) != null) {
                            context.findViewById<View>(R.id.settings_general_installreddit).visibility =
                                View.VISIBLE
                        }
                    } else {
                        if ((context.application as App).isNotificationAccessEnabled) {
                            val single =
                                context.findViewById<SwitchCompat>(R.id.settings_general_piggyback)
                            if (single != null) {
                                single.isChecked = true
                                single.isEnabled = false
                            }
                        } else {
                            val single =
                                context.findViewById<SwitchCompat>(R.id.settings_general_piggyback)
                            if (single != null) {
                                single.isChecked = false
                                single.isEnabled = true
                                single.setOnCheckedChangeListener { compoundButton, b ->
                                    single.isChecked = false
                                    val s = Snackbar.make(
                                        single,
                                        "Give Slide notification access",
                                        Snackbar.LENGTH_LONG
                                    )
                                    s.setAction(
                                        "Go to settings"
                                    ) { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
                                    s.show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
