package me.ccrama.redditslide.Activities

import android.R
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.cocosw.bottomsheet.BottomSheet
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.imageDownloadButton
import me.ccrama.redditslide.Activities.AlbumPager.Companion.loadImage
import me.ccrama.redditslide.Adapters.ImageGridAdapter
import me.ccrama.redditslide.Fragments.BlankFragment
import me.ccrama.redditslide.Fragments.SubmissionsView.Companion.datachanged
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.DialogUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.ShareUtil
import me.ccrama.redditslide.views.ToolbarColorizeHelper
import java.io.File
import java.util.Arrays

/**
 * This is an extension of RedditAlbum.java which utilizes a
 * ViewPager for Reddit Gallery content instead of a RecyclerView (horizontal vs vertical).
 */
class RedditGalleryPager : FullScreenActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.home) {
            onBackPressed()
        }
        if (id == ltd.ucode.slide.R.id.vertical) {
            albumSwipe = false
            val i = Intent(this@RedditGalleryPager, RedditGallery::class.java)
            if (intent.hasExtra(MediaView.SUBMISSION_URL)) {
                i.putExtra(
                    MediaView.SUBMISSION_URL,
                    intent.getStringExtra(MediaView.SUBMISSION_URL)
                )
            }
            if (intent.hasExtra(SUBREDDIT)) {
                i.putExtra(SUBREDDIT, intent.getStringExtra(SUBREDDIT))
            }
            if (submissionTitle != null) i.putExtra(
                ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                submissionTitle
            )
            i.putExtras(intent)
            val urlsBundle = Bundle()
            urlsBundle.putSerializable(
                RedditGallery.GALLERY_URLS,
                ArrayList<GalleryImage>(images)
            )
            i.putExtras(urlsBundle)
            startActivity(i)
            finish()
        }
        if (id == ltd.ucode.slide.R.id.grid) {
            mToolbar!!.findViewById<View>(ltd.ucode.slide.R.id.grid).callOnClick()
        }
        if (id == ltd.ucode.slide.R.id.external) {
            openExternally(intent.extras!!.getString("url", ""))
        }
        if (id == ltd.ucode.slide.R.id.comments) {
            val adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
            finish()
            datachanged(adapterPosition)
            //getIntent().getStringExtra(MediaView.SUBMISSION_SUBREDDIT));
            //SubmissionAdapter.setOpen(this, getIntent().getStringExtra(MediaView.SUBMISSION_URL));
        }
        if (id == ltd.ucode.slide.R.id.download && images != null) {
            for ((index, elem: GalleryImage) in images!!.withIndex()) {
                doImageSave(false, elem.url, index)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3) {
            appRestart.edit().putBoolean("tutorialSwipe", true).apply()
        }
    }

    var subreddit: String? = null
    private var submissionTitle: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        theme.applyStyle(
            ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE),
            true
        )
        setContentView(ltd.ucode.slide.R.layout.album_pager)

        //Keep the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (intent.hasExtra(SUBREDDIT)) {
            subreddit = intent.getStringExtra(SUBREDDIT)
        }
        if (intent.hasExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)) {
            submissionTitle =
                intent.extras!!.getString(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)
        }
        mToolbar = findViewById<View>(ltd.ucode.slide.R.id.toolbar) as Toolbar
        mToolbar!!.setTitle(ltd.ucode.slide.R.string.type_album)
        ToolbarColorizeHelper.colorizeToolbar(mToolbar, Color.WHITE, this)
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        mToolbar!!.popupTheme =
            ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE)
        adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
        val url = intent.extras!!.getString("url", "")
        shareUrl = url
        if (!appRestart.contains("tutorialSwipe")) {
            startActivityForResult(Intent(this, SwipeTutorial::class.java), 3)
        }
        findViewById<View>(ltd.ucode.slide.R.id.progress).visibility = View.GONE
        images =
            intent.getSerializableExtra(RedditGallery.Companion.GALLERY_URLS) as ArrayList<GalleryImage>?
        p = findViewById<View>(ltd.ucode.slide.R.id.images_horizontal) as ViewPager
        if (supportActionBar != null) {
            supportActionBar!!.setSubtitle(1.toString() + "/" + images!!.size)
        }
        val adapter = GalleryViewPagerAdapter(supportFragmentManager)
        p!!.adapter = adapter
        p!!.currentItem = 1
        findViewById<View>(ltd.ucode.slide.R.id.grid).setOnClickListener {
            val l = layoutInflater
            val body = l.inflate(ltd.ucode.slide.R.layout.album_grid_dialog, null, false)
            val gridview = body.findViewById<GridView>(ltd.ucode.slide.R.id.images)
            gridview.adapter = ImageGridAdapter(this@RedditGalleryPager, true, images)
            val builder = AlertDialog.Builder(this@RedditGalleryPager)
                .setView(body)
            val d: Dialog = builder.create()
            gridview.onItemClickListener =
                AdapterView.OnItemClickListener { parent, v, position, id ->
                    p!!.currentItem = position + 1
                    d.dismiss()
                }
            d.show()
        }
        p!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (position != 0) {
                    if (supportActionBar != null) {
                        supportActionBar!!.setSubtitle((position).toString() + "/" + images!!.size)
                    }
                }
                if (position == 0 && positionOffset < 0.2) {
                    finish()
                }
            }
        })
        adapter.notifyDataSetChanged()
    }

    var p: ViewPager? = null
    private var images: List<GalleryImage>? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(ltd.ucode.slide.R.menu.album_pager, menu)
        adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
        if (adapterPosition < 0) {
            menu.findItem(ltd.ucode.slide.R.id.comments).isVisible = false
        }
        return true
    }

    private inner class GalleryViewPagerAdapter(m: FragmentManager?) :
        FragmentStatePagerAdapter(
            (m)!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
        override fun getItem(i: Int): Fragment {
            var i = i
            if (i == 0) {
                return BlankFragment()
            }
            i--
            val f: Fragment = ImageFullNoSubmission()
            val args = Bundle()
            args.putInt("page", i)
            f.arguments = args
            return f
        }

        override fun getCount(): Int {
            return if (images == null) {
                0
            } else images!!.size + 1
        }
    }

    fun showBottomSheetImage(
        contentUrl: String?, isGif: Boolean,
        index: Int
    ) {
        val attrs = intArrayOf(ltd.ucode.slide.R.attr.tintColor)
        val ta = obtainStyledAttributes(attrs)
        val color = ta.getColor(0, Color.WHITE)
        val external = resources.getDrawable(ltd.ucode.slide.R.drawable.ic_open_in_browser)
        val share = resources.getDrawable(ltd.ucode.slide.R.drawable.ic_share)
        val image = resources.getDrawable(ltd.ucode.slide.R.drawable.ic_image)
        val save = resources.getDrawable(ltd.ucode.slide.R.drawable.ic_download)
        val drawableSet = Arrays.asList(external, share, image, save)
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
        ta.recycle()
        val b = BottomSheet.Builder(this).title(contentUrl)
        b.sheet(2, external, getString(ltd.ucode.slide.R.string.open_externally))
        b.sheet(5, share, getString(ltd.ucode.slide.R.string.submission_link_share))
        if (!isGif) b.sheet(3, image, getString(ltd.ucode.slide.R.string.share_image))
        b.sheet(4, save, getString(ltd.ucode.slide.R.string.submission_save_image))
        b.listener { dialog, which ->
            when (which) {
                (2) -> {
                    openExternally((contentUrl)!!)
                }

                (3) -> {
                    ShareUtil.shareImage(contentUrl, this@RedditGalleryPager)
                }

                (5) -> {
                    defaultShareText("", contentUrl, this@RedditGalleryPager)
                }

                (4) -> {
                    doImageSave(isGif, contentUrl, index)
                }
            }
        }
        b.show()
    }

    fun doImageSave(isGif: Boolean, contentUrl: String?, index: Int) {
        if (!isGif) {
            if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
                showFirstDialog()
            } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
                showErrorDialog()
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

    class ImageFullNoSubmission : Fragment() {
        private var i = 0
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val rootView = inflater.inflate(
                ltd.ucode.slide.R.layout.album_image_pager,
                container,
                false
            ) as ViewGroup
            val current = (activity as RedditGalleryPager?)!!.images!![i]
            val url = current.url
            var lq = false
            if (SettingValues.loadImageLq && (SettingValues.lowResAlways || ((!NetworkUtil.isConnectedWifi(
                    activity
                )
                        && SettingValues.lowResMobile)))
            ) {
                val lqurl = (url.substring(0, url.lastIndexOf("."))
                        + (if (SettingValues.lqLow) "m" else (if (SettingValues.lqMid) "l" else "h"))
                        + url.substring(url.lastIndexOf(".")))
                loadImage(
                    rootView,
                    this,
                    lqurl,
                    (activity as RedditGalleryPager?)!!.images!!.size == 1
                )
                lq = true
            } else {
                loadImage(
                    rootView,
                    this,
                    url,
                    (activity as RedditGalleryPager?)!!.images!!.size == 1
                )
            }
            run {
                rootView.findViewById<View>(ltd.ucode.slide.R.id.more)
                    .setOnClickListener {
                        (activity as RedditGalleryPager?)!!.showBottomSheetImage(
                            url,
                            false,
                            i
                        )
                    }
                run {
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.save)
                        .setOnClickListener {
                            (activity as RedditGalleryPager?)!!.doImageSave(
                                false,
                                url,
                                i
                            )
                        }
                    if (!imageDownloadButton) {
                        rootView.findViewById<View>(ltd.ucode.slide.R.id.save).visibility = View.INVISIBLE
                    }
                }
                rootView.findViewById<View>(ltd.ucode.slide.R.id.panel).visibility = View.GONE
                (rootView.findViewById<View>(ltd.ucode.slide.R.id.margin)).setPadding(0, 0, 0, 0)
            }
            rootView.findViewById<View>(ltd.ucode.slide.R.id.hq).visibility = View.GONE
            if (requireActivity().intent.hasExtra(MediaView.SUBMISSION_URL)) {
                rootView.findViewById<View>(ltd.ucode.slide.R.id.comments).setOnClickListener {
                    requireActivity().finish()
                    datachanged(adapterPosition)
                }
            } else {
                rootView.findViewById<View>(ltd.ucode.slide.R.id.comments).visibility = View.GONE
            }
            return rootView
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val bundle = this.arguments
            i = bundle!!.getInt("page", 0)
        }
    }

    private fun showFirstDialog() {
        runOnUiThread { DialogUtil.showFirstDialog(this@RedditGalleryPager) { _, folder -> onFolderSelection(folder) } }
    }

    private fun showErrorDialog() {
        runOnUiThread { DialogUtil.showErrorDialog(this@RedditGalleryPager) { _, folder -> onFolderSelection(folder) } }
    }

    private fun onFolderSelection(
        folder: File
    ) {
        appRestart.edit().putString("imagelocation", folder.absolutePath).apply()
        Toast.makeText(
            this, (
                    getString(
                        ltd.ucode.slide.R.string.settings_set_image_location,
                        folder.absolutePath
                    )
                            + folder.absolutePath), Toast.LENGTH_LONG
        ).show()
    }


    companion object {
        private var adapterPosition = 0
        val SUBREDDIT = "subreddit"
    }
}
