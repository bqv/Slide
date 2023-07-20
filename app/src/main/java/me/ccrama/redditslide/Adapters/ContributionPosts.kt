package me.ccrama.redditslide.Adapters

import android.os.AsyncTask
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ltd.ucode.network.reddit.data.RedditSubmission
import ltd.ucode.slide.Authentication
import me.ccrama.redditslide.HasSeen.setHasSeenContrib
import me.ccrama.redditslide.PostMatch.doesMatch
import me.ccrama.redditslide.util.SortingUtil
import net.dean.jraw.models.Contribution
import net.dean.jraw.models.Submission
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod
import net.dean.jraw.paginators.UserContributionPaginator

open class ContributionPosts(protected val subreddit: String, protected val where: String) : GeneralPosts<Contribution>() {
    @JvmField var loading = false
    private var paginator: UserContributionPaginator? = null
    protected var refreshLayout: SwipeRefreshLayout? = null
    protected var adapter: ContributionAdapter? = null
    fun bindAdapter(a: ContributionAdapter?, layout: SwipeRefreshLayout?) {
        adapter = a
        refreshLayout = layout
        loadMore(a, subreddit, true)
    }

    open fun loadMore(adapter: ContributionAdapter?, subreddit: String?, reset: Boolean) {
        LoadData(reset).execute(subreddit)
    }

    open inner class LoadData(val reset: Boolean) : AsyncTask<String?, Void?, ArrayList<Contribution>?>() {
        public override fun onPostExecute(submissions: ArrayList<Contribution>?) {
            loading = false
            if (submissions != null && !submissions.isEmpty()) {
                // new submissions found
                var start = 0
                if (posts != null) {
                    start = posts.size + 1
                }
                if (reset || posts == null) {
                    posts = submissions
                    start = -1
                } else {
                    posts.addAll(submissions)
                }
                val finalStart = start
                // update online
                if (refreshLayout != null) {
                    refreshLayout!!.isRefreshing = false
                }
                if (finalStart != -1) {
                    adapter!!.notifyItemRangeInserted(finalStart + 1, posts.size)
                } else {
                    adapter!!.notifyDataSetChanged()
                }
            } else if (submissions != null) {
                // end of submissions
                nomore = true
                adapter!!.notifyDataSetChanged()
            } else if (!nomore) {
                // error
                adapter!!.setError(true)
            }
            refreshLayout!!.isRefreshing = false
        }

        override fun doInBackground(vararg subredditPaginators: String?): ArrayList<Contribution>? {
            val newData = ArrayList<Contribution>()
            return try {
                if (reset || paginator == null) {
                    paginator = UserContributionPaginator(Authentication.reddit, where, subreddit)
                    paginator!!.sorting = SortingUtil.getSorting(subreddit, Sorting.NEW)
                    paginator!!.timePeriod = SortingUtil.getTime(subreddit, TimePeriod.ALL)
                }
                if (!paginator!!.hasNext()) {
                    nomore = true
                    return ArrayList()
                }
                for (c in paginator!!.next()) {
                    if (c is Submission) {
                        val s = c
                        if (!doesMatch(RedditSubmission(s))) {
                            newData.add(s)
                        }
                    } else {
                        newData.add(c)
                    }
                }
                setHasSeenContrib(newData)
                newData
            } catch (e: Exception) {
                null
            }
        }
    }
}
