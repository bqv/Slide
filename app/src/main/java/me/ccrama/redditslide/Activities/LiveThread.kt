package me.ccrama.redditslide.Activities

import android.R
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFactory
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.network.ContentType.Companion.hostContains
import ltd.ucode.slide.R.color
import ltd.ucode.slide.R.id
import ltd.ucode.slide.R.layout
import ltd.ucode.slide.R.string
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Activities.LiveThread.PaginatorAdapter.ItemHolder
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.HttpUtil
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.util.TwitterObject
import me.ccrama.redditslide.views.CommentOverflow
import me.ccrama.redditslide.views.SidebarLayout
import net.dean.jraw.managers.LiveThreadManager
import net.dean.jraw.models.LiveThread
import net.dean.jraw.models.LiveUpdate
import net.dean.jraw.paginators.LiveThreadPaginator
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.URI

class LiveThread() : BaseActivityAnim() {
    var thread: LiveThread? = null
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                onBackPressed()
                return true
            }

            id.info -> {
                (findViewById<View>(id.drawer_layout) as DrawerLayout).openDrawer(
                    Gravity.RIGHT
                )
                return true
            }

            else -> return false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(ltd.ucode.slide.R.menu.settings_info, menu)
        return true
    }

    var baseRecycler: RecyclerView? = null
    var term: String? = null
    override fun onDestroy() {
        super.onDestroy()
        //todo finish
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.background = null
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(layout.activity_livethread)
        baseRecycler = findViewById<View>(id.content_view) as RecyclerView
        baseRecycler!!.layoutManager = LinearLayoutManager(this@LiveThread)
        object : AsyncTask<Void?, Void?, Void?>() {
            var d: MaterialDialog? = null
            public override fun onPreExecute() {
                d = MaterialDialog(this@LiveThread)
                    .title(string.livethread_loading_title)
                    .message(string.misc_please_wait)
                    //.progress(true, 100)
                    .cancelable(false)
                    .also { it.show() }
            }

            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    thread = LiveThreadManager(Authentication.reddit)[intent.getStringExtra(
                        EXTRA_LIVEURL
                    )]
                } catch (e: Exception) {
                }
                return null
            }

            public override fun onPostExecute(aVoid: Void?) {
                if (thread == null) {
                    AlertDialog.Builder(this@LiveThread)
                        .setTitle(string.livethread_not_found)
                        .setMessage(string.misc_please_try_again_soon)
                        .setPositiveButton(string.btn_ok) { dialog: DialogInterface?, which: Int -> finish() }
                        .setOnDismissListener { dialog: DialogInterface? -> finish() }
                        .setCancelable(false)
                        .show()
                } else {
                    d!!.dismiss()
                    setupAppBar(id.toolbar, thread!!.title, enableUpButton = true, colorToolbar = false)
                    (findViewById<View>(id.toolbar)).setBackgroundResource(color.md_red_300)
                    (findViewById<View>(id.header_sub)).setBackgroundResource(color.md_red_300)
                    themeSystemBars(Palette.getDarkerColor(resources.getColor(color.md_red_300)))
                    setRecentBar(
                        getString(
                            string.livethread_recents_title,
                            thread!!.title
                        ), resources.getColor(color.md_red_300)
                    )
                    doPaginator()
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    var updates: ArrayList<LiveUpdate>? = null
    var paginator: LiveThreadPaginator? = null

    fun doPaginator() {
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                paginator = LiveThreadManager(Authentication.reddit).stream(thread)
                updates = ArrayList(paginator!!.accumulateMerged(5))
                return null
            }

            public override fun onPostExecute(aVoid: Void?) {
                doLiveThreadUpdates()
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun doLiveThreadUpdates() {
        val adapter = PaginatorAdapter(this)
        baseRecycler!!.adapter = adapter
        doLiveSidebar()
        if (thread!!.websocketUrl != null && thread!!.websocketUrl.isNotEmpty()) {
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    val o = ObjectMapper().reader()
                    try {
                        val ws = WebSocketFactory().createSocket(
                            thread!!.websocketUrl
                        )
                        ws.addListener(object : WebSocketAdapter() {
                            override fun onTextMessage(
                                websocket: WebSocket, s: String
                            ) {
                                LogUtil.v("Recieved$s")
                                if (s.contains("\"type\": \"update\"")) {
                                    try {
                                        val u = LiveUpdate(o.readTree(s)["payload"]["data"])
                                        updates!!.add(0, u)
                                        runOnUiThread(Runnable {
                                            adapter.notifyItemInserted(0)
                                            baseRecycler!!.smoothScrollToPosition(0)
                                        })
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                } else if (s.contains("embeds_ready")) {
                                    var node = updates!![0].dataNode.toString()
                                    LogUtil.v("Getting")
                                    try {
                                        node = node.replace(
                                            "\"embeds\":[]",
                                            "\"embeds\":" + o.readTree(s)["payload"]["media_embeds"].toString()
                                        )
                                        val u = LiveUpdate(o.readTree(node))
                                        updates!![0] = u
                                        runOnUiThread { adapter.notifyItemChanged(0) }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } /* todo else if(s.contains("delete")){
                                    updates.remove(0);
                                    adapter.notifyItemRemoved(0);
                                }*/
                            }
                        })
                        ws.connect()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: WebSocketException) {
                        e.printStackTrace()
                    }
                    return null
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    inner class PaginatorAdapter(context: Context?) : RecyclerView.Adapter<ItemHolder>() {
        private val layoutInflater: LayoutInflater

        init {
            layoutInflater = LayoutInflater.from(context)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            val itemView =
                layoutInflater.inflate(layout.live_list_item, parent, false)
            return ItemHolder(itemView)
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val u = updates!![position]
            holder.title.text =
                "/u/" + u.author + " " + TimeUtils.getTimeAgo(u.created.time, this@LiveThread)
            if (u.body.isEmpty()) {
                holder.info.visibility = View.GONE
            } else {
                holder.info.visibility = View.VISIBLE
                holder.info.setTextHtml(
                    CompatUtil.fromHtml(u.dataNode["body_html"].asText()),
                    "NO SUBREDDIT"
                )
            }
            holder.title.setOnClickListener {
                val i = Intent(this@LiveThread, Profile::class.java)
                i.putExtra(Profile.EXTRA_PROFILE, u.author)
                startActivity(i)
            }
            holder.imageArea.visibility = View.GONE
            holder.twitterArea.visibility = View.GONE
            holder.twitterArea.stopLoading()
            if (u.embeds.isEmpty()) {
                holder.go.visibility = View.GONE
            } else {
                val url = u.embeds[0].url
                holder.go.visibility = View.VISIBLE
                holder.go.setOnClickListener {
                    val i = Intent(this@LiveThread, Website::class.java)
                    i.putExtra(LinkUtil.EXTRA_URL, url)
                    startActivity(i)
                }
                val host = URI.create(url).host.lowercase()
                if (hostContains(host, "imgur.com")) {
                    LogUtil.v("Imgur")
                    holder.imageArea.visibility = View.VISIBLE
                    holder.imageArea.setOnClickListener { holder.go.callOnClick() }
                    (applicationContext as App).imageLoader!!.displayImage(url, holder.imageArea)
                } else if (hostContains(host, "twitter.com")) {
                    LogUtil.v("Twitter")
                    holder.twitterArea.visibility = View.VISIBLE
                    LoadTwitter(
                        holder.twitterArea,
                        url
                    ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            }
        }

        inner class LoadTwitter(private val view: WebView, var url: String) :
            AsyncTask<String?, Void?, Void?>() {
            private val client: OkHttpClient?
            private val gson: Gson
            var twitter: TwitterObject? = null

            init {
                client = App.client
                gson = Gson()
            }

            fun parseJson() {
                try {
                    val result = HttpUtil.getJsonObject(
                        client,
                        gson,
                        "https://publish.twitter.com/oembed?url=$url",
                        null
                    )
                    LogUtil.v("Got " + CompatUtil.fromHtml(result.toString()))
                    twitter = ObjectMapper().readValue(result.toString(), TwitterObject::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun doInBackground(vararg sub: String?): Void? {
                parseJson()
                return null
            }

            public override fun onPostExecute(aVoid: Void?) {
                if (twitter != null && twitter!!.html != null) {
                    view.loadData(
                        twitter!!.html.replace(
                            "//platform.twitter",
                            "https://platform.twitter"
                        ), "text/html", "UTF-8"
                    )
                }
            }
        }

        override fun getItemCount(): Int {
            return updates!!.size
        }

        inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var title: TextView
            var info: SpoilerRobotoTextView
            var imageArea: ImageView
            var twitterArea: WebView
            var go: View

            init {
                title = itemView.findViewById(id.title)
                info = itemView.findViewById(id.body)
                go = itemView.findViewById(id.go)
                imageArea = itemView.findViewById(id.image_area)
                twitterArea = itemView.findViewById(id.twitter_area)
                twitterArea.webChromeClient = WebChromeClient()
                twitterArea.settings.javaScriptEnabled = true
                twitterArea.setBackgroundColor(Color.TRANSPARENT)
                twitterArea.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
            }
        }
    }

    fun doLiveSidebar() {
        findViewById<View>(id.loader).visibility = View.GONE
        val dialoglayout = findViewById<View>(id.sidebarsub)
        dialoglayout.findViewById<View>(id.sub_stuff).visibility = View.GONE
        (dialoglayout.findViewById<View>(id.sub_infotitle) as TextView).text =
            (if (thread!!.state) "LIVE: " else "") + thread!!.title
        (dialoglayout.findViewById<View>(id.active_users) as TextView).text =
            thread!!.localizedViewerCount + " viewing"
        (dialoglayout.findViewById<View>(id.active_users) as TextView).text =
            thread!!.localizedViewerCount
        run {
            val text: String = thread!!.getDataNode().get("resources_html").asText()
            val body: SpoilerRobotoTextView =
                findViewById<View>(id.sidebar_text) as SpoilerRobotoTextView
            val overflow: CommentOverflow =
                findViewById<View>(id.commentOverflow) as CommentOverflow
            setViews(text, "none", body, overflow)
        }
        run {
            val text: String = thread!!.getDataNode().get("description_html").asText()
            val body: SpoilerRobotoTextView =
                findViewById<View>(id.sub_title) as SpoilerRobotoTextView
            val overflow: CommentOverflow =
                findViewById<View>(id.sub_title_overflow) as CommentOverflow
            setViews(text, "none", body, overflow)
        }
    }

    private fun setViews(
        rawHTML: String,
        subreddit: String,
        firstTextView: SpoilerRobotoTextView,
        commentOverflow: CommentOverflow
    ) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks.get(0) != "<div class=\"md\">") {
            firstTextView.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks[0], subreddit)
            startIndex = 1
        } else {
            firstTextView.text = ""
            firstTextView.visibility = View.GONE
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                commentOverflow.setViews(blocks, subreddit)
            } else {
                commentOverflow.setViews(blocks.subList(startIndex, blocks.size), subreddit)
            }
            val sidebar = findViewById<View>(id.drawer_layout) as SidebarLayout
            for (i in 0 until commentOverflow.childCount) {
                val maybeScrollable = commentOverflow.getChildAt(i)
                if (maybeScrollable is HorizontalScrollView) {
                    sidebar.addScrollable(maybeScrollable)
                }
            }
        } else {
            commentOverflow.removeAllViews()
        }
    }

    companion object {
        @JvmField
        val EXTRA_LIVEURL = "liveurl"
    }
}
