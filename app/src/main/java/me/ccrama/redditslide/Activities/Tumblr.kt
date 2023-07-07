package me.ccrama.redditslide.Activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.Adapters.TumblrView
import me.ccrama.redditslide.Fragments.BlankFragment
import me.ccrama.redditslide.Fragments.SubmissionsView.Companion.datachanged
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.Tumblr.Photo
import me.ccrama.redditslide.Tumblr.TumblrUtils.GetTumblrPostWithCallback
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.DialogUtil
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.views.PreCachingLayoutManager
import me.ccrama.redditslide.views.ToolbarColorizeHelper
import java.io.File

/**
 * This class is responsible for accessing the Tumblr api to get
 * the image-related json data from a URL. It extends FullScreenActivity and supports swipe from
 * anywhere.
 */
class Tumblr : FullScreenActivity() {
    private var images: List<Photo>? = null
    private var adapterPosition: Int = 0
    var subreddit: String? = null

    private fun onFolderSelection(folder: File) {
        appRestart.edit().putString("imagelocation", folder.absolutePath).apply()
        Toast.makeText(
            this,
            getString(R.string.settings_set_image_location, folder.absolutePath),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
        }
        if (id == R.id.slider) {
            albumSwipe = true
            val i: Intent = Intent(this@Tumblr, TumblrPager::class.java)
            val adapterPosition: Int = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
            i.putExtra(MediaView.ADAPTER_POSITION, adapterPosition)
            if (intent.hasExtra(MediaView.SUBMISSION_URL)) {
                i.putExtra(
                    MediaView.SUBMISSION_URL,
                    intent.getStringExtra(MediaView.SUBMISSION_URL)
                )
            }
            if (intent.hasExtra(SUBREDDIT)) {
                i.putExtra(SUBREDDIT, intent.getStringExtra(SUBREDDIT))
            }
            i.putExtra("url", url)
            startActivity(i)
            finish()
        }
        if (id == R.id.grid) {
            mToolbar!!.findViewById<View>(R.id.grid).callOnClick()
        }
        if (id == R.id.comments) {
            datachanged(adapterPosition)
            finish()
        }
        if (id == R.id.external) {
            openExternally((url)!!)
        }
        if (id == R.id.download) {
            for (elem: Photo in images!!) {
                doImageSave(false, elem.originalSize.url)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun doImageSave(isGif: Boolean, contentUrl: String?) {
        if (!isGif) {
            if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
                DialogUtil.showFirstDialog(this@Tumblr) { _, folder -> onFolderSelection(folder) }
            } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
                DialogUtil.showErrorDialog(this@Tumblr) { _, folder -> onFolderSelection(folder) }
            } else {
                val i: Intent = Intent(this, ImageDownloadNotificationService::class.java)
                i.putExtra("actuallyLoaded", contentUrl)
                if (subreddit != null && !subreddit!!.isEmpty()) i.putExtra("subreddit", subreddit)
                startService(i)
            }
        } else {
            MediaView.doOnClick!!.run()
        }
    }

    var url: String? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.album_vertical, menu)
        adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
        if (adapterPosition < 0) {
            menu.findItem(R.id.comments).isVisible = false
        }
        return true
    }

    @JvmField
    var album: TumblrPagerAdapter? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        theme.applyStyle(
            ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE),
            true
        )
        setContentView(R.layout.album)

        //Keep the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val pager: ViewPager = findViewById<View>(R.id.images) as ViewPager
        album = TumblrPagerAdapter(supportFragmentManager)
        pager.adapter = album
        pager.currentItem = 1
        if (intent.hasExtra(SUBREDDIT)) {
            subreddit = intent.getStringExtra(SUBREDDIT)
        }
        pager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (position == 0 && positionOffsetPixels == 0) {
                    finish()
                }
                if ((position == 0
                            && (pager.adapter as TumblrPagerAdapter?)!!.blankPage != null)
                ) {
                    if ((pager.adapter as TumblrPagerAdapter?)!!.blankPage != null) {
                        (pager.adapter as TumblrPagerAdapter?)!!.blankPage!!.doOffset(
                            positionOffset
                        )
                    }
                    (pager.adapter as TumblrPagerAdapter?)!!.blankPage!!.realBack.setBackgroundColor(
                        Palette.adjustAlpha(positionOffset * 0.7f)
                    )
                }
            }
        }
        )
        if (!appRestart.contains("tutorialSwipe")) {
            startActivityForResult(Intent(this, SwipeTutorial::class.java), 3)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3) {
            appRestart.edit().putBoolean("tutorialSwipe", true).apply()
        }
    }

    class TumblrPagerAdapter constructor(fm: FragmentManager?) : FragmentStatePagerAdapter(
        (fm)!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        var blankPage: BlankFragment? = null
        @JvmField
        var album: AlbumFrag? = null
        override fun getItem(i: Int): Fragment {
            if (i == 0) {
                blankPage = BlankFragment()
                return blankPage!!
            } else {
                album = AlbumFrag()
                return album!!
            }
        }

        override fun getCount(): Int {
            return 2
        }
    }

    class AlbumFrag : Fragment() {
        var rootView: View? = null
        @JvmField
        var recyclerView: RecyclerView? = null
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            rootView = inflater.inflate(R.layout.fragment_verticalalbum, container, false)
            val mLayoutManager = PreCachingLayoutManager(activity)
            recyclerView = rootView!!.findViewById(R.id.images)
            recyclerView!!.layoutManager = mLayoutManager
            (activity as Tumblr?)!!.url = requireActivity().intent.extras!!
                .getString(EXTRA_URL, "")
            (activity as BaseActivity?)!!.shareUrl = (activity as Tumblr?)!!.url
            LoadIntoRecycler((activity as Tumblr?)!!.url!!, requireActivity()).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR
            )
            (activity as Tumblr?)!!.mToolbar = rootView!!.findViewById(R.id.toolbar)
            (activity as Tumblr?)!!.mToolbar!!.setTitle(R.string.type_album)
            ToolbarColorizeHelper.colorizeToolbar(
                (activity as Tumblr?)!!.mToolbar, Color.WHITE,
                (activity)
            )
            (activity as Tumblr?)!!.setSupportActionBar((activity as Tumblr?)!!.mToolbar)
            (activity as Tumblr?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            (activity as Tumblr?)!!.mToolbar!!.popupTheme = ColorPreferences(activity).getDarkThemeSubreddit(
                ColorPreferences.FONT_STYLE
            )
            return rootView
        }

        inner class LoadIntoRecycler constructor(var url: String, baseActivity: Activity) :
            GetTumblrPostWithCallback(
                url, baseActivity
            ) {
            override fun onError() {
                val i: Intent = Intent(activity, Website::class.java)
                i.putExtra(LinkUtil.EXTRA_URL, url)
                startActivity(i)
                activity!!.finish()
            }

            override fun doWithData(jsonElements: List<Photo>) {
                super.doWithData(jsonElements)
                if (activity != null) {
                    activity!!.findViewById<View>(R.id.progress).visibility = View.GONE
                    (activity as Tumblr?)!!.images = ArrayList(jsonElements)
                    val adapter: TumblrView = TumblrView(
                        baseActivity,
                        (activity as Tumblr?)!!.images,
                        activity!!.findViewById<View>(R.id.toolbar).height,
                        (activity as Tumblr?)!!.subreddit
                    )
                    recyclerView!!.adapter = adapter
                }
            }
        }
    }

    companion object {
        val EXTRA_URL: String = "url"
        @JvmField
        val SUBREDDIT: String = "subreddit"
    }
}
