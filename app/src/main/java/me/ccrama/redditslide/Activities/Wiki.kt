package me.ccrama.redditslide.Activities

import android.content.DialogInterface
import android.content.res.TypedArray
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Fragments.WikiPage
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.views.ToggleSwipeViewPager
import me.ccrama.redditslide.views.disableSwipingUntilRelease
import net.dean.jraw.managers.WikiManager

class Wiki : BaseActivityAnim(), WikiPage.WikiPageListener {
    private lateinit var tabs: TabLayout
    private lateinit var pager: ToggleSwipeViewPager
    private lateinit var subreddit: String
    private var adapter: WikiPagerAdapter? = null
    private var pages: MutableList<String>? = null
    private lateinit var page: String
    @JvmField var wiki: WikiManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        subreddit = intent.extras!!.getString(EXTRA_SUBREDDIT, "")
        shareUrl = "https://reddit.com/r/$subreddit/wiki/"
        applyColorTheme(subreddit)
        createCustomCss()
        createCustomJavaScript()
        setContentView(R.layout.activity_slidetabs)
        setupSubredditAppBar(R.id.toolbar, "/r/$subreddit wiki", true, subreddit)
        if (intent.hasExtra(EXTRA_PAGE)) {
            page = intent.extras!!.getString(EXTRA_PAGE)!!
            LogUtil.v("Page is $page")
        } else {
            page = "index"
        }
        tabs = findViewById<TabLayout>(R.id.sliding_tabs)
        tabs.tabMode = TabLayout.MODE_SCROLLABLE
        tabs.setSelectedTabIndicatorColor(ColorPreferences(this@Wiki).getColor("no sub"))
        pager = findViewById<ToggleSwipeViewPager>(R.id.content_view)
        findViewById<View>(R.id.header).setBackgroundColor(Palette.getColor(subreddit))
        AsyncGetWiki().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun createCustomCss() {
        val customCssBuilder = StringBuilder()
        customCssBuilder.append("<style>")
        val ta: TypedArray = obtainStyledAttributes(intArrayOf(R.attr.activity_background, R.attr.fontColor, R.attr.colorAccent))
        customCssBuilder.append("html { ")
            .append("background: ").append(getHexFromColorInt(ta.getColor(0, Color.WHITE))).append(";")
            .append("color: ").append(getHexFromColorInt(ta.getColor(1, Color.BLACK))).append(";")
            .append("; }")
        customCssBuilder.append("a { ")
            .append("color: ").append(getHexFromColorInt(ta.getColor(2, Color.BLUE))).append(";")
            .append("; }")
        ta.recycle()
        customCssBuilder.append("table, code { display: block; overflow-x: scroll; }")
        customCssBuilder.append("table { white-space: nowrap; }")
        customCssBuilder.append("</style>")
        globalCustomCss = customCssBuilder.toString()
    }

    private fun createCustomJavaScript() {
        globalCustomJavaScript = "<script type=\"text/javascript\">" +
            "window.addEventListener('touchstart', function onSlideUserTouch(e) {" +
            "var element = e.target;" +
            "while(element) {" +
            "if(element.tagName && (element.tagName.toLowerCase() === 'table' || element.tagName.toLowerCase() === 'code')) {" +
            "Slide.overflowTouched();" +
            "return;" +
            "} else {" +
            "element = element.parentNode;" +
            "}}}, false)" + "</script>"
    }

    override fun embeddedWikiLinkClicked(wikiPageTitle: String) {
        if (pages!!.contains(wikiPageTitle)) {
            pager!!.currentItem = pages!!.indexOf(wikiPageTitle)
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.page_not_found)
                .setMessage(R.string.page_does_not_exist)
                .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                .create()
                .show()
        }
    }

    override fun overflowTouched() {
        pager.disableSwipingUntilRelease()
    }

    private inner class AsyncGetWiki : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            wiki = WikiManager(Authentication.reddit)
            try {
                pages = wiki!!.getPages(subreddit)
                val toRemove: MutableList<String?> = ArrayList()
                for (s in pages!!) {
                    if (s!!.startsWith("config")) {
                        toRemove.add(s)
                    }
                }
                pages!!.removeAll(toRemove)
                adapter = WikiPagerAdapter(supportFragmentManager)
            } catch (e: Exception) {
                runOnUiThread(Runnable {
                    try {
                        AlertDialog.Builder(this@Wiki)
                            .setTitle(R.string.wiki_err)
                            .setMessage(R.string.wiki_err_msg)
                            .setPositiveButton(R.string.btn_close) { dialog: DialogInterface, which: Int ->
                                dialog.dismiss()
                                finish()
                            }
                            .setOnDismissListener { dialog: DialogInterface? -> finish() }
                            .show()
                    } catch (ignored: Exception) {
                    }
                })
            }
            return null
        }

        public override fun onPostExecute(d: Void?) {
            if (adapter != null) {
                pager!!.adapter = adapter!!
                TabLayoutMediator(tabs, pager!!) { tab, position ->
                    tab
                    position
                }.attach()
                if (pages!!.contains(page)) {
                    pager!!.currentItem = pages!!.indexOf(page)
                }
            } else {
                try {
                    AlertDialog.Builder(this@Wiki)
                        .setTitle(R.string.wiki_err)
                        .setMessage(R.string.wiki_err_msg)
                        .setPositiveButton(R.string.btn_close) { dialog: DialogInterface, which: Int ->
                            dialog.dismiss()
                            finish()
                        }
                        .setOnDismissListener { dialog: DialogInterface? -> finish() }
                        .show()
                } catch (_: Exception) {
                }
            }
        }
    }

    private inner class WikiPagerAdapter(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        override fun createFragment(i: Int): Fragment {
            val f = WikiPage()
            f.setListener(this@Wiki)
            val args = Bundle()
            args.putString("title", pages!![i])
            args.putString("subreddit", subreddit)
            f.arguments = args
            return f
        }

        override fun getItemCount(): Int = if (pages == null) {
            1
        } else {
            pages!!.size
        }

        fun getPageTitle(position: Int): CharSequence? {
            return pages!![position]
        }
    }

    companion object {
        const val EXTRA_SUBREDDIT = "subreddit"
        const val EXTRA_PAGE = "page"
        @JvmStatic var globalCustomCss: String? = null
            private set
        @JvmStatic var globalCustomJavaScript: String? = null
            private set

        private fun getHexFromColorInt(@ColorInt colorInt: Int): String {
            return String.format("#%06X", 0xFFFFFF and colorInt)
        }
    }
}
