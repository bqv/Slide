package me.ccrama.redditslide.Activities

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import ltd.ucode.network.reddit.data.RedditSubmission
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.authentication
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Autocache.AutoCacheScheduler
import me.ccrama.redditslide.Fragments.BlankFragment
import me.ccrama.redditslide.Fragments.CommentPage
import me.ccrama.redditslide.HasSeen.addSeen
import me.ccrama.redditslide.HasSeen.setHasSeenSubmission
import me.ccrama.redditslide.LastComments.setComments
import me.ccrama.redditslide.Notifications.NotificationJobScheduler
import me.ccrama.redditslide.SwipeLayout.Utils
import me.ccrama.redditslide.UserSubscriptions.doCachedModSubs
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.LogUtil

/**
 * This activity takes parameters for a submission id (through intent or direct link), retrieves the
 * Submission object, and then displays the submission with its comments.
 */
class CommentsScreenSingle : BaseActivityAnim() {
    var comments: CommentsScreenSinglePagerAdapter? = null
    var np = false
    private var pager: ViewPager2? = null
    private var subreddit: String? = null
    private var name: String? = null
    private var context: String? = null
    private var contextNumber = 0
    private var doneTranslucent = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 14 && comments != null) {
            comments!!.notifyDataSetChanged()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        return if (SettingValues.commentVolumeNav) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> (comments!!.currentFragment as CommentPage?)!!.onKeyDown(keyCode, event)
                else -> super.dispatchKeyEvent(event)
            }
        } else super.dispatchKeyEvent(event)
    }

    public override fun onCreate(savedInstance: Bundle?) {
        disableSwipeBackLayout()
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.background = null
        super.onCreate(savedInstance)
        applyColorTheme()
        setContentView(R.layout.activity_slide)
        name = intent.extras!!.getString(EXTRA_SUBMISSION, "")
        subreddit = intent.extras!!.getString(EXTRA_SUBREDDIT, "")
        np = intent.extras!!.getBoolean(EXTRA_NP, false)
        context = intent.extras!!.getString(EXTRA_CONTEXT, "")
        contextNumber = intent.extras!!.getInt(EXTRA_CONTEXT_NUMBER, 5)
        if (subreddit == App.EMPTY_STRING) {
            AsyncGetSubredditName().execute(name)
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.activity_background, typedValue, true)
            val color = typedValue.data
            findViewById<View>(R.id.content_view).setBackgroundColor(color)
        } else {
            setupAdapter()
        }
        if (Authentication.isLoggedIn && Authentication.me == null) {
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    if (Authentication.reddit == null) {
                        Authentication(applicationContext)
                    } else {
                        try {
                            Authentication.me = Authentication.reddit.me()
                            Authentication.mod = Authentication.me!!.isMod
                            authentication.edit()
                                .putBoolean(App.SHARED_PREF_IS_MOD, Authentication.mod)
                                .apply()
                            if (App.notificationTime != -1) {
                                App.notifications = NotificationJobScheduler(this@CommentsScreenSingle)
                                App.notifications!!.start()
                            }
                            if (App.cachedData!!.contains("toCache")) {
                                App.autoCache = AutoCacheScheduler(this@CommentsScreenSingle)
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
                        } catch (e: Exception) {
                            Authentication(applicationContext)
                        }
                    }
                    return null
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private fun setupAdapter() {
        themeSystemBars(subreddit)
        setRecentBar(subreddit)
        pager = findViewById<View>(R.id.content_view) as ViewPager2
        comments = CommentsScreenSinglePagerAdapter(supportFragmentManager)
        pager!!.adapter = comments
        pager!!.setBackgroundColor(Color.TRANSPARENT)
        pager!!.currentItem = 1
        pager!!.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (position == 0 && positionOffsetPixels == 0) {
                    finish()
                }
                if (position == 0
                    && (pager!!.adapter as CommentsScreenSinglePagerAdapter?)!!.blankPage != null) {
                    (pager!!.adapter as CommentsScreenSinglePagerAdapter?)!!.blankPage!!.doOffset(positionOffset)
                    pager!!.setBackgroundColor(Palette.adjustAlpha(positionOffset * 0.7f))
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (!doneTranslucent) {
                    doneTranslucent = true
                    Utils.convertActivityToTranslucent(this@CommentsScreenSingle)
                }
            }
        })
    }

    var locked = false
    var archived = false
    var contest = false

    private inner class AsyncGetSubredditName : AsyncTask<String?, Void?, String?>() {
        override fun onPostExecute(s: String?) {
            subreddit = s
            setupAdapter()
        }

        override fun doInBackground(vararg params: String?): String? {
            return try {
                val s = Authentication.reddit!!.getSubmission(params[0])
                if (SettingValues.storeHistory) {
                    if (SettingValues.storeNSFWHistory && s.isNsfw || !s.isNsfw) {
                        addSeen(s.fullName)
                    }
                    setComments(RedditSubmission(s))
                }
                setHasSeenSubmission(listOf(s))
                locked = s.isLocked
                archived = s.isArchived
                contest = s.dataNode["contest_mode"].asBoolean()
                subreddit = if (s.subredditName == null) {
                    "Promoted"
                } else {
                    s.subredditName
                }
                subreddit
            } catch (e: Exception) {
                try {
                    runOnUiThread {
                        AlertDialog.Builder(this@CommentsScreenSingle)
                            .setTitle(R.string.submission_not_found)
                            .setMessage(R.string.submission_not_found_msg)
                            .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int -> finish() }
                            .setOnDismissListener { dialog: DialogInterface? -> finish() }
                            .show()
                    }
                } catch (ignored: Exception) {
                }
                null
            }
        }
    }

    inner class CommentsScreenSinglePagerAdapter internal constructor(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        var currentFragment: Fragment? = null
            private set
        var blankPage: BlankFragment? = null

        fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) {
            /*
            if (currentFragment !== obj) {
                currentFragment = obj as Fragment
            }
            super.setPrimaryItem(container, position, obj)
             */TODO("hmm")
        }

        override fun createFragment(i: Int): Fragment {
            return if (i == 0) {
                blankPage = BlankFragment()
                blankPage!!
            } else {
                val f: Fragment = CommentPage()
                val args = Bundle()
                if (name!!.contains("t3_")) name = name!!.substring(3)
                args.putString("id", name)
                args.putString("context", context)
                if (SettingValues.storeHistory) {
                    if (context != null && !context!!.isEmpty() && context != App.EMPTY_STRING) {
                        addSeen("t1_$context")
                    } else {
                        addSeen(name!!)
                    }
                }
                args.putBoolean("archived", archived)
                args.putBoolean("locked", locked)
                args.putBoolean("contest", contest)
                args.putInt("contextNumber", contextNumber)
                args.putString("subreddit", subreddit)
                args.putBoolean("single", intent.getBooleanExtra(EXTRA_LOADMORE, true))
                args.putBoolean("np", np)
                f.arguments = args
                f
            }
        }

        override fun getItemCount(): Int = 2
    }

    companion object {
        const val EXTRA_SUBREDDIT = "subreddit"
        const val EXTRA_CONTEXT = "context"
        const val EXTRA_CONTEXT_NUMBER = "contextNumber"
        const val EXTRA_SUBMISSION = "submission"
        const val EXTRA_NP = "np"
        const val EXTRA_LOADMORE = "loadmore"
    }
}
