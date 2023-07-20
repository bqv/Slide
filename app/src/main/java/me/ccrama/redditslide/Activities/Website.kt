package me.ccrama.redditslide.Activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.webkit.WebViewClientCompat
import ltd.ucode.network.ContentType
import ltd.ucode.network.ContentType.Companion.getContentType
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.SettingValues.cookies
import ltd.ucode.slide.ui.BaseActivityAnim
import ltd.ucode.slide.ui.submissionView.SubmissionsViewFragment.Companion.datachanged
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.PostMatch.openExternal
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.AdBlocker
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.util.LinkUtil.tryOpenWithVideoPlugin
import me.ccrama.redditslide.util.LogUtil
import java.net.URI
import java.net.URISyntaxException

class Website : BaseActivityAnim() {
    var v: WebView? = null
    var url: String? = null
    var subredditColor = 0
    var client: MyWebViewClient? = null
    var webClient: AdBlockWebViewClient? = null
    var p: ProgressBar? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_website, menu)

        //   if (mShowInfoButton) menu.findItem(R.id.action_info).setVisible(true);
        //   else menu.findItem(R.id.action_info).setVisible(false);
        val item = menu.findItem(R.id.store_cookies)
        item.isChecked = cookies
        if (!intent.hasExtra(LinkUtil.ADAPTER_POSITION)) {
            menu.findItem(R.id.comments).isVisible = false
        }
        return true
    }

    override fun onBackPressed() {
        if (v!!.canGoBack()) {
            v!!.goBack()
        } else if (!isFinishing) {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }

            R.id.refresh -> {
                v!!.reload()
                return true
            }

            R.id.back -> {
                v!!.goBack()
                return true
            }

            R.id.comments -> {
                val commentUrl = intent.extras!!.getInt(LinkUtil.ADAPTER_POSITION)
                finish()
                datachanged(commentUrl)
            }

            R.id.external -> {
                val inte = Intent(this, MakeExternal::class.java)
                inte.putExtra("url", url)
                startActivity(inte)
                return true
            }

            R.id.store_cookies -> {
                cookies = !cookies
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
                overridePendingTransition(0, 0)
                return true
            }

            R.id.read -> {
                v!!.evaluateJavascript(
                    "(function(){return \"<html>\" + document.documentElement.innerHTML + \"</html>\";})();"
                ) { html ->
                    val i = Intent(this@Website, ReaderMode::class.java)
                    if (html != null && !html.isEmpty()) {
                        ReaderMode.html = html
                        LogUtil.v(html)
                    } else {
                        ReaderMode.html = ""
                        i.putExtra("url", v!!.url)
                    }
                    i.putExtra(LinkUtil.EXTRA_COLOR, subredditColor)
                    startActivity(i)
                }
                return true
            }

            R.id.chrome -> {
                openExternally(v!!.url!!)
                return true
            }

            R.id.share -> {
                defaultShareText(v!!.title, v!!.url, this@Website)
                return true
            }
        }
        return false
    }

    //Stop audio
    override fun finish() {
        super.finish()
        v!!.loadUrl("about:blank")
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        applyColorTheme("")
        setContentView(R.layout.activity_web)
        url = intent.extras!!.getString(LinkUtil.EXTRA_URL, "")
        subredditColor = intent.extras!!.getInt(LinkUtil.EXTRA_COLOR, Palette.getDefaultColor())
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
        setupAppBar(R.id.toolbar, "", true, subredditColor, R.id.appbar)
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        p = findViewById<View>(R.id.progress) as ProgressBar
        v = findViewById<View>(R.id.web) as WebView
        client = MyWebViewClient()
        webClient = AdBlockWebViewClient()
        if (!cookies) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            cookieManager.setAcceptCookie(false)
            val ws = v!!.settings
            ws.saveFormData = false
            ws.savePassword = false
        }


        /* todo in the future, drag left and right to go back and forward in history

        IOverScrollDecor decor = new HorizontalOverScrollBounceEffectDecorator(new WebViewOverScrollDecoratorAdapter(v));

        decor.setOverScrollStateListener(new IOverScrollStateListener() {
             @Override
             public void onOverScrollStateChange(IOverScrollDecor decor, int oldState, int newState) {
                 switch (newState) {
                     case IOverScrollState.STATE_IDLE:
                         // No over-scroll is in effect.
                         break;
                     case IOverScrollState.STATE_DRAG_START_SIDE:
                         break;
                     case IOverScrollState.STATE_DRAG_END_SIDE:
                         break;
                     case IOverScrollState.STATE_BOUNCE_BACK:
                         if (oldState == IOverScrollState.STATE_DRAG_START_SIDE) {
                             if(v.canGoBack())
                                 v.goBack();
                         } else { // i.e. (oldState == STATE_DRAG_END_SIDE)
                             if(v.canGoForward())
                                 v.goForward();
                         }
                         break;
                 }
             }
         });
         */v!!.webChromeClient = client
        v!!.webViewClient = webClient!!
        v!!.settings.builtInZoomControls = true
        v!!.settings.displayZoomControls = false
        v!!.settings.javaScriptEnabled = true
        v!!.settings.loadWithOverviewMode = true
        v!!.settings.useWideViewPort = true
        v!!.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength -> //Downloads using download manager on default browser
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        v!!.loadUrl(url!!)
    }

    fun setValue(newProgress: Int) {
        p!!.progress = newProgress
        if (newProgress == 100) {
            p!!.visibility = View.GONE
        } else if (p!!.visibility == View.GONE) {
            p!!.visibility = View.VISIBLE
        }
    }

    inner class MyWebViewClient : WebChromeClient() {
        private var fullscreenCallback: CustomViewCallback? = null
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            this@Website.setValue(newProgress)
            super.onProgressChanged(view, newProgress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            try {
                super.onReceivedTitle(view, title)
                if (supportActionBar != null) {
                    if (!title.isEmpty()) {
                        if (supportActionBar != null) {
                            supportActionBar!!.title = title
                            shareUrl = url
                            if (url!!.contains("/")) {
                                supportActionBar!!.subtitle = getDomainName(url)
                            }
                            currentURL = url
                        }
                    } else {
                        if (supportActionBar != null) {
                            supportActionBar!!.title = getDomainName(url)
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            fullscreenCallback = callback
            findViewById<View>(R.id.appbar).visibility = View.INVISIBLE
            val attributes = window.attributes
            attributes.flags = attributes.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
            attributes.flags = attributes.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            window.attributes = attributes
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            val fullscreenViewFrame = findViewById<View>(R.id.web_fullscreen) as FrameLayout
            fullscreenViewFrame.addView(view)
        }

        override fun onHideCustomView() {
            val fullscreenViewFrame = findViewById<View>(R.id.web_fullscreen) as FrameLayout
            fullscreenViewFrame.removeAllViews()
            val attributes = window.attributes
            attributes.flags = attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
            attributes.flags = attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
            window.attributes = attributes
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            findViewById<View>(R.id.appbar).visibility = View.VISIBLE
            if (fullscreenCallback != null) {
                fullscreenCallback!!.onCustomViewHidden()
                fullscreenCallback = null
            }
        }
    }

    var currentURL: String? = null

    //Method adapted from http://www.hidroh.com/2016/05/19/hacking-up-ad-blocker-android/
    inner class AdBlockWebViewClient : WebViewClientCompat() {
        private val loadedUrls: MutableMap<String, Boolean> = HashMap()
        override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
            val ad: Boolean
            if (!loadedUrls.containsKey(url)) {
                ad = AdBlocker.isAd(url, this@Website)
                loadedUrls[url] = ad
            } else {
                ad = loadedUrls[url]!!
            }
            return if (ad && currentURL != null && !currentURL!!.contains("twitter.com") && SettingValues.isPro) AdBlocker.createEmptyResource() else super.shouldInterceptRequest(view, url)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.startsWith("intent://")) {
                try {
                    // https://stackoverflow.com/a/58163386/6952238
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent != null && (intent.scheme == "https" || intent.scheme == "http")) {
                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        v!!.loadUrl(fallbackUrl!!)
                    }
                    return true
                } catch (ignored: URISyntaxException) {
                }
            }
            val type = getContentType(url)
            if (triedURLS == null) {
                triedURLS = ArrayList()
            }
            return if ((!openExternal(url) || type === ContentType.Type.VIDEO)
                && !triedURLS!!.contains(url)) {
                triedURLS!!.add(url)
                when (type) {
                    ContentType.Type.DEVIANTART, ContentType.Type.IMGUR, ContentType.Type.IMAGE -> {
                        if (SettingValues.image) {
                            val intent2 = Intent(view.context, MediaView::class.java)
                            intent2.putExtra(MediaView.EXTRA_URL, url)
                            view.context.startActivity(intent2)
                            return true
                        }
                        super.shouldOverrideUrlLoading(view, url)
                    }

                    ContentType.Type.REDDIT -> {
                        if (!url.contains("inapp=false")) {
                            val opened = OpenRedditLink.openUrl(view.context, url, false)
                            if (!opened) {
                                return super.shouldOverrideUrlLoading(view, url)
                            }
                        } else {
                            return false
                        }
                        true
                    }

                    ContentType.Type.STREAMABLE -> {
                        if (SettingValues.video) {
                            val myIntent = Intent(view.context, MediaView::class.java)
                            myIntent.putExtra(MediaView.EXTRA_URL, url)
                            view.context.startActivity(myIntent)
                            return true
                        }
                        super.shouldOverrideUrlLoading(view, url)
                    }

                    ContentType.Type.ALBUM -> {
                        if (SettingValues.album) {
                            val i: Intent
                            i = if (albumSwipe) {
                                Intent(view.context, AlbumPager::class.java)
                            } else {
                                Intent(view.context, Album::class.java)
                            }
                            i.putExtra(Album.EXTRA_URL, url)
                            view.context.startActivity(i)
                            return true
                        }
                        super.shouldOverrideUrlLoading(view, url)
                    }

                    ContentType.Type.GIF -> {
                        if (SettingValues.gif) {
                            val myIntent = Intent(view.context, MediaView::class.java)
                            myIntent.putExtra(MediaView.EXTRA_URL, url)
                            view.context.startActivity(myIntent)
                            return true
                        }
                        super.shouldOverrideUrlLoading(view, url)
                    }

                    ContentType.Type.VIDEO -> {
                        if (!tryOpenWithVideoPlugin(url)) {
                            super.shouldOverrideUrlLoading(view, url)
                        } else super.shouldOverrideUrlLoading(view, url)
                    }

                    ContentType.Type.EXTERNAL -> super.shouldOverrideUrlLoading(view, url)
                    else -> super.shouldOverrideUrlLoading(view, url)
                }
            } else {
                super.shouldOverrideUrlLoading(view, url)
            }
        }
    }

    companion object {
        private fun getDomainName(url: String?): String? {
            val uri: URI
            try {
                uri = URI(url)
                val domain = uri.host ?: return ""
                return if (domain.startsWith("www.")) domain.substring(4) else domain
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            return url
        }

        var triedURLS: ArrayList<String>? = null
    }
}
