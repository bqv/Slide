package me.ccrama.redditslide.ui.settings

import android.app.Activity
import android.content.DialogInterface
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.google.common.collect.ImmutableList
import com.rey.material.app.TimePickerDialog
import ltd.ucode.slide.App
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.PostRepository
import me.ccrama.redditslide.Autocache.AutoCacheScheduler
import me.ccrama.redditslide.CommentCacheAsync
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.StringUtil
import me.ccrama.redditslide.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale

class ManageOfflineContentFragment(private val context: Activity) {
    val postRepository: PostRepository get() = when (context) {
        is ManageOfflineContent -> { context.postRepository }
        is SettingsActivity -> { context.postRepository }
        else -> { throw IllegalArgumentException(context.localClassName) }
    }

    val commentRepository: CommentRepository get() = when (context) {
        is ManageOfflineContent -> { context.commentRepository }
        is SettingsActivity -> { context.commentRepository }
        else -> { throw IllegalArgumentException(context.localClassName) }
    }

    fun Bind() {
        if (!NetworkUtil.isConnected(context)) SettingsThemeFragment.changed = true
        context.findViewById<View>(R.id.manage_history_clear_all).setOnClickListener {
            val wifi = App.cachedData!!.getBoolean("wifiOnly", false)
            val sync = App.cachedData!!.getString("toCache", "")
            val hour = (App.cachedData!!.getInt("hour", 0))
            val minute = (App.cachedData!!.getInt("minute", 0))
            App.cachedData!!.edit().clear().apply()
            App.cachedData!!.edit().putBoolean("wifiOnly", wifi).putString(
                "toCache", sync
            ).putInt("hour", hour).putInt("minute", minute).apply()
            context.finish()
        }
        if (NetworkUtil.isConnectedNoOverride(context)) {
            context.findViewById<View>(R.id.manage_history_sync_now)
                .setOnClickListener {
                    CommentCacheAsync(
                        context, postRepository, commentRepository, App.cachedData!!.getString(
                            "toCache", ""
                        )!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    ).execute()
                }
        } else {
            context.findViewById<View>(R.id.manage_history_sync_now).visibility = View.GONE
        }
        run {
            val single: SwitchCompat = context.findViewById(R.id.manage_history_wifi)
            single.isChecked = App.cachedData!!.getBoolean("wifiOnly", false)
            single.setOnCheckedChangeListener { buttonView, isChecked ->
                App.cachedData!!.edit().putBoolean("wifiOnly", isChecked).apply()
            }
        }
        updateBackup()
        updateFilters()
        val commentDepths: List<Int?> = ImmutableList.of(2, 4, 6, 8, 10)
        val commentDepthArray = arrayOfNulls<String>(commentDepths.size)
        context.findViewById<View>(R.id.manage_history_comments_depth)
            .setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    val commentDepth = SettingValues.commentDepth ?: 2
                    AlertDialog.Builder(context)
                        .setTitle(R.string.comments_depth)
                        .setSingleChoiceItems(
                            commentDepths.map { toString() }.toTypedArray(),
                            commentDepths.indexOf(commentDepth)
                        ) { dialog: DialogInterface?, which: Int ->
                            SettingValues.commentDepth = commentDepths[which]
                        }
                        .show()
                }
            })
        val commentCounts: List<Int> = ImmutableList.of(20, 40, 60, 80, 100)
        context.findViewById<View>(R.id.manage_history_comments_count)
            .setOnClickListener {
                val commentCount = SettingValues.commentCount ?: 20
                AlertDialog.Builder(context)
                    .setTitle(R.string.comments_count)
                    .setSingleChoiceItems(
                        commentCounts.map { it.toString() }.toTypedArray(),
                        commentCounts.indexOf(commentCount)
                    ) { dialog, which ->
                        SettingValues.commentCount = commentCounts[which]
                    }
                    .show()
            }
        context.findViewById<View>(R.id.manage_history_autocache)
            .setOnClickListener {
                val sorted: List<String> = UserSubscriptions.sort(
                    UserSubscriptions.getSubscriptions(context)
                )
                val all = arrayOfNulls<String>(sorted.size)
                val checked = BooleanArray(all.size)
                val s2: MutableList<String?> = ArrayList()
                Collections.addAll(
                    s2, *App.cachedData!!.getString("toCache", "")!!
                        .split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                )
                for ((i, s: String?) in sorted.withIndex()) {
                    all[i] = s
                    if (s2.contains(s)) {
                        checked[i] = true
                    }
                }
                val toCheck = ArrayList(s2)
                AlertDialog.Builder(context)
                    .setMultiChoiceItems(
                        all,
                        checked
                    ) { dialog: DialogInterface?, which: Int, isChecked: Boolean ->
                        if (!isChecked) {
                            toCheck.remove(all.get(which))
                        } else {
                            toCheck.add(all.get(which))
                        }
                    }
                    .setTitle(R.string.multireddit_selector)
                    .setPositiveButton(
                        context.getString(R.string.btn_add).uppercase(Locale.getDefault())
                    ) { dialog: DialogInterface?, which: Int ->
                        App.cachedData!!.edit()
                            .putString("toCache", StringUtil.arrayToString(toCheck)).apply()
                        updateBackup()
                    }
                    .show()
            }
        updateTime()
        context.findViewById<View>(R.id.manage_history_autocache_time_touch)
            .setOnClickListener {
                val d = TimePickerDialog(
                    context
                )
                d.hour(App.cachedData!!.getInt("hour", 0))
                d.minute(App.cachedData!!.getInt("minute", 0))
                d.applyStyle(
                    ColorPreferences(
                        context
                    ).fontStyle.baseId
                )
                d.positiveAction("SET")
                val typedValue = TypedValue()
                val theme = context.theme
                theme.resolveAttribute(R.attr.activity_background, typedValue, true)
                val color = typedValue.data
                d.backgroundColor(color)
                d.actionTextColor(
                    context.resources.getColor(
                        ColorPreferences(context).fontStyle.color
                    )
                )
                d.positiveActionClickListener {
                    App.cachedData!!.edit()
                        .putInt("hour", d.hour)
                        .putInt("minute", d.minute)
                        .commit()
                    App.autoCache = AutoCacheScheduler(context)
                    App.autoCache!!.start()
                    updateTime()
                    d.dismiss()
                }
                theme.resolveAttribute(R.attr.fontColor, typedValue, true)
                val color2 = typedValue.data
                d.setTitle(context.getString(R.string.choose_sync_time))
                d.titleColor(color2)
                d.show()
            }
    }

    fun updateTime() {
        val text = context.findViewById<TextView>(R.id.manage_history_autocache_time)
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = App.cachedData!!.getInt("hour", 0)
        cal[Calendar.MINUTE] = App.cachedData!!.getInt("minute", 0)
        if (text != null) {
            text.text = context.getString(
                R.string.settings_backup_occurs, SimpleDateFormat("hh:mm a").format(cal.time)
            )
        }
    }

    fun updateBackup() {
        subsToBack = ArrayList()
        Collections.addAll(subsToBack, *App.cachedData!!.getString("toCache", "")!!
            .split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        val text = context.findViewById<TextView>(R.id.manage_history_autocache_text)
        if (!App.cachedData!!.getString("toCache", "")!!.contains(",") || subsToBack!!.isEmpty()) {
            text.setText(R.string.settings_backup_none)
        } else {
            val toSayBuilder = StringBuilder()
            for (s: String in subsToBack!!) {
                if (!s.isEmpty()) toSayBuilder.append(s).append(", ")
            }
            var toSay = toSayBuilder.toString()
            toSay = toSay.substring(0, toSay.length - 2)
            toSay += context.getString(R.string.settings_backup_will_backup)
            text.text = toSay
        }
    }

    var domains = ArrayList<String>()
    var subsToBack: MutableList<String>? = null
    fun updateFilters() {
        if (context.findViewById<View?>(R.id.manage_history_domainlist) != null) {
            val multiNameToSubsMap = UserSubscriptions.getMultiNameToSubs(true)
            domains = ArrayList()
            (context.findViewById<View>(R.id.manage_history_domainlist) as LinearLayout).removeAllViews()
            for (s: String in OfflineSubreddit.all) {
                if (!s.isEmpty()) {
                    val split = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    var sub: String? = split[0]
                    if (multiNameToSubsMap.containsKey(sub)) {
                        sub = multiNameToSubsMap[sub]
                    }
                    val name =
                        (if (sub!!.contains("/m/")) sub else "/c/$sub") + " → " + (if (split[1].toLong() == 0L) context.getString(
                            R.string.settings_backup_submission_only
                        ) else TimeUtils.getTimeAgo(
                            split[1].toLong(), context
                        ) + context.getString(R.string.settings_backup_comments))
                    domains.add(name)
                    val t = context.layoutInflater.inflate(
                        R.layout.account_textview,
                        context.findViewById(R.id.manage_history_domainlist),
                        false
                    )
                    (t.findViewById<View>(R.id.name) as TextView).text = name
                    t.findViewById<View>(R.id.remove)
                        .setOnClickListener {
                            domains.remove(name)
                            App.cachedData!!.edit().remove(s).apply()
                            updateFilters()
                        }
                    (context.findViewById<View>(R.id.manage_history_domainlist) as LinearLayout).addView(
                        t
                    )
                }
            }
        }
    }
}
