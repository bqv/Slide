package me.ccrama.redditslide.Activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.Adapters.RedditGalleryView
import me.ccrama.redditslide.Fragments.BlankFragment
import me.ccrama.redditslide.Fragments.SubmissionsView.Companion.datachanged
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.DialogUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.views.PreCachingLayoutManager
import me.ccrama.redditslide.views.ToolbarColorizeHelper
import java.io.File

class RedditGallery : FullScreenActivity() {
    private var images: List<GalleryImage>? = null
    private var adapterPosition = 0

    private fun onFolderSelection(folder: File) {
        appRestart.edit().putString("imagelocation", folder.absolutePath).apply()
        Toast.makeText(
            this,
            getString(R.string.settings_set_image_location, folder.absolutePath),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
        }
        if (id == R.id.slider) {
            albumSwipe = true
            val i = Intent(this@RedditGallery, RedditGalleryPager::class.java)
            val adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
            i.putExtra(MediaView.ADAPTER_POSITION, adapterPosition)
            if (intent.hasExtra(MediaView.SUBMISSION_URL)) {
                i.putExtra(
                    MediaView.SUBMISSION_URL,
                    intent.getStringExtra(MediaView.SUBMISSION_URL)
                )
            }
            if (subreddit != null && !subreddit!!.isEmpty()) i.putExtra(
                RedditGalleryPager.Companion.SUBREDDIT,
                subreddit
            )
            if (submissionTitle != null) i.putExtra(
                ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                submissionTitle
            )
            val urlsBundle = Bundle()
            urlsBundle.putSerializable(GALLERY_URLS, ArrayList(images))
            i.putExtras(urlsBundle)
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
            openExternally(url!!)
        }
        if (id == R.id.download) {
            var index = 0
            for (elem in images!!) {
                doImageSave(false, elem.url, index)
                index++
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun doImageSave(isGif: Boolean, contentUrl: String?, index: Int) {
        if (!isGif) {
            if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
                DialogUtil.showFirstDialog(this@RedditGallery) { _, folder -> onFolderSelection(folder) }
            } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
                DialogUtil.showErrorDialog(this@RedditGallery) { _, folder -> onFolderSelection(folder) }
            } else {
                val i = Intent(this, ImageDownloadNotificationService::class.java)
                i.putExtra("actuallyLoaded", contentUrl)
                if (subreddit != null && !subreddit!!.isEmpty()) i.putExtra("subreddit", subreddit)
                if (submissionTitle != null) i.putExtra(
                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                    submissionTitle
                )
                i.putExtra("index", index)
                startService(i)
            }
        } else {
            MediaView.doOnClick!!.run()
        }
    }

    var url: String? = null
    var subreddit: String? = null
    private var submissionTitle: String? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.album_vertical, menu)
        adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
        if (adapterPosition < 0) {
            menu.findItem(R.id.comments).isVisible = false
        }
        return true
    }

    var album: RedditGalleryPagerAdapter? = null
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
        if (intent.hasExtra(SUBREDDIT)) {
            subreddit = intent.extras!!.getString(SUBREDDIT)
        }
        if (intent.hasExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)) {
            submissionTitle =
                intent.extras!!.getString(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)
        }
        val pager = findViewById<View>(R.id.images) as ViewPager2
        album = RedditGalleryPagerAdapter(supportFragmentManager)
        pager.adapter = album
        pager.currentItem = 1
        pager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (position == 0 && positionOffsetPixels == 0) {
                    finish()
                }
                if (position == 0
                    && (pager.adapter as RedditGalleryPagerAdapter?)!!.blankPage != null
                ) {
                    (pager.adapter as RedditGalleryPagerAdapter?)!!.blankPage!!.doOffset(positionOffset)
                    (pager.adapter as RedditGalleryPagerAdapter?)!!.blankPage!!.realBack.setBackgroundColor(
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

    inner class RedditGalleryPagerAdapter internal constructor(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        var blankPage: BlankFragment? = null
        var album: AlbumFrag? = null
        override fun createFragment(i: Int): Fragment {
            return if (i == 0) {
                blankPage = BlankFragment()
                blankPage!!
            } else {
                album = AlbumFrag()
                album!!
            }
        }

        override fun getItemCount(): Int {
            return 2
        }
    }

    class AlbumFrag : Fragment() {
        var rootView: View? = null
        var recyclerView: RecyclerView? = null

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            rootView = inflater.inflate(R.layout.fragment_verticalalbum, container, false)
            val mLayoutManager = PreCachingLayoutManager(activity)
            recyclerView = rootView!!.findViewById(R.id.images)
            recyclerView!!.layoutManager = mLayoutManager
            val galleryActivity = activity as RedditGallery?
            galleryActivity!!.images =
                requireActivity().intent.getSerializableExtra(GALLERY_URLS) as ArrayList<GalleryImage>?
            (activity as BaseActivity?)!!.shareUrl = galleryActivity.url
            galleryActivity.mToolbar = rootView!!.findViewById(R.id.toolbar)
            galleryActivity.mToolbar!!.setTitle(R.string.type_album)
            ToolbarColorizeHelper.colorizeToolbar(
                galleryActivity.mToolbar, Color.WHITE,
                activity
            )
            galleryActivity.setSupportActionBar(galleryActivity.mToolbar)
            galleryActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            galleryActivity.mToolbar!!.popupTheme = ColorPreferences(activity).getDarkThemeSubreddit(
                ColorPreferences.FONT_STYLE
            )
            rootView!!.post {
                rootView!!.findViewById<View>(R.id.progress).visibility = View.GONE
                val adapter = RedditGalleryView(
                    galleryActivity, galleryActivity.images,
                    rootView!!.findViewById<View>(R.id.toolbar).height, galleryActivity.subreddit,
                    galleryActivity.submissionTitle
                )
                recyclerView!!.adapter = adapter
            }
            return rootView
        }
    }

    companion object {
        const val SUBREDDIT = "subreddit"
        const val GALLERY_URLS = "galleryurls"
    }
}
