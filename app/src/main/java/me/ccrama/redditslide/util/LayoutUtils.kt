package me.ccrama.redditslide.util

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.ViewTreeObserver
import android.widget.TextView
import com.google.android.material.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import ltd.ucode.slide.App
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.dualPortrait

object LayoutUtils {
    /**
     * Method to scroll the TabLayout to a specific index
     *
     * @param tabLayout   the tab layout
     * @param tabPosition index to scroll to
     */
    fun scrollToTabAfterLayout(tabLayout: TabLayout?, tabPosition: Int) {
        //from http://stackoverflow.com/a/34780589/3697225
        if (tabLayout != null) {
            val observer = tabLayout.viewTreeObserver
            if (observer.isAlive) {
                observer.dispatchOnGlobalLayout() // In case a previous call is waiting when this call is made
                observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        tabLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        try { // TODO: remove
                            tabLayout.getTabAt(tabPosition)!!.select()
                        } catch (e: NullPointerException) {
                            SettingValues.single = true
                            SettingValues.commentPager = true
                            throw e
                        }
                    }
                })
            }
        }
    }

    @JvmStatic fun showSnackbar(s: Snackbar) {
        val view = s.view
        val tv = view.findViewById<TextView>(R.id.snackbar_text)
        tv.setTextColor(Color.WHITE)
        s.show()
    }

    // <ccrama> Should this go here in this class??? I don't think it should but idk where else to put it
    fun getNumColumns(orientation: Int, activity: Activity): Int {
        var singleColumnMultiWindow = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            singleColumnMultiWindow = activity.isInMultiWindowMode && SettingValues.singleColumnMultiWindow
        }
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE && SettingValues.isPro && !singleColumnMultiWindow) {
            App.dpWidth
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT && dualPortrait) {
            2
        } else {
            1
        }
    }
}
