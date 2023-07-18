package me.ccrama.redditslide.Activities

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Fragments.ReadLaterView
import me.ccrama.redditslide.Visuals.ColorPreferences

class PostReadLater : BaseActivityAnim() {
    public override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstance)
        applyColorTheme()
        setContentView(R.layout.activity_read_later)
        setupAppBar(R.id.toolbar, "Read later", true, true)
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        val pager = findViewById<View>(R.id.content_view) as ViewPager2
        pager.adapter = ReadLaterPagerAdapter(supportFragmentManager)
    }

    private inner class ReadLaterPagerAdapter internal constructor(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        override fun createFragment(i: Int): Fragment {
            return ReadLaterView()
        }

        override fun getItemCount(): Int = 1
    }
}
