package me.ccrama.redditslide.Activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Fragments.SubredditListView
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette

class Discover : BaseActivityAnim() {
    var adapter: DiscoverPagerAdapter? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = getMenuInflater()
        inflater.inflate(R.menu.menu_discover, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            R.id.search -> {
                run {
                    MaterialDialog(this@Discover)
                        .input(getString(R.string.discover_search), waitForPositiveButton = false,
                            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) { dialog: MaterialDialog, input: CharSequence ->
                            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = input.length >= 3
                        }
                        .positiveButton(res = R.string.search_all) { dialog: MaterialDialog ->
                            val inte: Intent =
                                Intent(this@Discover, SubredditSearch::class.java)
                            inte.putExtra(
                                "term",
                                dialog.getInputField().text.toString()
                            )
                            this@Discover.startActivity(inte)
                        }
                        .negativeButton(R.string.btn_cancel)
                        .show()
                }
                true
            }

            else -> false
        }
    }

    override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstance)
        applyColorTheme("")
        setContentView(R.layout.activity_multireddits)
        (findViewById<View>(R.id.drawer_layout) as DrawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        setupAppBar(R.id.toolbar, R.string.discover_title, true, false)
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        findViewById<View>(R.id.header).setBackgroundColor(Palette.getDefaultColor())
        val tabs: TabLayout = findViewById<View>(R.id.sliding_tabs) as TabLayout
        tabs.tabMode = TabLayout.MODE_FIXED
        tabs.setSelectedTabIndicatorColor(ColorPreferences(this@Discover).getColor("no sub"))
        val pager: ViewPager2 = findViewById<View>(R.id.content_view) as ViewPager2
        pager.adapter = DiscoverPagerAdapter(supportFragmentManager)
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab
            position
        }
        pager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                findViewById<View>(R.id.header).animate()
                    .translationY(0f)
                    .setInterpolator(LinearInterpolator())
                    .setDuration(180)
            }
        })
    }

    inner class DiscoverPagerAdapter internal constructor(fm: FragmentManager) :
        FragmentStateAdapter(fm, lifecycle) {
        override fun createFragment(i: Int): Fragment {
            val f: Fragment = SubredditListView()
            val args = Bundle()
            args.putString("id", if (i == 1) "trending" else "popular")
            f.arguments = args
            return f
        }

        override fun getItemCount(): Int {
            return 2
        }

        fun getPageTitle(position: Int): CharSequence? {
            return if (position == 0) {
                getString(R.string.discover_popular)
            } else {
                getString(R.string.discover_trending)
            }
        }
    }
}
