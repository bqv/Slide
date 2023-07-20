package me.ccrama.redditslide

import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.view.View
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import me.ccrama.redditslide.util.LayoutUtils
import net.dean.jraw.ApiException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.VoteDirection

class Vote : AsyncTask<PublicContribution?, Void?, Void?> {
    private val direction: VoteDirection
    private var v: View?
    private var c: Context?

    constructor(b: Boolean, v: View?, c: Context?) {
        direction = if (b) VoteDirection.UPVOTE else VoteDirection.DOWNVOTE
        this.v = v
        this.c = c
        //CrashReportHandler.reinstall()
    }

    constructor(v: View?, c: Context?) {
        direction = VoteDirection.NO_VOTE
        this.v = v
        this.c = c
    }

    override fun doInBackground(vararg sub: PublicContribution?): Void? {
        if (Authentication.isLoggedIn) {
            try {
                AccountManager(Authentication.reddit).vote(
                    sub[0], direction
                )
            } catch (e: ApiException) {
                createVoteSnackbar(R.string.vote_err)
                e.printStackTrace()
            } catch (e: RuntimeException) {
                createVoteSnackbar(R.string.vote_err)
                e.printStackTrace()
            }
        } else {
            createVoteSnackbar(R.string.vote_err_login)
        }
        return null
    }

    private fun createVoteSnackbar(i: Int) {
        (c!! as Activity).runOnUiThread {
            try {
                if (v != null && c != null && v!!.context != null) {
                    val snackbar = Snackbar.make(v!!, i, Snackbar.LENGTH_SHORT)
                    LayoutUtils.showSnackbar(snackbar)
                }
            } catch (ignored: Exception) {
            }
            c = null
            v = null
        }
    }
}
