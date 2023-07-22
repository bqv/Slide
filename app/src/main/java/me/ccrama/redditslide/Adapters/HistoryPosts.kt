package me.ccrama.redditslide.Adapters

import android.os.AsyncTask
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ltd.ucode.network.reddit.data.RedditSubmission
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import me.ccrama.redditslide.PostMatch.doesMatch
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.Submission
import net.dean.jraw.paginators.FullnamesPaginator
import java.util.Collections
import java.util.TreeMap

class HistoryPosts : GeneralPosts<PublicContribution> {
    private var refreshLayout: SwipeRefreshLayout? = null
    private var adapter: ContributionAdapter? = null
    @JvmField var loading = false
    var prefix = ""

    constructor() {}
    constructor(prefix: String) {
        this.prefix = prefix
    }

    fun bindAdapter(a: ContributionAdapter?, layout: SwipeRefreshLayout?) {
        adapter = a
        refreshLayout = layout
        loadMore(a, true)
    }

    fun loadMore(adapter: ContributionAdapter?, reset: Boolean) {
        LoadData(reset).execute()
    }

    var paginator: FullnamesPaginator? = null

    inner class LoadData(val reset: Boolean) : AsyncTask<String?, Void?, ArrayList<PublicContribution>?>() {
        public override fun onPostExecute(submissions: ArrayList<PublicContribution>?) {
            loading = false
            if (submissions != null && !submissions.isEmpty()) {
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
                    posts = filteredSubmissions.toMutableList()
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
            } else {
                // end of submissions
                nomore = true
                adapter!!.notifyDataSetChanged()
            }
            refreshLayout!!.isRefreshing = false
        }

        override fun doInBackground(vararg subredditPaginators: String?): ArrayList<PublicContribution>? {
            val newData = ArrayList<PublicContribution>()
            return try {
                if (reset || paginator == null) {
                    val ids = ArrayList<String>()
                    val idsSorted = HashMap<Long, String>()
                    val values: Map<String, String> = if (prefix.isEmpty()) {
                        App.contentDatabase.seen.getByContains("")
                    } else {
                        App.contentDatabase.seen.getByPrefix(prefix)
                    }
                    for (entry in values.entries) {
                        var done: Any
                        done = if (entry.value == "true" || entry.value == "false") {
                            java.lang.Boolean.valueOf(entry.value)
                        } else {
                            java.lang.Long.valueOf(entry.value)
                        }
                        if (prefix.isEmpty()) {
                            if (!entry.key.contains("readLater")) {
                                if (entry.key.length == 6 && done is Boolean) {
                                    ids.add("t3_" + entry.key)
                                } else if (done is Long) {
                                    if (entry.key.contains("_")) {
                                        idsSorted[done] = entry.key
                                    } else {
                                        idsSorted[done] = "t3_" + entry.key
                                    }
                                }
                            }
                        } else {
                            var key = entry.key
                            if (!key.contains("_")) {
                                key = "t3_$key"
                            }
                            idsSorted[done as Long] = key.replace(prefix, "")
                        }
                    }
                    if (idsSorted.isNotEmpty()) {
                        val result2 = TreeMap<Long, String>(Collections.reverseOrder())
                        result2.putAll(idsSorted)
                        ids.addAll(0, result2.values)
                    }
                    paginator = FullnamesPaginator(Authentication.reddit,
                        ids.toTypedArray())
                }
                if (!paginator!!.hasNext()) {
                    nomore = true
                    return ArrayList()
                }
                for (c in paginator!!.next()) {
                    if (c is PublicContribution) {
                        newData.add(c)
                    }
                }
                newData
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
