package me.ccrama.redditslide.SubmissionViews

import android.content.Intent
import android.os.AsyncTask
import android.view.View
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.Activities.MediaView
import me.ccrama.redditslide.Fragments.SubmissionsView.Companion.currentPosition
import me.ccrama.redditslide.Fragments.SubmissionsView.Companion.currentSubmission
import me.ccrama.redditslide.submission
import me.ccrama.redditslide.util.LayoutUtils
import net.dean.jraw.ApiException
import net.dean.jraw.managers.AccountManager

object PopulateBase {
    fun addAdaptorPosition(myIntent: Intent, submission: IPost, adapterPosition: Int) {
        if (submission.comments == null && adapterPosition != -1) {
            myIntent.putExtra(MediaView.ADAPTER_POSITION, adapterPosition)
            myIntent.putExtra(MediaView.SUBMISSION_URL, submission.permalink)
        }
        currentPosition(adapterPosition)
        currentSubmission(submission)
    }

    class AsyncReportTask(private val submission: IPost, private val contextView: View) :
        AsyncTask<String?, Void?, Void?>() {
        override fun doInBackground(vararg reason: String?): Void? {
            try {
                AccountManager(Authentication.reddit).report(
                    submission.submission, reason[0]
                )
            } catch (e: ApiException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            val s = Snackbar.make(contextView, R.string.msg_report_sent, Snackbar.LENGTH_SHORT)
            LayoutUtils.showSnackbar(s)
        }
    }
}
