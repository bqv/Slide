package me.ccrama.redditslide.Adapters

import android.app.Activity
import android.os.AsyncTask
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ltd.ucode.slide.Authentication
import net.dean.jraw.models.Message
import net.dean.jraw.models.PrivateMessage
import net.dean.jraw.paginators.InboxPaginator
import net.dean.jraw.paginators.Paginator

class InboxMessages(var where: String) : GeneralPosts<Message>() {
    @JvmField var loading = false
    private var paginator: Paginator<Message>? = null
    private var refreshLayout: SwipeRefreshLayout? = null
    private var adapter: InboxAdapter? = null

    fun bindAdapter(a: InboxAdapter?, layout: SwipeRefreshLayout?) {
        adapter = a
        refreshLayout = layout
        loadMore(a, where, true)
    }

    fun loadMore(adapter: InboxAdapter?, where: String?, refresh: Boolean) {
        LoadData(refresh).execute(where)
    }

    inner class LoadData(val reset: Boolean) : AsyncTask<String?, Void?, List<Message>?>() {
        public override fun onPostExecute(subs: List<Message>?) {
            if (subs == null && !nomore) {
                adapter!!.setError(true)
                refreshLayout!!.isRefreshing = false
            } else if (!nomore) {
                if (subs!!.size < 25) {
                    nomore = true
                }
                if (reset) {
                    posts.clear()
                }
                posts.addAll(subs)
                (adapter!!.mContext as Activity).runOnUiThread {
                    refreshLayout!!.isRefreshing = false
                    loading = false
                    adapter!!.notifyDataSetChanged()
                }
            }
        }

        override fun doInBackground(vararg subredditPaginators: String?): List<Message>? {
            return try {
                if (reset || paginator == null) {
                    paginator = InboxPaginator(Authentication.reddit, where)
                    paginator!!.setLimit(25)
                    nomore = false
                }
                nomore = if (paginator!!.hasNext()) {
                    val done = ArrayList<Message>()
                    for (m in paginator!!.next()) {
                        done.add(m)
                        if (m.dataNode.has("replies") && !m.dataNode["replies"].toString().isEmpty() && m.dataNode["replies"].has("data") && m.dataNode["replies"]["data"].has("children")) {
                            val n = m.dataNode["replies"]["data"]["children"]
                            for (o in n) {
                                done.add(PrivateMessage(o["data"]))
                            }
                        }
                    }
                    return done
                } else {
                    true
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
