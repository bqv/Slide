package me.ccrama.redditslide.Adapters

import android.app.Activity
import android.os.AsyncTask
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ltd.ucode.network.reddit.data.RedditSubmission
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import me.ccrama.redditslide.Activities.MultiredditOverview
import me.ccrama.redditslide.PostMatch.doesMatch
import me.ccrama.redditslide.util.SortingUtil
import net.dean.jraw.http.NetworkException
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.Submission
import net.dean.jraw.paginators.Paginator
import net.dean.jraw.paginators.SubmissionSearchPaginator
import net.dean.jraw.paginators.SubmissionSearchPaginatorMultireddit
import net.dean.jraw.paginators.TimePeriod
import java.net.UnknownHostException

class SubredditSearchPosts(subreddit: String?, term: String, parent: Activity, multireddit: Boolean) : GeneralPosts<PublicContribution>() {
    private var term: String
    private var subreddit = ""
    var loading = false
    private var paginator: Paginator<Submission>? = null
    var refreshLayout: SwipeRefreshLayout? = null
    private var adapter: ContributionAdapter? = null
    var parent: Activity

    fun bindAdapter(a: ContributionAdapter?, layout: SwipeRefreshLayout?) {
        adapter = a
        refreshLayout = layout
        loadMore(a, subreddit, term, true)
    }

    fun loadMore(a: ContributionAdapter?, subreddit: String, where: String, reset: Boolean) {
        adapter = a
        this.subreddit = subreddit
        term = where
        LoadData(reset).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun loadMore(
        a: ContributionAdapter?, subreddit: String, where: String, reset: Boolean,
        multi: Boolean, time: TimePeriod
    ) {
        adapter = a
        this.subreddit = subreddit
        term = where
        multireddit = multi
        this.time = time
        LoadData(reset).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    var multireddit: Boolean
    var time = TimePeriod.ALL

    init {
        if (subreddit != null) {
            this.subreddit = subreddit
        }
        this.parent = parent
        this.term = term
        this.multireddit = multireddit
    }

    fun reset(time: TimePeriod) {
        this.time = time
        LoadData(true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    inner class LoadData(val reset: Boolean) : AsyncTask<String?, Void?, ArrayList<PublicContribution>?>() {
        public override fun onPostExecute(submissions: ArrayList<PublicContribution>?) {
            loading = false
            if (error != null) {
                if (error is NetworkException) {
                    val e = error as NetworkException
                    Toast.makeText(adapter!!.mContext, "Loading failed, " + e.response.statusCode + ": " + (error as NetworkException).response.statusMessage, Toast.LENGTH_LONG).show()
                }
                if (error!!.cause is UnknownHostException) {
                    Toast.makeText(adapter!!.mContext, "Loading failed, please check your internet connection", Toast.LENGTH_LONG).show()
                }
            }
            if (!submissions.isNullOrEmpty()) {
                // new submissions found
                var start = 0
                if (posts != null) {
                    start = posts.size + 1
                }
                val filteredSubmissions = ArrayList<PublicContribution>()
                for (c in submissions) {
                    if (c is Submission) {
                        if (!doesMatch(RedditSubmission((c as Submission?)!!))) {
                            filteredSubmissions.add(c)
                        }
                    } else {
                        filteredSubmissions.add(c)
                    }
                }
                if (reset || posts == null) {
                    posts = filteredSubmissions
                    start = -1
                } else {
                    posts.addAll(filteredSubmissions)
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
                if (reset) {
                    Toast.makeText(adapter!!.mContext, R.string.no_posts_found, Toast.LENGTH_LONG).show()
                }
            } else if (!nomore) {
                // error
                adapter!!.setError(true)
            }
            refreshLayout!!.isRefreshing = false
        }

        override fun doInBackground(vararg subredditPaginators: String?): ArrayList<PublicContribution>? {
            val newSubmissions = ArrayList<PublicContribution>()
            return try {
                if (reset || paginator == null) {
                    if (multireddit) {
                        paginator = SubmissionSearchPaginatorMultireddit(Authentication.reddit,
                            term)
                        (paginator as SubmissionSearchPaginatorMultireddit).multiReddit = MultiredditOverview.searchMulti
                        (paginator as SubmissionSearchPaginatorMultireddit).searchSorting = SubmissionSearchPaginatorMultireddit.SearchSort.valueOf(
                            SortingUtil.search.toString())
                        (paginator as SubmissionSearchPaginatorMultireddit).syntax = SubmissionSearchPaginatorMultireddit.SearchSyntax.LUCENE
                    } else {
                        paginator = SubmissionSearchPaginator(Authentication.reddit, term)
                        if (!subreddit.isEmpty()) {
                            (paginator as SubmissionSearchPaginator).subreddit = subreddit
                        }
                        (paginator as SubmissionSearchPaginator).searchSorting = SortingUtil.search
                        (paginator as SubmissionSearchPaginator).syntax = SubmissionSearchPaginator.SearchSyntax.LUCENE
                    }
                    paginator!!.timePeriod = time
                }
                if (!paginator!!.hasNext()) {
                    nomore = true
                    return newSubmissions
                }
                newSubmissions.addAll(paginator!!.next())
                newSubmissions
            } catch (e: Exception) {
                error = e
                e.printStackTrace()
                null
            }
        }

        var error: Exception? = null
    }
}
