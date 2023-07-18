package me.ccrama.redditslide.Activities

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.authentication
import ltd.ucode.slide.SettingValues.lastInbox
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Activities.SendMessage
import me.ccrama.redditslide.Autocache.AutoCacheScheduler
import me.ccrama.redditslide.ContentGrabber
import me.ccrama.redditslide.Fragments.InboxPage
import me.ccrama.redditslide.Notifications.NotificationJobScheduler
import me.ccrama.redditslide.UserSubscriptions.doCachedModSubs
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.ui.settings.SettingsGeneralFragment.Companion.setupNotificationSettings
import me.ccrama.redditslide.util.KeyboardUtil
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.LogUtil
import net.dean.jraw.managers.InboxManager

class Inbox : BaseActivityAnim() {
    var adapter: InboxPagerAdapter? = null
    private var tabs: TabLayout? = null
    private var pager: ViewPager2? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_inbox, menu)

        //   if (mShowInfoButton) menu.findItem(R.id.action_info).setVisible(true);
        //   else menu.findItem(R.id.action_info).setVisible(false);
        return true
    }

    private var changed = false
    @JvmField var last: Long = 0
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            me.zhanghai.android.materialprogressbar.R.id.home -> onBackPressed()
            R.id.notifs -> {
                val inflater = layoutInflater
                val dialoglayout = inflater.inflate(R.layout.inboxfrequency, null)
                setupNotificationSettings(dialoglayout, this@Inbox)
            }

            R.id.compose -> {
                val i = Intent(this@Inbox, SendMessage::class.java)
                startActivity(i)
            }

            R.id.read -> {
                changed = false
                object : AsyncTask<Void?, Void?, Void?>() {
                    override fun doInBackground(vararg params: Void?): Void? {
                        try {
                            InboxManager(Authentication.reddit).setAllRead()
                            changed = true
                        } catch (ignored: Exception) {
                            ignored.printStackTrace()
                        }
                        return null
                    }

                    override fun onPostExecute(aVoid: Void?) {
                        if (changed) { //restart the fragment
                            adapter!!.notifyDataSetChanged()
                            try {
                                val CURRENT_TAB = tabs!!.selectedTabPosition
                                adapter = InboxPagerAdapter(supportFragmentManager)
                                pager!!.adapter = adapter
                                TabLayoutMediator(tabs!!, pager!!) { tab, position ->
                                }
                                LayoutUtils.scrollToTabAfterLayout(tabs, CURRENT_TAB)
                                pager!!.currentItem = CURRENT_TAB
                            } catch (_: Exception) {
                            }
                        }
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        if (Authentication.reddit == null || !Authentication.reddit.isAuthenticated || Authentication.me == null) {
            LogUtil.v("Reauthenticating")
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    if (Authentication.reddit == null) {
                        Authentication(applicationContext)
                    }
                    try {
                        Authentication.me = Authentication.reddit!!.me()
                        Authentication.mod = Authentication.me!!.isMod
                        authentication.edit()
                            .putBoolean(App.SHARED_PREF_IS_MOD, Authentication.mod)
                            .apply()
                        if (App.notificationTime != -1) {
                            App.notifications = NotificationJobScheduler(this@Inbox)
                            App.notifications!!.start()
                        }
                        if (App.cachedData!!.contains("toCache")) {
                            App.autoCache = AutoCacheScheduler(this@Inbox)
                            App.autoCache!!.start()
                        }
                        val name = Authentication.me!!.fullName
                        Authentication.name = name
                        LogUtil.v("AUTHENTICATED")
                        doCachedModSubs()
                        if (Authentication.reddit.isAuthenticated) {
                            val accounts = authentication.getStringSet("accounts", HashSet())
                            if (accounts!!.contains(name)) { //convert to new system
                                accounts.remove(name)
                                accounts.add(name + ":" + Authentication.refresh)
                                authentication.edit()
                                    .putStringSet("accounts", accounts)
                                    .apply() //force commit
                            }
                            Authentication.isLoggedIn = true
                            App.notFirst = true
                        }
                    } catch (ignored: Exception) {
                    }
                    return null
                }
            }.execute()
        }
        super.onCreate(savedInstance)
        last = lastInbox
        applyColorTheme("")
        setContentView(R.layout.activity_inbox)
        setupAppBar(R.id.toolbar, R.string.title_inbox, true, true)
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        tabs = findViewById<View>(R.id.sliding_tabs) as TabLayout
        tabs!!.tabMode = TabLayout.MODE_SCROLLABLE
        tabs!!.setSelectedTabIndicatorColor(ColorPreferences(this@Inbox).getColor("no sub"))
        pager = findViewById<View>(R.id.content_view) as ViewPager2
        findViewById<View>(R.id.header).setBackgroundColor(Palette.getDefaultColor())
        adapter = InboxPagerAdapter(supportFragmentManager)
        pager!!.adapter = adapter
        if (intent != null && intent.hasExtra(EXTRA_UNREAD)) {
            pager!!.currentItem = 1
        }
        TabLayoutMediator(tabs!!, pager!!) { tab, position ->
            findViewById<View>(R.id.header).animate()
                .translationY(0f)
                .setInterpolator(LinearInterpolator()).duration = 180
            if (position == 3 && findViewById<View?>(R.id.read) != null) {
                findViewById<View>(R.id.read).visibility = View.GONE
            } else if (findViewById<View?>(R.id.read) != null) {
                findViewById<View>(R.id.read).visibility = View.VISIBLE
            }
        }
    }

    inner class InboxPagerAdapter internal constructor(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        override fun createFragment(i: Int): Fragment {
            val f: Fragment = InboxPage()
            val args = Bundle()
            args.putString("id", ContentGrabber.InboxValue.values()[i].whereName)
            f.arguments = args
            return f
        }

        override fun getItemCount(): Int {
            return ContentGrabber.InboxValue.values().size
        }

        fun getPageTitle(position: Int): CharSequence {
            return getString(ContentGrabber.InboxValue.values()[position].displayName)
        }
    }

    override fun onResume() {
        super.onResume()
        KeyboardUtil.hideKeyboard(this, window.attributes.token, 0)
    }

    companion object {
        const val EXTRA_UNREAD = "unread"
    }
}
