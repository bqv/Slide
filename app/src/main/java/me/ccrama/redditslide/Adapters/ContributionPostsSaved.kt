package me.ccrama.redditslide.Adapters

import ltd.ucode.network.reddit.data.RedditSubmission
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.SettingValues.getSubmissionSort
import ltd.ucode.slide.SettingValues.getSubmissionTimePeriod
import me.ccrama.redditslide.HasSeen.setHasSeenContrib
import me.ccrama.redditslide.PostMatch.doesMatch
import net.dean.jraw.models.Contribution
import net.dean.jraw.models.Submission
import net.dean.jraw.paginators.UserSavedPaginator

class ContributionPostsSaved(subreddit: String?, where: String?, private val category: String?) : ContributionPosts(subreddit!!, where!!) {
    var paginator: UserSavedPaginator? = null
    override fun loadMore(adapter: ContributionAdapter?, subreddit: String?, reset: Boolean) {
        LoadData(reset).execute(subreddit)
    }

    inner class LoadData(reset: Boolean) : ContributionPosts.LoadData(reset) {
        override fun onPostExecute(submissions: ArrayList<Contribution>?) {
            super.onPostExecute(submissions)
        }

        override fun doInBackground(vararg subredditPaginators: String?): ArrayList<Contribution>? {
            val newData = ArrayList<Contribution>()
            return try {
                if (reset || paginator == null) {
                    paginator = UserSavedPaginator(Authentication.reddit, where, subreddit)
                    paginator!!.sorting = getSubmissionSort(subreddit)
                    paginator!!.timePeriod = getSubmissionTimePeriod(subreddit)
                    if (category != null) paginator!!.setCategory(category)
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
