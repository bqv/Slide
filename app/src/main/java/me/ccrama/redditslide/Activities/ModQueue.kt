package me.ccrama.redditslide.Activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Fragments.InboxPage
import me.ccrama.redditslide.Fragments.ModLog
import me.ccrama.redditslide.Fragments.ModPage
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette

class ModQueue : BaseActivityAnim() {
    var adapter: ModQueuePagerAdapter? = null

    public override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstance)
        applyColorTheme("")
        setContentView(R.layout.activity_inbox)
        setupAppBar(R.id.toolbar, R.string.drawer_moderation, true, true)
        val tabs = findViewById<View>(R.id.sliding_tabs) as TabLayout
        tabs.tabMode = TabLayout.MODE_SCROLLABLE
        tabs.setSelectedTabIndicatorColor(ColorPreferences(this@ModQueue).getColor("no sub"))
        val header = findViewById<View>(R.id.header)
        val pager = findViewById<ViewPager2>(R.id.content_view)
        findViewById<View>(R.id.header).setBackgroundColor(Palette.getDefaultColor())
        pager.adapter = ModQueuePagerAdapter(supportFragmentManager)
        TabLayoutMediator(tabs, pager) { tab, position ->
            header.animate()
                .translationY(0f)
                .setInterpolator(LinearInterpolator()).duration = 180
        }
    }

    inner class ModQueuePagerAdapter internal constructor(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        private var mCurrentFragment: Fragment? = null

        fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) {
            /*
            if (mCurrentFragment !== obj) {
                mCurrentFragment = obj as Fragment
            }
            super.setPrimaryItem(container, position, obj)
             */TODO("hmm")
        }

        override fun createFragment(i: Int): Fragment {
            val f: Fragment
            val args = Bundle()

            return when (i) {
                0 -> {
                    f = InboxPage()
                    args.putString("id", "moderator/unread")
                    f.setArguments(args)
                    f
                }

                1 -> {
                    f = InboxPage()
                    args.putString("id", "moderator")
                    f.setArguments(args)
                    f
                }

                2 -> {
                    f = ModPage()
                    args.putString("id", "modqueue")
                    args.putString("subreddit", "mod")
                    f.setArguments(args)
                    f
                }

                3 -> {
                    f = ModPage()
                    args.putString("id", "unmoderated")
                    args.putString("subreddit", "mod")
                    f.setArguments(args)
                    f
                }

                4 -> {
                    f = ModLog()
                    f.setArguments(args)
                    f
                }

                else -> {
                    f = ModPage()
                    args.putString("id", "modqueue")
                    args.putString("subreddit", UserSubscriptions.modOf!![i - 5])
                    f.setArguments(args)
                    f
                }
            }
        }

        override fun getItemCount(): Int = if (UserSubscriptions.modOf == null) 2 else UserSubscriptions.modOf!!.size + 5

        fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> getString(R.string.mod_mail_unread)
                1 -> getString(R.string.mod_mail)
                2 -> getString(R.string.mod_modqueue)
                3 -> getString(R.string.mod_unmoderated)
                4 -> getString(R.string.mod_log)
                else -> UserSubscriptions.modOf!![position - 5]
            }
        }
    }
}
