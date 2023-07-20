package me.ccrama.redditslide.Adapters

import android.os.AsyncTask
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ltd.ucode.slide.Authentication
import net.dean.jraw.models.ModAction
import net.dean.jraw.paginators.ModLogPaginator

class ModLogPosts {
    var posts: ArrayList<ModAction>? = null
    var loading = false
    private var refreshLayout: SwipeRefreshLayout? = null
    private var adapter: ModLogAdapter? = null
    private var paginator: ModLogPaginator? = null
    fun bindAdapter(a: ModLogAdapter?, layout: SwipeRefreshLayout?) {
        adapter = a
        refreshLayout = layout
        loadMore(a)
    }

    fun loadMore(adapter: ModLogAdapter?) {
        LoadData(true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    inner class LoadData(val reset: Boolean) : AsyncTask<String?, Void?, ArrayList<ModAction>?>() {
        public override fun onPostExecute(subs: ArrayList<ModAction>?) {
            if (subs != null) {
                posts = if (reset || posts == null) {
                    subs
                } else {
                    posts!!.addAll(subs)
                    posts
                }
                loading = false
                refreshLayout!!.isRefreshing = false
                adapter!!.dataSet = this@ModLogPosts
                adapter!!.notifyDataSetChanged()
            } else {
                adapter!!.setError(true)
                refreshLayout!!.isRefreshing = false
            }
        }

        override fun doInBackground(vararg subredditPaginators: String?): ArrayList<ModAction>? {
            return try {
                if (reset || paginator == null) {
                    paginator = ModLogPaginator(Authentication.reddit, "mod")
                }
                if (paginator!!.hasNext()) {
                    ArrayList(paginator!!.next())
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
